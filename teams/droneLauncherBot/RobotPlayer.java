package droneLauncherBot;

import battlecode.common.*;

import java.util.LinkedList;
import java.util.Random;

/**
 * Main class which defines every robot.
 * 
 * @author {LoveBracketsHateSemicolons;
 *
 */
public class RobotPlayer {

	// Controller and Type
	private static RobotController rc;
	private static RobotType thisRobotType;
	// Round number
	private static int roundNum;
	// Randomness based on ID
	private static Random rand;

	// Team variables
	private static Team Friend;
	private static Team Enemy;

	// Robots, towers, and HQ's
	private static RobotInfo[] friendlyRobots;
	private static RobotInfo[] enemyRobots;
	private static MapLocation friendlyHQ;
	private static int friendlyHQAttackRadiusSquared;
	private static MapLocation[] friendlyTowers;
	private static MapLocation enemyHQ;
	private static int enemyHQAttackRadiusSquared;
	private static MapLocation[] enemyTowers;

	private static int halfwayDistance;
	private static int wholeDistanceCoefficient;
	private static MapLocation previousTowerLocation;
	private static boolean swarming = true;
	private static int swarmRound;

	private static Direction facing;
	private static Direction[] directions = { Direction.NORTH,
			Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
			Direction.NORTH_WEST };

	// HQ only
	private static MapLocation lastSwarmTarget;
	
	// Missile only
	private static int turnsRemaining;

	// Drone only
	private static Direction patrolDirection;

	/**************************************************************************
	 * Main method called when it is a robot's turn. Needs to be looped
	 * indefinitely, otherwise the robot will die.
	 * 
	 * @param rc
	 *            - The robot controller class for this robot instance.
	 * @throws GameActionException
	 *************************************************************************/
	public static void run(RobotController rc) throws GameActionException {

		// Initialize all variables that need to be initialized only once per
		// robot.
		RobotPlayer.rc = rc;
		thisRobotType = rc.getType();
		Friend = rc.getTeam();
		Enemy = Friend.opponent();
		rand = new Random(rc.getID());
		if (thisRobotType != RobotType.MISSILE) {
			friendlyHQ = rc.senseHQLocation();
			enemyHQ = rc.senseEnemyHQLocation();
			friendlyTowers = rc.senseTowerLocations();
			enemyTowers = rc.senseEnemyTowerLocations();

			// Slightly less to avoid tower issues
			halfwayDistance = (int) (0.45 * Math.sqrt(friendlyHQ
					.distanceSquaredTo(enemyHQ)));
			wholeDistanceCoefficient = friendlyHQ.distanceSquaredTo(enemyHQ) / 700;

			facing = getRandomDirection(); // Randomize starting direction
			// It was this or int casting...
			swarmRound = rc.getRoundLimit() * 9 / 10;

			// HQ only stuff
			if (thisRobotType == RobotType.HQ) {
				lastSwarmTarget = friendlyHQ;
			}

			// Drone only stuff
			if (thisRobotType == RobotType.DRONE) {
				Direction HQdirection = friendlyHQ.directionTo(enemyHQ);
				patrolDirection = (rand.nextDouble() > 0.5) ? HQdirection
						.rotateLeft().rotateLeft() : HQdirection.rotateRight()
						.rotateRight();
			}

		} else {
			turnsRemaining = GameConstants.MISSILE_LIFESPAN;
		}

		// Method can never end or the robot is destroyed.
		while (true) {
			/*
			 * TODO: use the map analysis methods to determine useful cutoffs
			 * and probabilities for building or spawning certain robots
			 */

			try {
				/**************************************************************
				 * Initialize any per-round variables.
				 * ------------------------------------------------------------
				 * 1) Round number
				 * ------------------------------------------------------------
				 * 2) Number of towers on both teams
				 * ------------------------------------------------------------
				 * 3) HQ Attack Range on both teams
				 *************************************************************/
				if (thisRobotType != RobotType.MISSILE) {
					roundNum = Clock.getRoundNum();
					friendlyTowers = rc.senseTowerLocations();
					enemyTowers = rc.senseEnemyTowerLocations();
					friendlyHQAttackRadiusSquared = friendlyTowers.length >= 5 ? GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED
							: RobotType.HQ.attackRadiusSquared;
					enemyHQAttackRadiusSquared = enemyTowers.length >= 5 ? GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED
							: RobotType.HQ.attackRadiusSquared;
				}

				// Choose an action based on the type of robot.
				switch (thisRobotType) {

				case AEROSPACELAB:
					createUnit(RobotType.LAUNCHER, false);
					break;

				case BEAVER:
					attackEnemyZero();
					int numFriendlyUnit = rc.senseNearbyRobots(
							Integer.MAX_VALUE, Friend).length;

					// Building Order/Preferences
					if (rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 1) {
						createUnit(RobotType.MINERFACTORY, true);
					} else if (rc.readBroadcast(NUM_FRIENDLY_HELIPAD_CHANNEL) < 2) {
						createUnit(RobotType.HELIPAD, true);
					} else if (rc
							.readBroadcast(NUM_FRIENDLY_AEROSPACELAB_CHANNEL) < 1) {
						createUnit(RobotType.AEROSPACELAB, true);
					} else if (rc
							.readBroadcast(NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL) < 5) {
						createUnit(RobotType.SUPPLYDEPOT, true);
					} else if (rc.readBroadcast(NUM_FRIENDLY_HELIPAD_CHANNEL) < 3) {
						createUnit(RobotType.HELIPAD, true);
					} else if (rc
							.readBroadcast(NUM_FRIENDLY_AEROSPACELAB_CHANNEL) < 3) {
						createUnit(RobotType.AEROSPACELAB, true);
					} else if (rc
							.readBroadcast(NUM_FRIENDLY_HANDWASHSTATION_CHANNEL) < 3) {
						createUnit(RobotType.HANDWASHSTATION, true);
					} else if (rc
							.readBroadcast(NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL) < numFriendlyUnit / 7) {
						createUnit(RobotType.SUPPLYDEPOT, true);
					} else if (rc
							.readBroadcast(NUM_FRIENDLY_AEROSPACELAB_CHANNEL) < 5) {
						createUnit(RobotType.AEROSPACELAB, true);
					} else {
						createUnit(RobotType.HELIPAD, true);
					}

					mineAndMove();
					break;

				case DRONE:
					// if (swarming || roundNum > swarmRound) {
					// attackNearestTower();
					// } else if (rc.getSupplyLevel() < 80) {
					// moveTowardDestination(friendlyHQ, false, false, true);
					// } else {
					// targetEnemyMiners();
					// defendAndMove();
					// }
					// attackEnemyZero();

					if (rc.readBroadcast(SWARM_SIGNAL_CHANNEL) == 1) {
						attackNearestTower();
					} else if (!attackEnemyZero()) {
						defendAndMove();
					}
					
					break;

				case HELIPAD:
					// TODO: Use map analysis to decide drone production vs.
					// launcher production.

					// Get drone count
					int droneCount = rc
							.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL);
					
					// Exponential Decay for drone production
					double droneFate = rand.nextDouble();
					if (droneFate <= Math.pow(Math.E, -droneCount * 0.15)) {
						createUnit(RobotType.DRONE, false);
					}
					break;

				case HQ:
					attackEnemyZero();
					updateUnitCounts();

					if (roundNum % 5 == 0) {
						analyzeTowerStrength();
					}

					broadcastSwarmConditions();
					
					// Maintain only a few beavers
					if (rc.readBroadcast(NUM_FRIENDLY_BEAVERS_CHANNEL) < 3) {
						createUnit(RobotType.BEAVER, false);
					}
					break;

				case LAUNCHER:
					MapLocation currentLocation = rc.getLocation();

					double possEnemyFriendRatio;
					double bestEnemyFriendRatio = 0.0;
					MapLocation bestTarget = null;

					MapLocation radiusSquare;
					TerrainTile radiusTerrain;

					for (Direction possDir : directions) {
						radiusSquare = currentLocation.add(possDir, 5);
						radiusTerrain = rc.senseTerrainTile(radiusSquare);
						possEnemyFriendRatio = friendEnemyRatio(radiusSquare,
								RobotType.MISSILE.attackRadiusSquared, Enemy);

						if (radiusTerrain == TerrainTile.NORMAL
								&& possEnemyFriendRatio > bestEnemyFriendRatio) {
							bestEnemyFriendRatio = possEnemyFriendRatio;
							bestTarget = radiusSquare;
						}
					}

					RobotInfo[] targets = rc.senseNearbyRobots(
							GameConstants.MISSILE_LIFESPAN
									* GameConstants.MISSILE_LIFESPAN, Enemy);
					bestTarget = targets.length == 0 ? null
							: targets[0].location;
					
					if (bestTarget != null) {
						launchMissile(bestTarget);
						moveTowardDestination(
								currentLocation.subtract(currentLocation
										.directionTo(bestTarget)), true, false,
								false);
//					} else if (rc.readBroadcast(SWARM_SIGNAL_CHANNEL) == 1) {
//						attackNearestTower();
//					}
						
					} else {
						attackNearestTower();
					}
					break;

				case MINER:
					attackEnemyZero();
					mineAndMove();
					break;

				case MINERFACTORY:
					// Get miner count
					int minerCount = rc
							.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL);
					
					// Exponential Decay for miner production
					double miningFate = rand.nextDouble();
					if (roundNum < 1500
							&& miningFate <= Math
									.pow(Math.E, -minerCount * 0.4)) {
						createUnit(RobotType.MINER, false);
					}
					break;

				case MISSILE:
					RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(
							thisRobotType.sensorRadiusSquared, Enemy);
					if (nearbyEnemies.length == 0) {
						missileMoveTowardDestination(rc.senseEnemyHQLocation());
					} else {
						missileMoveTowardDestination(nearbyEnemies[0].location);
					}
					turnsRemaining--;
					break;

				case TANK:
					attackNearestTower();
					attackEnemyZero();
					break;

				case TANKFACTORY:
					createUnit(RobotType.TANK, false);
					break;

				case TOWER:
					attackEnemyZero();
					break;

				default:
					break;

				}

				if (thisRobotType == RobotType.HQ
						|| (thisRobotType == RobotType.DRONE && swarming)) {
					transferSupplies();
				}

			} catch (GameActionException e) {
				e.printStackTrace();
			}

