package testbot1;

import battlecode.common.*;
import java.util.Random;

public class RobotPlayer {

	static Direction facing;
	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	static Random rand; /*
						 * this will help to distinguish each robot otherwise,
						 * each robot will behave exactly the same
						 */
	static Team Enemy;
	static RobotController rc;

	public static void run(RobotController myRC) {
		rc = myRC;
		Enemy = rc.getTeam().opponent();
		rand = new Random(rc.getID());
		facing = getRandomDirection(); // randomize starting direction

		// if the run method ends, the robot dies
		while (true) {
			try {
				// Random number from 0 to 1 for probabilistic decisions
				double fate = rand.nextDouble();
				
				if (rc.getType() == RobotType.HQ) {
					/*
					 * each action requires several conditions that need to be
					 * satisfied; otherwise, exceptions will be thrown, each of
					 * which incurs a 500 bytecode penalty
					 */

					attackEnemyZero(); /*
										 * we can this method first before
										 * spawning beavers because we probably
										 * will want to spawn more efficient
										 * creatures such as miners; in
										 * addition, spawning introduces
										 * attacking delay
										 */
					spawnUnit(RobotType.BEAVER);

				} else if (rc.getType() == RobotType.BEAVER) {
					attackEnemyZero();

					if (Clock.getRoundNum() < 700) {
						buildUnit(RobotType.MINERFACTORY);
					} else if (fate < 0.01) {
						buildUnit(RobotType.SUPPLYDEPOT);
					} else if (fate > 0.01 && fate < 0.51){
						buildUnit(RobotType.BARRACKS);
					} else{
						buildUnit(RobotType.TANKFACTORY);
					}

					mineAndMove();

				} else if (rc.getType() == RobotType.MINER) {
					attackEnemyZero();
					mineAndMove();
				} else if (rc.getType() == RobotType.MINERFACTORY) {
					spawnUnit(RobotType.MINER);
				} else if (rc.getType() == RobotType.BARRACKS) {
					if (fate < .7) {
						spawnUnit(RobotType.SOLDIER);
					} else {
						spawnUnit(RobotType.BASHER);
					}
				} else if (rc.getType() == RobotType.TOWER) {
					attackEnemyZero(); // basic attacking method
				} else if (rc.getType() == RobotType.SOLDIER) {
					attackEnemyZero(); // soldiers attack, not mine
					moveAround(); /*
								 * POSSIBLE OPTIMIZATION: chase enemies In
								 * addition, soldiers need to attack towers
								 * eventually, so they will have to move within
								 * attacking range of the towers, which is not
								 * possible under moveAround()
								 */
				} else if (rc.getType() == RobotType.BASHER) {
					// BASHERs attack automatically, so let's just move around
					// mostly randomly
					moveAround();
				} else if (rc.getType() == RobotType.TANK) {
					attackEnemyZero();
					moveAround();
				}

				/*
				 * If robots go low on supplies, they will become less effective
				 * in attacking; HQ supplies goods at a constant rate + any
				 * additional units from having supply depots built; these units
				 * are to be passed from HQ among the robots in a way such that
				 * all robots are sufficiently supplied
				 * 
				 * NOTE: robots that are low on supplies will have a white
				 * square around them
				 */

				transferSupplies();

			} catch (GameActionException e) { /*
											 * spawn method contains
											 * GameActionException need to catch
											 * these exceptions; otherwise, the
											 * function returns, and robot
											 * explodes
											 */
				e.printStackTrace();
			}

			rc.yield(); // robot yields its turn --> saves bytecode to avoid
						// hitting limit
		}
	}

	private static void spawnUnit(RobotType roboType)
			throws GameActionException {
		Direction randomDir = getRandomDirection();
		if (rc.isCoreReady() && rc.canSpawn(randomDir, roboType)) {
			rc.spawn(randomDir, roboType);
		}
	}

	private static Direction getRandomDirection() {
		return Direction.values()[(int) rand.nextDouble() * 8];
	}

	private static void moveAround() throws GameActionException {
		if(rc.isCoreReady() && rc.canMove(facing)){
			MapLocation tileInFrontLocation = rc.getLocation().add(facing);
			TerrainTile tileInFrontTerrain = rc.senseTerrainTile(tileInFrontLocation);
			RobotType roboType = rc.getType();
			
			boolean tileInFrontSafe = true;
			
			while(tileInFrontSafe){
				if(tileInFrontTerrain != TerrainTile.NORMAL){
					if(!(tileInFrontTerrain == TerrainTile.VOID && (roboType == RobotType.DRONE || roboType == RobotType.MISSILE))){
						tileInFrontSafe = false;
						break;
					}
				}
				
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(roboType.sensorRadiusSquared, Enemy);
				
				for(RobotInfo r : enemyRobots){
					if(r.location.distanceSquaredTo(tileInFrontLocation) <= r.type.attackRadiusSquared){
						tileInFrontSafe = false;
						break;
					}
				}
				
				break;
			}
			
			double probCutoff = tileInFrontSafe ? 0.0 : 0.75;
			
			if (rand.nextDouble() >= probCutoff) {
				if (rand.nextDouble() < 0.5) {
					facing = facing.rotateLeft(); // 45 degree turn
				} else {
					facing = facing.rotateRight();
				}
			}else{ // try to move in the facing direction since the tile in front is safe
				rc.move(facing);
			}
		}
	}

	private static void mineAndMove() throws GameActionException {
		if (rc.senseOre(rc.getLocation()) > 1) { // if there is ore, try to mine
			if (rc.isCoreReady() && rc.canMine()) {
				rc.mine();
			}
		} else { // otherwise, look for ore
			moveAround();
		}
	}

	private static void attackEnemyZero() throws GameActionException {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
				rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		if (nearbyEnemies.length > 0) { /*
										 * there are enemies nearby, and we'll
										 * try to shoot a nearbyEnemies[0]
										 */
			if (rc.isWeaponReady()
					&& rc.canAttackLocation(nearbyEnemies[0].location)) {
				rc.attackLocation(nearbyEnemies[0].location);
			}
		}
	}

	private static void transferSupplies() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),
				GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;

		MapLocation suppliesToThisLocation = null;

		for (RobotInfo ri : nearbyAllies) {
			if (ri.supplyLevel < lowestSupply) {
				lowestSupply = ri.supplyLevel;
				transferAmount = (rc.getSupplyLevel() - ri.supplyLevel) / 2;
				suppliesToThisLocation = ri.location;
			}
		}

		if (suppliesToThisLocation != null) {
			rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
		}
	}

	private static void buildUnit(RobotType roboType) throws GameActionException {
		if (rc.getTeamOre() > roboType.oreCost) {
			Direction buildDir = getRandomDirection();

			if (rc.isCoreReady() && rc.canBuild(buildDir, roboType)) {
				rc.build(buildDir, roboType);
			}
		}
	}

	// This method will attempt to move in Direction d (or as close to it as
	// possible)
	private static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5
				&& (!rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8]) || blocked)) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
		}
	}

	private static int directionToInt(Direction d) {
		switch (d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}
}