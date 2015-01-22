// finishedspecializedbot from YouTube channel

package examplePlayer6;

import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	public static boolean needsSupplier(RobotController rc) throws GameActionException {
		if (rc.readBroadcast(200) == 0) {
			return true;
		}
		return false;
	}
	public static void run(RobotController rc) throws GameActionException {
        BaseBot myself;

        if (rc.getType() == RobotType.HQ) {
            myself = new HQ(rc);
        } else if (rc.getType() == RobotType.BEAVER) {
            myself = new Beaver(rc);
        } else if (rc.getType() == RobotType.BARRACKS || rc.getType() == RobotType.HELIPAD || rc.getType() == RobotType.TANKFACTORY) {
            myself = new SimpleBuilding(rc);
        } else if (rc.getType() == RobotType.SOLDIER || rc.getType() == RobotType.DRONE || rc.getType() == RobotType.TANK) {
			if (rc.getType() == RobotType.DRONE && needsSupplier(rc)) {
				myself = new Supplier(rc);
				rc.broadcast(200, rc.readBroadcast(200) + 1);
                rc.broadcast(199, rc.getID());
			}	
			else {
				myself = new SimpleFighter(rc);
			}
        } else if (rc.getType() == RobotType.TOWER) {
            myself = new Tower(rc);
        } else {
            myself = new BaseBot(rc);
        }

        while (true) {
            try {
                myself.go();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

    public static class BaseBot {
        protected RobotController rc;
        protected MapLocation myHQ, theirHQ;
        protected Team myTeam, theirTeam;

        public BaseBot(RobotController rc) {
            this.rc = rc;
            this.myHQ = rc.senseHQLocation();
            this.theirHQ = rc.senseEnemyHQLocation();
            this.myTeam = rc.getTeam();
            this.theirTeam = this.myTeam.opponent();
        }

        public Direction[] getDirectionsToward(MapLocation dest) {
            Direction toDest = rc.getLocation().directionTo(dest);
            Direction[] dirs = {toDest,
		    		toDest.rotateLeft(), toDest.rotateRight(),
				toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};

            return dirs;
        }

        public Direction getMoveDir(MapLocation dest) {
            Direction[] dirs = getDirectionsToward(dest);
            for (Direction d : dirs) {
                if (rc.canMove(d)) {
                    return d;
                }
            }
            return null;
        }

        public Direction getSpawnDirection(RobotType type) {
            Direction[] dirs = getDirectionsToward(this.theirHQ);
            for (Direction d : dirs) {
                if (rc.canSpawn(d, type)) {
                    return d;
                }
            }
            return null;
        }

        public Direction getBuildDirection(RobotType type) {
            Direction[] dirs = getDirectionsToward(this.theirHQ);
            for (Direction d : dirs) {
                if (rc.canBuild(d, type)) {
                    return d;
                }
            }
            return null;
        }

        public RobotInfo[] getAllies() {
            RobotInfo[] allies = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
            return allies;
        }

        public RobotInfo[] getEnemiesInAttackingRange() {
            RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, theirTeam);
            return enemies;
        }

        public void attackLeastHealthEnemy(RobotInfo[] enemies) throws GameActionException {
            if (enemies.length == 0) {
                return;
            }

            double minEnergon = Double.MAX_VALUE;
            MapLocation toAttack = null;
            for (RobotInfo info : enemies) {
                if (info.health < minEnergon) {
                    toAttack = info.location;
                    minEnergon = info.health;
                }
            }

            rc.attackLocation(toAttack);
        }

        public void beginningOfTurn() {
            if (rc.senseEnemyHQLocation() != null) {
                this.theirHQ = rc.senseEnemyHQLocation();
            }
        }

        public void endOfTurn() {
        }

        public void go() throws GameActionException {
            beginningOfTurn();
            execute();
            endOfTurn();
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }

    public static class HQ extends BaseBot {
        public static int xMin, xMax, yMin, yMax;
        public static int xpos, ypos;
        public static int totalNormal, totalVoid, totalProcessed;
        public static int towerThreat;

        public static double ratio;
        public static boolean isFinished;

        public static int strategy; // 0 = "defend", 1 = "build drones", 2 = "build soldiers"

        public HQ(RobotController rc) throws GameActionException {
            super(rc);

            xMin = Math.min(this.myHQ.x, this.theirHQ.x);
            xMax = Math.max(this.myHQ.x, this.theirHQ.x);
            yMin = Math.min(this.myHQ.y, this.theirHQ.y);
            yMax = Math.max(this.myHQ.y, this.theirHQ.y);

            xpos = xMin;
            ypos = yMin;

            totalNormal = totalVoid = totalProcessed = 0;
            towerThreat = 0;
            strategy = 0;
            isFinished = false;

            rc.broadcast(298, 300);
            rc.broadcast(299, 300);
        }

        public void analyzeMap() {
            while (ypos < yMax + 1) {
                TerrainTile t = rc.senseTerrainTile(new MapLocation(xpos, ypos));

                if (t == TerrainTile.NORMAL) {
                    totalNormal++;
                    totalProcessed++;
                }
                else if (t == TerrainTile.VOID) {
                    totalVoid++;
                    totalProcessed++;
                }
                xpos++;
                if (xpos == xMax + 1) {
                    xpos = xMin;
                    ypos++;
                }

                if (Clock.getBytecodesLeft() < 100) {
                    return;
                }
            }
            ratio = (double)totalNormal / totalProcessed;
            isFinished = true;
        }
        public void analyzeTowers() {
            MapLocation[] towers = rc.senseEnemyTowerLocations();
            towerThreat = 0;

            for (int i=0; i<towers.length; ++i) {
                MapLocation towerLoc = towers[i];

                if ((xMin <= towerLoc.x && towerLoc.x <= xMax && yMin <= towerLoc.y && towerLoc.y <= yMax) || towerLoc.distanceSquaredTo(this.theirHQ) <= 50) {
                    for (int j=0; j<towers.length; ++j) {
                        if (towers[j].distanceSquaredTo(towerLoc) <= 50) {
                            towerThreat++;
                        }
                    }
                }
            }
        }

        public void chooseStrategy() throws GameActionException {
            if (towerThreat >= 10) {
                //play defensive
                strategy = 0;
            }
            else {
                if (ratio <= 0.85) {
                    //build drones
                    strategy = 1;
                }
                else {
                    //build soldiers
                    strategy = 2;
                }
            }
            rc.broadcast(100, strategy);
        }
        
        public void execute() throws GameActionException {
            int numBeavers = rc.readBroadcast(2);

            if (rc.isCoreReady() && rc.getTeamOre() > 100 && numBeavers < 10) {
                Direction newDir = getSpawnDirection(RobotType.BEAVER);
                if (newDir != null) {
                    rc.spawn(newDir, RobotType.BEAVER);
                    rc.broadcast(2, numBeavers + 1);
                }
            }
            MapLocation rallyPoint;
            if (Clock.getRoundNum() < 1000) {
                rallyPoint = new MapLocation( (this.myHQ.x + this.theirHQ.x) / 2,
                                              (this.myHQ.y + this.theirHQ.y) / 2);
            }
            else {
                rallyPoint = this.theirHQ;
            }
            rc.broadcast(0, rallyPoint.x);
            rc.broadcast(1, rallyPoint.y);
            
            RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
            int idToLook = rc.readBroadcast(199);

            for (int i=0; i<allies.length; ++i) {
                RobotInfo k = allies[i];

                if (k.ID == idToLook) {
                    rc.transferSupplies(100000, allies[i].location);
                }
            }

            if (!isFinished) {
                analyzeMap();
                analyzeTowers();
            }
            else {
                chooseStrategy();
            }

            rc.yield();
        }
    }

    public static class Beaver extends BaseBot {
        public Beaver(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            if (rc.isCoreReady()) {
                if (rc.getTeamOre() < 300) {
                    //mine
                    if (rc.senseOre(rc.getLocation()) > 0) {
                        rc.mine();
                    }
                    else {
                        Direction newDir = getMoveDir(this.theirHQ);

                        if (newDir != null) {
                            rc.move(newDir);
                        }
                    }
                }
                else {
                    //build barracks
                    int strategy = rc.readBroadcast(100);

                    RobotType toBuild = RobotType.BARRACKS;
                    if (strategy == 1) toBuild = RobotType.HELIPAD;
                    if (strategy == 0) {
                        if (rc.checkDependencyProgress(RobotType.BARRACKS) != DependencyProgress.NONE) {
                            toBuild = RobotType.TANKFACTORY;
                        }
                    }
					if (rc.checkDependencyProgress(RobotType.HELIPAD) != DependencyProgress.DONE && needsSupplier(rc)) {
						toBuild = RobotType.HELIPAD;
					}

                    Direction newDir = getBuildDirection(toBuild);
                    if (newDir != null) {
                        rc.build(newDir, toBuild);
                    }
                }
            }

            rc.yield();
        }
    }

    public static class SimpleBuilding extends BaseBot {
        public SimpleBuilding(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            RobotType toSpawn = null;

            if (rc.getType() == RobotType.BARRACKS) toSpawn = RobotType.SOLDIER;
            else if (rc.getType() == RobotType.HELIPAD) toSpawn = RobotType.DRONE;
            else if (rc.getType() == RobotType.TANKFACTORY) toSpawn = RobotType.TANK;

			boolean shouldSpawn = true;
            if (rc.getType() != RobotType.HELIPAD && needsSupplier(rc)) {
                shouldSpawn = false;
            }
			if (rc.readBroadcast(100) == 0) {
				shouldSpawn = (rc.getType() == RobotType.TANKFACTORY);
			}
			if (rc.getType() == RobotType.HELIPAD && rc.readBroadcast(100) != 1) {
				shouldSpawn = needsSupplier(rc);
			}

            if (toSpawn != null && rc.isCoreReady() && rc.getTeamOre() > toSpawn.oreCost && shouldSpawn) {
                Direction newDir = getSpawnDirection(toSpawn);
                if (newDir != null) {
                    rc.spawn(newDir, toSpawn);
                }
            }

            rc.yield();
        }
    }

    public static class SimpleFighter extends BaseBot {
        static boolean isSupplyLow = false;

        public SimpleFighter(RobotController rc) {
            super(rc);
        }

		public void addToQueue(RobotController rc) throws GameActionException {
            int queueStart = rc.readBroadcast(298), queueEnd = rc.readBroadcast(299);

            rc.broadcast(queueEnd, rc.getID());

            queueEnd ++;
            if (queueEnd >= 2000) {
                queueEnd = 300;
            }
            rc.broadcast(299, queueEnd);
		}

        public void execute() throws GameActionException {
            RobotInfo[] enemies = getEnemiesInAttackingRange();

            if (enemies.length > 0) {
                //attack!
                if (rc.isWeaponReady()) {
                    attackLeastHealthEnemy(enemies);
                }
            }
            else if (rc.isCoreReady()) {
                int rallyX = rc.readBroadcast(0);
                int rallyY = rc.readBroadcast(1);
                MapLocation rallyPoint = new MapLocation(rallyX, rallyY);

                Direction newDir = getMoveDir(rallyPoint);

                if (newDir != null) {
                    rc.move(newDir);
                }
            }
			double supply = rc.getSupplyLevel();
			if (!isSupplyLow && supply <= 500) {
				addToQueue(rc);
                isSupplyLow = true;
			}
            else if (supply > 500) {
                isSupplyLow = false;
            }
            rc.yield();

        }
    }

    public static class Supplier extends BaseBot {
		public Supplier(RobotController rc) {
			super(rc);
		}

		public void execute() throws GameActionException {
            int queueStart = rc.readBroadcast(298), queueEnd = rc.readBroadcast(299);

            if (rc.isCoreReady()) {
                if (queueStart != queueEnd && rc.getSupplyLevel() > 1000) {
                    RobotInfo[] allies = getAllies();

                    int target = rc.readBroadcast(queueStart);

                    for (int i=0; i<allies.length; ++i) {
                        if (allies[i].ID == target) {
                            if (rc.getLocation().distanceSquaredTo(allies[i].location) <= GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
                                rc.transferSupplies(10000, allies[i].location);
                                rc.broadcast(298, queueStart+1);
                            }
                            else {
                                Direction toGoDir = getMoveDir(allies[i].location);

                                if (toGoDir != null) {
                                    rc.move(toGoDir);
                                }
                            }
                            break;
                        }
                    }
                }
                if (rc.getSupplyLevel() <= 1000) {
                    Direction toGoDir = getMoveDir(this.myHQ);

                    if (toGoDir != null) {
                        rc.move(toGoDir);
                    }
                }
            }
            
			rc.yield();
		}
    }

    public static class Tower extends BaseBot {
        public Tower(RobotController rc) {
            super(rc);
        }

        public void execute() throws GameActionException {
            rc.yield();
        }
    }
}