			rc.yield(); // End the robot's turn to save bytecode

		}

	}

	/************************************************************************
	 * Missile launching towards a particular location 
	 * 
	 * @param targetLoc - location to launch missile towards
	 * @return missile launched or not
	 * @throws GameActionException
	 ************************************************************************/
	private static boolean launchMissile(MapLocation targetLoc)
			throws GameActionException {
		MapLocation currentLocation = rc.getLocation();
		Direction directionToTarget = currentLocation.directionTo(targetLoc);

		if (rc.getMissileCount() > 0 && rc.canLaunch(directionToTarget)) {
			rc.launchMissile(directionToTarget);
			return true;
		}

		return false;
	}

	/***************************************************************
	 * Directs drones to patrol the borderline between the two HQ's
	 * 
	 * @throws GameActionException
	 ***************************************************************/
	private static void patrolBorder() throws GameActionException {
		// TODO: should other robots help patrol as well?
		MapLocation currentLocation = rc.getLocation();

		double distanceToFriendHQ = Math.pow(
				currentLocation.distanceSquaredTo(friendlyHQ), 0.5);
		double distanceToEnemyHQ = Math.pow(
				currentLocation.distanceSquaredTo(enemyHQ), 0.5);

		if (distanceToFriendHQ < 0.85 * distanceToEnemyHQ
				|| distanceToEnemyHQ < 0.95) {

			Direction HQdirection = friendlyHQ.directionTo(enemyHQ);
			MapLocation checkpoint = friendlyHQ.add(HQdirection,
					halfwayDistance);

			moveTowardDestination(checkpoint, false, false, true);

		} else {
			MapLocation newLocation = currentLocation.add(patrolDirection);

			if (rc.senseTerrainTile(newLocation) == TerrainTile.OFF_MAP) {
				patrolDirection = patrolDirection.opposite();
				newLocation = currentLocation.add(patrolDirection);
			}

			moveTowardDestination(newLocation, false, false, true);
		}
	}

	/*******************************************************************
	 * Directs drones and launchers to attack the nearest tower when it 
	 * determines that there are sufficient resources to attack or when
	 * the time is appropriate for attacking
	 * 
	 * @throws GameActionException
	 *******************************************************************/
	private static void broadcastSwarmConditions() throws GameActionException {
		int tankCount = rc.readBroadcast(NUM_FRIENDLY_TANKS_CHANNEL);
		int droneCount = rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL);
		int launcherCount = rc.readBroadcast(NUM_FRIENDLY_LAUNCHERS_CHANNEL);
		
		MapLocation targetLocation;
		int possDistance;
		int minDistance;
		int multiplier;
		
		if(enemyTowers.length == 0) {
			targetLocation = enemyHQ;
			multiplier = 1;
			
		} else {
			minDistance = Integer.MAX_VALUE;
			targetLocation = null;
			
			for(int index = 0; index < enemyTowers.length; index++) {
				possDistance = lastSwarmTarget.distanceSquaredTo(enemyTowers[index]);
				
				if(possDistance < minDistance) {
					targetLocation = enemyTowers[index];
					minDistance = possDistance;
				}
			}
			
			/* We can safely assume that targetLocation != null because the maximum
			 * squared distance between any two points on the map has an upper bound
			 * of 2 * 120^2 = 28,800
			 *
			 * Testing has suggested that the number of attacking units is proportional
			 * to the approximate number of 700 squared units if distance between
			 * 'lastSwarmTarget' and 'targetLocation' */
			
			multiplier = (int) ((double) minDistance / 700.0);
		}
		
		if(roundNum > swarmRound || tankCount > 10 * multiplier
				|| droneCount > 20 * multiplier || launcherCount > 5 * multiplier) {
			
			lastSwarmTarget = targetLocation;
			rc.broadcast(SWARM_SIGNAL_CHANNEL, 1);
			rc.broadcast(SWARM_LOCATION_X_CHANNEL, targetLocation.x);
			rc.broadcast(SWARM_LOCATION_Y_CHANNEL, targetLocation.y);
			
		} else {
			rc.broadcast(SWARM_SIGNAL_CHANNEL, 0);
			rc.broadcast(SWARM_LOCATION_X_CHANNEL, targetLocation.x);
			rc.broadcast(SWARM_LOCATION_Y_CHANNEL, targetLocation.y);
		}
	}

	/*******************************************************************
	 * Swarms the nearest tower as indicated by HQ 
	 * 
	 * @throws GameActionException
	 *******************************************************************/
	private static void attackNearestTower() throws GameActionException {
		// TODO: Make number counts a function of towerStrength
		MapLocation currentLocation = rc.getLocation();
		MapLocation targetLocation = new MapLocation(rc.readBroadcast(SWARM_LOCATION_X_CHANNEL),
				rc.readBroadcast(SWARM_LOCATION_Y_CHANNEL));
		
		double distanceToTarget = Math.sqrt(currentLocation.distanceSquaredTo(targetLocation));
		if (thisRobotType == RobotType.LAUNCHER) {
			double maxRadius = (GameConstants.MISSILE_LIFESPAN - 1.0) +
					Math.sqrt(RobotType.MISSILE.attackRadiusSquared);
			
			if (distanceToTarget < maxRadius) {
				launchMissile(targetLocation);
				moveTowardDestination(currentLocation.subtract(currentLocation
						.directionTo(targetLocation)), true, false, false);
			}
		}
		
		if (rc.isWeaponReady() && rc.canAttackLocation(targetLocation)) {
			rc.attackLocation(targetLocation);
		} else {
			if (distanceToTarget > 4.0 * Math.sqrt(thisRobotType.attackRadiusSquared)) {
				moveTowardDestination(targetLocation, false, false, true);
			} else {
				moveTowardDestination(targetLocation, true, false, false);
			}
		}
	}

	private static boolean targetEnemyMiners() throws GameActionException {
		RobotInfo[] allEnemies = rc.senseNearbyRobots(enemyHQ,
				Integer.MAX_VALUE, Enemy);
		for (RobotInfo e : allEnemies) {
			if (e.type == RobotType.MINER || e.type == RobotType.BEAVER
					|| !e.type.needsSupply()) {
				moveTowardDestination(e.location, false, false, true, true);
				return true;
			}
		}
		return false;
	}

	/*
	 * ==========================================================================
	 */

	public static class SearchNode {
		private MapLocation nodeLoc;
		private SearchNode nodeParent;

		public SearchNode(MapLocation loc, SearchNode parent) {
			nodeLoc = loc;
			nodeParent = parent;
		}

		public MapLocation getLoc() {
			return nodeLoc;
		}

		public SearchNode getParent() {
			return nodeParent;
		}

		public LinkedList<MapLocation> getPath() {
			LinkedList<MapLocation> nodePath = new LinkedList<MapLocation>();
			nodePath.add(0, this.getLoc());

			SearchNode currentNode = this;

			while (currentNode.getParent() != null) {
				currentNode = currentNode.getParent();
				nodePath.add(0, currentNode.getLoc());
			}

			return nodePath;
		}
	}

	private static boolean search(MapLocation dest, boolean DFS)
			throws GameActionException {
		MapLocation currentLocation = rc.getLocation();

		if (!reachedGoal(currentLocation, dest)) {
			LinkedList<SearchNode> agenda = new LinkedList<SearchNode>();
			LinkedList<MapLocation> visited = new LinkedList<MapLocation>();

			agenda.add(new SearchNode(currentLocation, null));
			visited.add(currentLocation);

			SearchNode currentNode;

			while (agenda.size() != 0) {
				if (DFS) {
					currentNode = agenda.removeLast();
				} else {
					currentNode = agenda.removeFirst();
				}

				MapLocation currentLoc = currentNode.getLoc();
				MapLocation parentLoc = currentNode.getParent().getLoc();
				Direction parentToCurrent = parentLoc.directionTo(currentLoc);

				rc.broadcast(channelHashFunc(parentLoc),
						directionToInt(parentToCurrent));

				LinkedList<MapLocation> nodeLocations = getChildren(currentNode
						.getLoc());

				for (MapLocation nodeLocation : nodeLocations) {
					SearchNode childNode = new SearchNode(nodeLocation,
							currentNode);

					if (reachedGoal(nodeLocation, dest)) {
						Direction currentToChild = currentLoc
								.directionTo(nodeLocation);
						rc.broadcast(channelHashFunc(currentLoc),
								directionToInt(currentToChild));
						return true;

					} else {
						if (!visited.contains(nodeLocation)) {
							visited.add(nodeLocation);
							agenda.add(childNode);
						}
					}
				}
			}
		}

		return false;
	}

	private static boolean reachedGoal(MapLocation loc, MapLocation dest) {
		return (loc.x == dest.x) && (loc.y == dest.y);
	}

	private static LinkedList<MapLocation> getChildren(MapLocation loc) {
		LinkedList<MapLocation> possibleChildren = new LinkedList<MapLocation>();

		for (Direction possDirection : Direction.values()) {
			MapLocation possSquare = loc.add(possDirection);
			TerrainTile possSquareTerrain = rc.senseTerrainTile(possSquare);

			if (possSquareTerrain == TerrainTile.NORMAL) {
				possibleChildren.add(possSquare);
			}
		}

		return possibleChildren;
	}

	private static int channelHashFunc(MapLocation loc) {
		int maxWidth = GameConstants.MAP_MAX_WIDTH;
		int maxHeight = GameConstants.MAP_MAX_HEIGHT;
		return 10000 + loc.x % maxWidth + maxWidth * loc.y % maxHeight;
	}

	/*
	 * ==========================================================================
	 */

	/**************************************************************************
	 * Gives the ratio of friend to enemy robots around a given location that
	 * are at most a given distance away.
	 * 
	 * @param loc - the location being analyzed
	 * @param radiusSquared - the radius being checked
	 * @param friendTeam - the team considered friendly
	 * @return ratio of friend to enemy robots
	 *************************************************************************/
	private static double friendEnemyRatio(MapLocation loc, int radiusSquared,
			Team friendTeam) {
		RobotInfo[] surroundingRobots;

		if (loc != null) {
			surroundingRobots = rc.senseNearbyRobots(loc, radiusSquared, null);
		} else {
			surroundingRobots = rc.senseNearbyRobots(radiusSquared);
		}

		double enemyCount = 0.0, friendCount = 0.0;

		if (friendTeam == null) {
			friendTeam = rc.getTeam();
		}

		for (RobotInfo roboInfo : surroundingRobots) {
			if (roboInfo.team == friendTeam) {
				friendCount++;
			} else {
				enemyCount++;
			}
		}

		if (friendCount == 0) {
			return 0;
		} else if (enemyCount == 0 && friendCount != 0) {
			return Integer.MAX_VALUE;
		} else {
			return friendCount / enemyCount;
		}
	}

	/**************************************************************************
	 * Attacks the first enemy in the list of enemies within attack range
	 * 
	 * @return True if an attack was successfully carried out
	 * @throws GameActionException
	 *************************************************************************/
	private static boolean attackEnemyZero() throws GameActionException {
		// TODO: find the best (not the first) enemy to attack
		if (rc.isWeaponReady()) {
			int attackRadiusSquared;
			MapLocation thisRobotLocation = rc.getLocation();

			attackRadiusSquared = (thisRobotType == RobotType.HQ) ? friendlyHQAttackRadiusSquared
					: thisRobotType.attackRadiusSquared;

			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(thisRobotLocation,
					attackRadiusSquared, Enemy);

			// Go for direct targets first.
			if (nearbyEnemies.length > 0) {
				MapLocation enemyZeroLocation = nearbyEnemies[0].location;

				if (rc.canAttackLocation(enemyZeroLocation)) {
					rc.attackLocation(enemyZeroLocation);
					return true;
				}
				// If there are none and the HQ has splash, then check if there
				// are targets in splash range.
			} else if (thisRobotType == RobotType.HQ
					&& friendlyTowers.length >= 5) {
				RobotInfo[] splashRangeEnemies = rc
						.senseNearbyRobots(
								thisRobotLocation,
								(int) Math.pow(
										Math.sqrt(attackRadiusSquared)
												+ Math.sqrt(GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED),
										2), Enemy);

				if (splashRangeEnemies.length > 0) {
					MapLocation actualEnemyZeroLocation = splashRangeEnemies[0].location;
					Direction towardsEnemyZero = friendlyHQ
							.directionTo(actualEnemyZeroLocation);
					MapLocation splashEnemyZeroLocation = friendlyHQ.add(
							towardsEnemyZero,
							(int) Math.sqrt(attackRadiusSquared));

					// Subtract at most one square
					if (!rc.canAttackLocation(splashEnemyZeroLocation)) {
						splashEnemyZeroLocation.subtract(towardsEnemyZero);
					}

					if (rc.canAttackLocation(splashEnemyZeroLocation)) {
						rc.attackLocation(splashEnemyZeroLocation);
					}
					return true;

				}
			}
		}

		return false;
	}

	/***************************************************************************
	 * Determines whether a location is considered safe.
	 * 
	 * @param loc - the location being tested.
	 * @param onlyHQAndTowers - considers only enemy HQ and Tower range unsafe.
	 * @param checkFriendlyMissiles - considers also being within friendly missile
	 * 								  range to be unsafe
	 * @return - True if the location is safe, false if it is not
	 **************************************************************************/
	private static boolean isSafe(MapLocation loc, boolean onlyHQAndTowers,
			boolean checkFriendlyMissiles) throws GameActionException {
		return isSafe(loc, onlyHQAndTowers, checkFriendlyMissiles, false);
	}

	private static boolean isSafe(MapLocation loc, boolean onlyHQAndTowers,
			boolean checkFriendlyMissiles, boolean ignoreMinersBeavers)
			throws GameActionException {
		TerrainTile locTerrain = rc.senseTerrainTile(loc);

		if (locTerrain != TerrainTile.NORMAL
				&& !(locTerrain == TerrainTile.VOID && (thisRobotType == RobotType.DRONE || thisRobotType == RobotType.MISSILE))) {
			return false;
		}

		// Check if HQ is in range
		if (enemyHQ.distanceSquaredTo(loc) <= enemyHQAttackRadiusSquared) {
			return false;
		}

		// Check if towers are in range
		for (MapLocation enemyTower : enemyTowers) {
			if (enemyTower.distanceSquaredTo(loc) <= RobotType.TOWER.attackRadiusSquared) {
				return false;
			}
		}

		// if (onlyHQAndTowers && !checkFriendlyMissiles) {
		// return true;
		// }
		//
		// Team roboTeam = checkFriendlyMissiles ? null : Enemy;
		// roboTeam = onlyHQAndTowers ? Friend : roboTeam;

		if (onlyHQAndTowers) {
			return true;
		}

		Team roboTeam = Enemy;
		/*
		 * Check if any enemies are in range or if any friendly drones are
		 * within explosion range
		 */
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(
				thisRobotType.sensorRadiusSquared, roboTeam);

		for (RobotInfo r : nearbyRobots) {
			if (r.location.distanceSquaredTo(loc) <= r.type.attackRadiusSquared
					&& (r.team == Enemy || (checkFriendlyMissiles && r.type == RobotType.MISSILE))
					&& !(ignoreMinersBeavers && (r.type == RobotType.BEAVER || r.type == RobotType.MINER))) {
				return false;
			}
		}

		return true;
	}

	/***********************************************************
	 * Generates a random, valid direction.
	 * 
	 * @return the generated direction
	 ***********************************************************/
	private static Direction getRandomDirection() {
		return Direction.values()[(int) rand.nextDouble() * 8];
	}

	/**************************************************************************
	 * Moves robot around randomly.
	 * 
	 * @throws GameActionException
	 **************************************************************************/
	private static void moveAround() throws GameActionException {
		if (rand.nextDouble() < 0.1) {
			if (rand.nextDouble() < 0.5) {
				facing = facing.rotateLeft(); // 45 degree turn
			} else {
				facing = facing.rotateRight();
			}
		}
		if (rc.isCoreReady() && rc.canMove(facing)) {
			MapLocation tileInFrontLocation = rc.getLocation().add(facing);
			boolean tileInFrontSafe = isSafe(tileInFrontLocation, false, true);
			double probCutoff = tileInFrontSafe ? 0.75 : 0.0;

			if (rand.nextDouble() >= probCutoff) {
				if (rand.nextDouble() < 0.5) {
					facing = facing.rotateLeft(); // 45 degree turn
				} else {
					facing = facing.rotateRight();
				}
			} else { // try to move in the facing direction since the tile in
						// front is safe
				rc.move(facing);
			}
		}
	}

	/********************************************************************************
	 * Directs a robot toward a given destination.
	 * 
	 * @param startLoc - starting location from which to move toward the target
	 *            		 destination in question; default is the current location of
	 *            		 the robot calling the function
	 * @param dest - the target location
	 * @param ignoreSafety - boolean to determine whether to call isSafe
	 * @param onlyHQAndTowers - parameter for isSafe
	 * @param checkFriendlyMissiles
	 *            - considers also being within friendly missile range to be
	 *            unsafe
	 * 
	 * @return True if there is a direction that the robot can move towards the
	 *         given destination
	 * @throws GameActionException
	 *********************************************************************************/

	private static boolean moveTowardDestination(MapLocation dest,
			boolean ignoreSafety, boolean onlyHQAndTowers,
			boolean checkFriendlyMissiles) throws GameActionException {

		return moveTowardDestination(rc.getLocation(), dest, ignoreSafety,
				onlyHQAndTowers, checkFriendlyMissiles, false);
	}

	private static boolean moveTowardDestination(MapLocation dest,
			boolean ignoreSafety, boolean onlyHQAndTowers,
			boolean checkFriendlyMissiles, boolean ignoreMinersBeavers)
			throws GameActionException {

		return moveTowardDestination(rc.getLocation(), dest, ignoreSafety,
				onlyHQAndTowers, checkFriendlyMissiles, ignoreMinersBeavers);
	}

	private static boolean moveTowardDestination(MapLocation startLoc,
			MapLocation dest, boolean ignoreSafety, boolean onlyHQAndTowers,
			boolean checkFriendlyMissiles, boolean ignoreMinersBeavers)
			throws GameActionException {
		// TODO: Should we consider including a "crowdedness" heuristic? If so,
		// how do we incorporate our current implementation?

		Direction straight = startLoc.directionTo(dest);
		MapLocation currentLocation = rc.getLocation();

		if (rc.isCoreReady()) {
			int straightIndex = directionToInt(straight);

			int[] offsets = new int[8];
			offsets[0] = 0;
			offsets[7] = 4;

			offsets[1] = (rand.nextDouble() > 0.5) ? 1 : -1;
			offsets[2] = -offsets[1];

			offsets[3] = (rand.nextDouble() > 0.5) ? 2 : -2;
			offsets[4] = -offsets[3];

			offsets[5] = (rand.nextDouble() > 0.5) ? 3 : -3;
			offsets[6] = -offsets[5];

			for (int offset : offsets) {
				Direction possDirection = directions[(straightIndex + offset + 8) % 8];

				if (rc.canMove(possDirection)) {
					MapLocation possSquare = currentLocation.add(possDirection);

					if (ignoreSafety
							|| isSafe(possSquare, onlyHQAndTowers,
									checkFriendlyMissiles, ignoreMinersBeavers)) {
						rc.move(possDirection);
						return true;
					}
				}
			}
		}

		return false;
	}

	// FOR MISSILES ONLY
	private static boolean missileMoveTowardDestination(MapLocation loc)
			throws GameActionException {
		Direction dir = rc.getLocation().directionTo(loc);
		if (rc.isCoreReady() && rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		return false;
	}

	/**************************************************************************
	 * Searches for enemies that have crossed the halfway point between the two
	 * HQ's and tries to attack them; otherwise, moves around randomly.
	 * 
	 * @throws GameActionException
	 *************************************************************************/
	private static void defendAndMove() throws GameActionException {

		// TODO: create ordered list that allows "stronger" robots to attack
		// "weaker" robots
		// TODO: create target variable that allows robots to track down the
		// invading robot in
		// question; otherwise, look for another one

		RobotInfo[] incomingEnemies = rc.senseNearbyRobots(
				(int) Math.pow(halfwayDistance, 2), Enemy);
		boolean weaponReady = rc.isWeaponReady();
		MapLocation targetLocation = null;

		for (RobotInfo robot : incomingEnemies) {
			if (weaponReady && rc.canAttackLocation(robot.location)) {
				rc.attackLocation(robot.location);
				return;

			} else if (targetLocation == null) {
				targetLocation = robot.location;
			}
		}

		if (targetLocation != null) {
			moveTowardDestination(targetLocation, false, true, true);

		} else if (thisRobotType == RobotType.DRONE) {
			//moveAround();
		}
	}

	/**************************************************************************
	 * Determines a direction in which a miner robot (either a BEAVER or MINER)
	 * can mine the most amount of ore.
	 * 
	 * @throws GameActionException
	 *************************************************************************/
	private static void locateBestOre() throws GameActionException {
		if (rc.isCoreReady()) {
			MapLocation currentLocation = rc.getLocation();

			int radius = (int) Math.sqrt(rc.getType().sensorRadiusSquared);

			double bestOreCount = 0.0;
			MapLocation bestDestination = null;

			for (Direction possDirection : Direction.values()) {
				MapLocation squareOne = currentLocation.add(possDirection,
						(int) (0.25 * radius));
				MapLocation squareTwo = currentLocation.add(possDirection,
						(int) (0.5 * radius));
				MapLocation squareThree = currentLocation.add(possDirection,
						(int) (0.75 * radius));
				MapLocation squareFour = currentLocation.add(possDirection,
						radius);

				double totalOreCount = rc.senseOre(squareOne)
						+ rc.senseOre(squareTwo) + rc.senseOre(squareThree)
						+ rc.senseOre(squareFour);

				if (totalOreCount > bestOreCount) {
					bestOreCount = totalOreCount;
					bestDestination = squareFour;
				}
			}

			if (bestDestination != null) {
				moveTowardDestination(bestDestination, false, false, true);
			} else {
				// TODO: Keep track of last destination traveled to by a miner?
				defendAndMove();
			}
		}
	}

	/**************************************************************************
	 * Mines at current location and then tries to look for more ore.
	 * 
	 * @throws GameActionException
	 *************************************************************************/
	private static void mineAndMove() throws GameActionException {
		if (rc.senseOre(rc.getLocation()) > 1) { // if there is ore, try to mine
			if (rc.isCoreReady() && rc.canMine()) {
				rc.mine();
			}
		} else { // otherwise, look for ore
			locateBestOre();
		}
	}

	/********************************************************
	 * Gets the corresponding index for each valid Direction
	 * 
	 * @param d
	 *            - the direction being indexed
	 * @return integer corresponding to a valid Direction
	 ********************************************************/
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

	/**************************************************************************
	 * Spawns or builds a robot.
	 * 
	 * @param roboType - the type of robot being built/spawned
	 * @param build - True if building, false if spawning
	 * @return - True if building/spawning succeeded
	 * @throws GameActionException
	 **************************************************************************/
	private static boolean createUnit(RobotType roboType, boolean build)
			throws GameActionException {
		if (rc.isCoreReady() && rc.getTeamOre() > roboType.oreCost) {
			MapLocation currentLocation = rc.getLocation();
			Direction testDir = Direction.values()[rand.nextInt(9)];
			boolean goLeft = rand.nextDouble() > 0.5;

			for (int turnCount = 0; turnCount < 8; turnCount++) {
				MapLocation testLoc = currentLocation.add(testDir);

				if (build) {
					if (rc.canBuild(testDir, roboType)
							&& isSafe(testLoc, false, true)) {
						rc.build(testDir, roboType);
						return true;
					}
				} else { // spawning
					if (rc.canSpawn(testDir, roboType)
							&& isSafe(testLoc, false, true)) {
						rc.spawn(testDir, roboType);
						return true;
					}
				}

				testDir = goLeft ? testDir.rotateLeft() : testDir.rotateRight();
			}
		}

		return false;
	}

	/**********************************************************************************
	 * Transfer supplies between units.
	 * 
	 * @throws GameActionException
	 **********************************************************************************/
	private static void transferSupplies() throws GameActionException {
		// TODO: Do we want to have a global ordering on robots? So that
		// robots may decide to "sacrifice" themselves for the sake of a
		// stronger, more able robot?

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),
				GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Friend);

		if (nearbyAllies.length == 0) {
			return;
		}

		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;

		MapLocation suppliesToThisLocation = null;
		if (thisRobotType == RobotType.DRONE) {
			if (lowestSupply > 200) {
				for (int i = 0; i < (int) Math.min(4, nearbyAllies.length); ++i) {
					int j = rand.nextInt(nearbyAllies.length);
					RobotInfo ally = nearbyAllies[j];
					if (ally.type.needsSupply() && ally.supplyLevel < 10) {
						transferAmount = (rc.getSupplyLevel() - ally.supplyLevel) / 2;
						suppliesToThisLocation = ally.location;
						break;
					}
				}
			}

		} else if (thisRobotType == RobotType.HQ) {
			for (RobotInfo ri : nearbyAllies) {
				if (ri.type.needsSupply() && ri.supplyLevel < lowestSupply) {
					lowestSupply = ri.supplyLevel;
					transferAmount = (rc.getSupplyLevel() - lowestSupply) / 2;
					suppliesToThisLocation = ri.location;
				}
			}
		}

		if (suppliesToThisLocation != null) {
			rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
		}

	}

	/**************************************************************************
	 * UNIT COUNTING
	 * ========================================================================
	 * NOTE: These variables are kept down here since they are solely used when
	 * updating quantity values. They need to be maintained outside the method
	 * since they are used across multiple rounds.
	 *************************************************************************/
	// Friendly Buildings
	private static int unitCountNumFriendlySupplyDepot;
	private static int unitCountNumFriendlyMinerFactory;
	private static int unitCountNumFriendlyTechInstitute;
	private static int unitCountNumFriendlyBarracks;
	private static int unitCountNumFriendlyHelipad;
	private static int unitCountNumFriendlyTrainingField;
	private static int unitCountNumFriendlyTankFactory;
	private static int unitCountNumFriendlyAerospaceLab;
	private static int unitCountNumFriendlyHandwashStation;
	// Friendly other units
	private static int unitCountNumFriendlyBeavers;
	private static int unitCountNumFriendlyMiners;
	private static int unitCountNumFriendlyComputers;
	private static int unitCountNumFriendlySoldiers;
	private static int unitCountNumFriendlyBashers;
	private static int unitCountNumFriendlyDrones;
	private static int unitCountNumFriendlyTanks;
	private static int unitCountNumFriendlyLaunchers;
	private static int unitCountNumFriendlyMissiles;
	// Enemy Buildings
	private static int unitCountNumEnemySupplyDepot;
	private static int unitCountNumEnemyMinerFactory;
	private static int unitCountNumEnemyTechInstitute;
	private static int unitCountNumEnemyBarracks;
	private static int unitCountNumEnemyHelipad;
	private static int unitCountNumEnemyTrainingField;
	private static int unitCountNumEnemyTankFactory;
	private static int unitCountNumEnemyAerospaceLab;
	private static int unitCountNumEnemyHandwashStation;
	// Enemy other units
	private static int unitCountNumEnemyBeavers;
	private static int unitCountNumEnemyMiners;
	private static int unitCountNumEnemyComputers;
	private static int unitCountNumEnemyCommanders;
	private static int unitCountNumEnemySoldiers;
	private static int unitCountNumEnemyBashers;
	private static int unitCountNumEnemyDrones;
	private static int unitCountNumEnemyTanks;
	private static int unitCountNumEnemyLaunchers;
	private static int unitCountNumEnemyMissiles;

	/***************************************************************************
	 * Collects and broadcasts the number of all unit types.
	 * 
	 * @throws GameActionException
	 **************************************************************************/
	@SuppressWarnings("fallthrough")
	private static void updateUnitCounts() throws GameActionException {

		// Run part of the work on each round
		// int roundNumMod = (roundNum % 10) / 2;
		int roundNumMod = roundNum % 5;
		if (roundNumMod == 0 || friendlyRobots == null || enemyRobots == null) {
			// Collect all robots into separate RobotInfo arrays.
			friendlyRobots = rc.senseNearbyRobots(Integer.MAX_VALUE, Friend);
			enemyRobots = rc.senseNearbyRobots(Integer.MAX_VALUE, Enemy);
		}
		int friendlyChunkSize = (int) Math.floor(friendlyRobots.length / 4);
		int enemyChunkSize = (int) Math.floor(enemyRobots.length / 4);
		int friendlyLoopStart = friendlyChunkSize * roundNumMod;
		// Make sure to read the whole array
		int friendlyLoopEnd = (roundNumMod == 3) ? friendlyRobots.length
				: friendlyChunkSize * (roundNumMod + 1);
		int enemyLoopStart = enemyChunkSize * roundNumMod;
		// Make sure to read the whole array
		int enemyLoopEnd = (roundNumMod == 3) ? enemyRobots.length
				: enemyChunkSize * (roundNumMod + 1);

		/**********************************************************************
		 * WARNING: Exhibits intentional FALLTHROUGH since the same loop is used
		 * for cases 1-3.
		 * -------------------------------------------------------------------
		 * Case 0: Initializes quantities to 0. Runs first fourth of the array.
		 * Cases 1-3: Each run a different fourth of the array. Case 4:
		 * Broadcasts all the quantities.
		 *********************************************************************/
		switch (roundNumMod) {
		case 0:
			// Set quantity variables to 0
			// Friendly Buildings
			unitCountNumFriendlySupplyDepot = 0;
			unitCountNumFriendlyMinerFactory = 0;
			unitCountNumFriendlyTechInstitute = 0;
			unitCountNumFriendlyBarracks = 0;
			unitCountNumFriendlyHelipad = 0;
			unitCountNumFriendlyTrainingField = 0;
			unitCountNumFriendlyTankFactory = 0;
			unitCountNumFriendlyAerospaceLab = 0;
			unitCountNumFriendlyHandwashStation = 0;
			// Friendly other units
			unitCountNumFriendlyBeavers = 0;
			unitCountNumFriendlyMiners = 0;
			unitCountNumFriendlyComputers = 0;
			unitCountNumFriendlySoldiers = 0;
			unitCountNumFriendlyBashers = 0;
			unitCountNumFriendlyDrones = 0;
			unitCountNumFriendlyTanks = 0;
			unitCountNumFriendlyLaunchers = 0;
			unitCountNumFriendlyMissiles = 0;
			// Enemy Buildings
			unitCountNumEnemySupplyDepot = 0;
			unitCountNumEnemyMinerFactory = 0;
			unitCountNumEnemyTechInstitute = 0;
			unitCountNumEnemyBarracks = 0;
			unitCountNumEnemyHelipad = 0;
			unitCountNumEnemyTrainingField = 0;
			unitCountNumEnemyTankFactory = 0;
			unitCountNumEnemyAerospaceLab = 0;
			unitCountNumEnemyHandwashStation = 0;
			// Enemy other units
			unitCountNumEnemyBeavers = 0;
			unitCountNumEnemyMiners = 0;
			unitCountNumEnemyComputers = 0;
			unitCountNumEnemySoldiers = 0;
			unitCountNumEnemyBashers = 0;
			unitCountNumEnemyDrones = 0;
			unitCountNumEnemyTanks = 0;
			unitCountNumEnemyLaunchers = 0;
			unitCountNumEnemyMissiles = 0;
		case 1:
		case 2:
		case 3:
			// Friendly Robot Loop
			for (int i = friendlyLoopStart; i < friendlyLoopEnd; ++i) {
				switch (friendlyRobots[i].type) {
				case AEROSPACELAB:
					++unitCountNumFriendlyAerospaceLab;
					break;
				case BARRACKS:
					++unitCountNumFriendlyBarracks;
					break;
				case BASHER:
					++unitCountNumFriendlyBashers;
					break;
				case BEAVER:
					++unitCountNumFriendlyBeavers;
					break;
				case COMPUTER:
					++unitCountNumFriendlyComputers;
					break;
				case DRONE:
					++unitCountNumFriendlyDrones;
					break;
				case HANDWASHSTATION:
					++unitCountNumFriendlyHandwashStation;
					break;
				case HELIPAD:
					++unitCountNumFriendlyHelipad;
					break;
				case HQ:
					// No need to count HQ!
					break;
				case LAUNCHER:
					++unitCountNumFriendlyLaunchers;
					break;
				case MINER:
					++unitCountNumFriendlyMiners;
					break;
				case MINERFACTORY:
					++unitCountNumFriendlyMinerFactory;
					break;
				case MISSILE:
					++unitCountNumFriendlyMissiles;
					break;
				case SOLDIER:
					++unitCountNumFriendlySoldiers;
					break;
				case SUPPLYDEPOT:
					++unitCountNumFriendlySupplyDepot;
					break;
				case TANK:
					++unitCountNumFriendlyTanks;
					break;
				case TANKFACTORY:
					++unitCountNumFriendlyTankFactory;
					break;
				case TECHNOLOGYINSTITUTE:
					++unitCountNumFriendlyTechInstitute;
					break;
				case TOWER:
					break;
				case TRAININGFIELD:
					++unitCountNumFriendlyTrainingField;
					break;
				default:
					break;
				}
			}

			// Enemy Robot Loop
			for (int j = enemyLoopStart; j < enemyLoopEnd; ++j) {
				switch (enemyRobots[j].type) {
				case AEROSPACELAB:
					++unitCountNumEnemyAerospaceLab;
					break;
				case BARRACKS:
					++unitCountNumEnemyBarracks;
					break;
				case BASHER:
					++unitCountNumEnemyBashers;
					break;
				case BEAVER:
					++unitCountNumEnemyBeavers;
					break;
				case COMMANDER:
					++unitCountNumEnemyCommanders;
					break;
				case COMPUTER:
					++unitCountNumEnemyComputers;
					break;
				case DRONE:
					++unitCountNumEnemyDrones;
					break;
				case HANDWASHSTATION:
					++unitCountNumEnemyHandwashStation;
					break;
				case HELIPAD:
					++unitCountNumEnemyHelipad;
					break;
				case HQ: // No need to count HQ! break; case LAUNCHER:
					++unitCountNumEnemyLaunchers;
					break;
				case MINER:
					++unitCountNumEnemyMiners;
					break;
				case MINERFACTORY:
					++unitCountNumEnemyMinerFactory;
					break;
				case MISSILE:
					++unitCountNumEnemyMissiles;
					break;
				case SOLDIER:
					++unitCountNumEnemySoldiers;
					break;
				case SUPPLYDEPOT:
					++unitCountNumEnemySupplyDepot;
					break;
				case TANK:
					++unitCountNumEnemyTanks;
					break;
				case TANKFACTORY:
					++unitCountNumEnemyTankFactory;
					break;
				case TECHNOLOGYINSTITUTE:
					++unitCountNumEnemyTechInstitute;
					break;
				case TOWER:
					break;
				case TRAININGFIELD:
					++unitCountNumEnemyTrainingField;
					break;
				default:
					break;

				}

			}
			break;
		case 4:
			// Friendly Buildings Broadcasts
			rc.broadcast(NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL,
					unitCountNumFriendlySupplyDepot);
			rc.broadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL,
					unitCountNumFriendlyMinerFactory);
			rc.broadcast(NUM_FRIENDLY_TECHINSTITUTE_CHANNEL,
					unitCountNumFriendlyTechInstitute);
			rc.broadcast(NUM_FRIENDLY_BARRACKS_CHANNEL,
					unitCountNumFriendlyBarracks);
			rc.broadcast(NUM_FRIENDLY_HELIPAD_CHANNEL,
					unitCountNumFriendlyHelipad);
			rc.broadcast(NUM_FRIENDLY_TRAININGFIELD_CHANNEL,
					unitCountNumFriendlyTrainingField);
			rc.broadcast(NUM_FRIENDLY_TANKFACTORY_CHANNEL,
					unitCountNumFriendlyTankFactory);
			rc.broadcast(NUM_FRIENDLY_AEROSPACELAB_CHANNEL,
					unitCountNumFriendlyAerospaceLab);
			rc.broadcast(NUM_FRIENDLY_HANDWASHSTATION_CHANNEL,
					unitCountNumFriendlyHandwashStation);
			// Friendly Units Broadcasts
			rc.broadcast(NUM_FRIENDLY_BEAVERS_CHANNEL,
					unitCountNumFriendlyBeavers);
			rc.broadcast(NUM_FRIENDLY_MINERS_CHANNEL,
					unitCountNumFriendlyMiners);
			rc.broadcast(NUM_FRIENDLY_COMPUTERS_CHANNEL,
					unitCountNumFriendlyComputers);
			rc.broadcast(NUM_FRIENDLY_SOLDIERS_CHANNEL,
					unitCountNumFriendlySoldiers);
			rc.broadcast(NUM_FRIENDLY_BASHERS_CHANNEL,
					unitCountNumFriendlyBashers);
			rc.broadcast(NUM_FRIENDLY_DRONES_CHANNEL,
					unitCountNumFriendlyDrones);
			rc.broadcast(NUM_FRIENDLY_TANKS_CHANNEL, unitCountNumFriendlyTanks);
			rc.broadcast(NUM_FRIENDLY_LAUNCHERS_CHANNEL,
					unitCountNumFriendlyLaunchers);
			rc.broadcast(NUM_FRIENDLY_MISSILES_CHANNEL,
					unitCountNumFriendlyMissiles);
			// Enemy Buildings Broadcasts
			rc.broadcast(NUM_ENEMY_SUPPLYDEPOT_CHANNEL,
					unitCountNumEnemySupplyDepot);
			rc.broadcast(NUM_ENEMY_MINERFACTORY_CHANNEL,
					unitCountNumEnemyMinerFactory);
			rc.broadcast(NUM_ENEMY_TECHINSTITUTE_CHANNEL,
					unitCountNumEnemyTechInstitute);
			rc.broadcast(NUM_ENEMY_BARRACKS_CHANNEL, unitCountNumEnemyBarracks);
			rc.broadcast(NUM_ENEMY_HELIPAD_CHANNEL, unitCountNumEnemyHelipad);
			rc.broadcast(NUM_ENEMY_TRAININGFIELD_CHANNEL,
					unitCountNumEnemyTrainingField);
			rc.broadcast(NUM_ENEMY_TANKFACTORY_CHANNEL,
					unitCountNumEnemyTankFactory);
			rc.broadcast(NUM_ENEMY_AEROSPACELAB_CHANNEL,
					unitCountNumEnemyAerospaceLab);
			rc.broadcast(NUM_ENEMY_HANDWASHSTATION_CHANNEL,
					unitCountNumEnemyHandwashStation);
			// Enemy Units Broadcasts
			rc.broadcast(NUM_ENEMY_BEAVERS_CHANNEL, unitCountNumEnemyBeavers);
			rc.broadcast(NUM_ENEMY_MINERS_CHANNEL, unitCountNumEnemyMiners);
			rc.broadcast(NUM_ENEMY_COMMANDERS_CHANNEL,
					unitCountNumEnemyCommanders);
			rc.broadcast(NUM_ENEMY_COMPUTERS_CHANNEL,
					unitCountNumEnemyComputers);
			rc.broadcast(NUM_ENEMY_SOLDIERS_CHANNEL, unitCountNumEnemySoldiers);
			rc.broadcast(NUM_ENEMY_BASHERS_CHANNEL, unitCountNumEnemyBashers);
			rc.broadcast(NUM_ENEMY_DRONES_CHANNEL, unitCountNumEnemyDrones);
			rc.broadcast(NUM_ENEMY_TANKS_CHANNEL, unitCountNumEnemyTanks);
			rc.broadcast(NUM_ENEMY_LAUNCHERS_CHANNEL,
					unitCountNumEnemyLaunchers);
			rc.broadcast(NUM_ENEMY_MISSILES_CHANNEL, unitCountNumEnemyMissiles);
			break;
		}

	}

	// ************************** START OF MAP ANALYSIS ***********************
	/**
	 * 
	 * @throws GameActionException
	 */
	private static void analyzeTowerStrength() throws GameActionException {
		int towerStrength = 0;

		// One or no towers -> very weak. Keep at 0.
		// Otherwise measure strength based on closeness.
		for (int i = 0; i < enemyTowers.length; ++i) {
			if (Math.sqrt(enemyTowers[i].distanceSquaredTo(enemyHQ)) < Math
					.sqrt(RobotType.TOWER.attackRadiusSquared)
					+ Math.sqrt(RobotType.HQ.attackRadiusSquared)) {
				towerStrength += 2;
			}

			for (int j = i + 1; j < enemyTowers.length; ++j) {
				if (Math.sqrt(enemyTowers[i].distanceSquaredTo(enemyTowers[j])) < 2 * Math
						.sqrt(RobotType.TOWER.attackRadiusSquared)) {
					towerStrength += 1;
				}
			}
		}

		rc.broadcast(TOWER_STRENGTH_CHANNEL, towerStrength);
	}

	/**************************************************************************
	 * BROADCAST CHANNELS
	 * ========================================================================
	 * NOTE 1: Constant are located here for less cluttering.
	 * ------------------------------------------------------------------------
	 * NOTE 2: Building/Units order follows directly from online documentation.
	 * ------------------------------------------------------------------------
	 * NOTE 3: Search does not have constants for the channels it uses.
	 * ========================================================================
	 * 0-9: Friendly Buildings
	 * ------------------------------------------------------------------------
	 * 10-19: Friendly Units
	 * ------------------------------------------------------------------------
	 * 20-29: Enemy Buildings
	 * ------------------------------------------------------------------------
	 * 30-39: Enemy Units
	 * ------------------------------------------------------------------------
	 * 100-999: Coordinate Planning + Rallying Points
	 * ------------------------------------------------------------------------
	 * 1000-1999: Offense/Defense Signals (e.g. Swarm)
	 * ------------------------------------------------------------------------
	 * 2000-2999: Map Analysis
	 * ------------------------------------------------------------------------
	 * 10000-29999: Search
	 *************************************************************************/

	// Friendly Buildings Channels
	private static final int NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL = 1;
	private static final int NUM_FRIENDLY_MINERFACTORY_CHANNEL = 2;
	private static final int NUM_FRIENDLY_TECHINSTITUTE_CHANNEL = 3;
	private static final int NUM_FRIENDLY_BARRACKS_CHANNEL = 4;
	private static final int NUM_FRIENDLY_HELIPAD_CHANNEL = 5;
	private static final int NUM_FRIENDLY_TRAININGFIELD_CHANNEL = 6;
	private static final int NUM_FRIENDLY_TANKFACTORY_CHANNEL = 7;
	private static final int NUM_FRIENDLY_AEROSPACELAB_CHANNEL = 8;
	private static final int NUM_FRIENDLY_HANDWASHSTATION_CHANNEL = 9;
	// Friendly Units Channels
	private static final int NUM_FRIENDLY_BEAVERS_CHANNEL = 10;
	private static final int NUM_FRIENDLY_MINERS_CHANNEL = 11;
	private static final int NUM_FRIENDLY_COMPUTERS_CHANNEL = 12;
	private static final int NUM_FRIENDLY_SOLDIERS_CHANNEL = 13;
	private static final int NUM_FRIENDLY_BASHERS_CHANNEL = 14;
	private static final int NUM_FRIENDLY_DRONES_CHANNEL = 15;
	private static final int NUM_FRIENDLY_TANKS_CHANNEL = 16;
	private static final int NUM_FRIENDLY_COMMANDERS_CHANNEL = 17;
	private static final int NUM_FRIENDLY_LAUNCHERS_CHANNEL = 18;
	private static final int NUM_FRIENDLY_MISSILES_CHANNEL = 19;
	// Enemy Buildings Channels
	private static final int NUM_ENEMY_SUPPLYDEPOT_CHANNEL = 21;
	private static final int NUM_ENEMY_MINERFACTORY_CHANNEL = 22;
	private static final int NUM_ENEMY_TECHINSTITUTE_CHANNEL = 23;
	private static final int NUM_ENEMY_BARRACKS_CHANNEL = 24;
	private static final int NUM_ENEMY_HELIPAD_CHANNEL = 25;
	private static final int NUM_ENEMY_TRAININGFIELD_CHANNEL = 26;
	private static final int NUM_ENEMY_TANKFACTORY_CHANNEL = 27;
	private static final int NUM_ENEMY_AEROSPACELAB_CHANNEL = 28;
	private static final int NUM_ENEMY_HANDWASHSTATION_CHANNEL = 29;
	// Enemy Units Channels
	private static final int NUM_ENEMY_BEAVERS_CHANNEL = 30;
	private static final int NUM_ENEMY_MINERS_CHANNEL = 31;
	private static final int NUM_ENEMY_COMPUTERS_CHANNEL = 32;
	private static final int NUM_ENEMY_SOLDIERS_CHANNEL = 33;
	private static final int NUM_ENEMY_BASHERS_CHANNEL = 34;
	private static final int NUM_ENEMY_DRONES_CHANNEL = 35;
	private static final int NUM_ENEMY_TANKS_CHANNEL = 36;
	private static final int NUM_ENEMY_COMMANDERS_CHANNEL = 37;
	private static final int NUM_ENEMY_LAUNCHERS_CHANNEL = 38;
	private static final int NUM_ENEMY_MISSILES_CHANNEL = 39;
	// Swarm Signals
	private static final int SWARM_SIGNAL_CHANNEL = 1000;
	private static final int SWARM_LOCATION_X_CHANNEL = 1001;
	private static final int SWARM_LOCATION_Y_CHANNEL = 1002;
	// Map Analysis
	private static final int TOWER_STRENGTH_CHANNEL = 2000;
}