package droneBot2;

import battlecode.common.*;
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

	private static Direction facing;
	private static Direction[] directions = { Direction.NORTH,
			Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
			Direction.NORTH_WEST };

	// Drone only
	private static Direction patrolDirection;
	private static boolean arrivedAtCircle;
	
	/**
	 * Main method called when it is a robot's turn. Needs to be looped
	 * indefinitely, otherwise the robot will die.
	 *
	 * @param rc
	 *            - The robot controller class for this robot instance.
	 * @throws GameActionException
	 */
	public static void run(RobotController rc) throws GameActionException {

		// Initialize all variables that need to be initialized only once per
		// robot.
		RobotPlayer.rc = rc;
		thisRobotType = rc.getType();
		Friend = rc.getTeam();
		Enemy = Friend.opponent();
		
			
		friendlyHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		friendlyTowers = rc.senseTowerLocations();
		enemyTowers = rc.senseEnemyTowerLocations();

		rand = new Random(rc.getID());
		facing = getRandomDirection(); // Randomize starting direction

		// Slightly less to avoid tower issues
		halfwayDistance = (int) (0.45 * Math.sqrt(friendlyHQ
				.distanceSquaredTo(enemyHQ)));

		// Drone only stuff
		if (thisRobotType == RobotType.DRONE) {
			Direction HQdirection = friendlyHQ.directionTo(enemyHQ);
			patrolDirection = (rand.nextDouble() > 0.5) ? HQdirection
					.rotateLeft().rotateLeft() : HQdirection.rotateRight().rotateRight();
					
			arrivedAtCircle = false;
		}

		// Method can never end or the robot is destroyed.
		while (true) {
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
				
				roundNum = Clock.getRoundNum();
				friendlyTowers = rc.senseTowerLocations();
				enemyTowers = rc.senseEnemyTowerLocations();
				friendlyHQAttackRadiusSquared = getActualHQAttackRadiusSquared(friendlyTowers.length);
				enemyHQAttackRadiusSquared = getActualHQAttackRadiusSquared(enemyTowers.length);

				// Choose an action based on the type of robot.
				switch (thisRobotType) {

				case BEAVER:
					attackEnemyZero();

					// Building Order/Preferences
					if (rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 1) {
						createUnit(RobotType.MINERFACTORY, true);

					} else if (rc.readBroadcast(NUM_FRIENDLY_HELIPAD_CHANNEL) < 5){
						createUnit(RobotType.HELIPAD, true);
					}

					mineAndMove();
					break;

				case DRONE:
//					if (rc.getSupplyLevel() < 50){
//						moveTowardDestination(friendlyHQ, false, false, true);
//						arrivedAtCircle = false;
//						break;
//					}
					
					attackEnemyZero();
					targetEnemyMinersAndStructures();
					
					if (!arrivedAtCircle) {
						moveTowardDestination(enemyHQ, false, false, true);
						checkIfCloseToHQ();
						
					} else {
						droneCircle();
					}
					
					break;

				case HELIPAD:
					createUnit(RobotType.DRONE, false);
					break;

				case HQ:
					attackEnemyZero();
					updateUnitCounts();
					
					int beaverCount = rc.readBroadcast(NUM_FRIENDLY_BEAVERS_CHANNEL);
					
					// Maintain only a few beavers
					if (beaverCount < 1) {
						if (createUnit(RobotType.BEAVER, false)) {
							rc.broadcast(NUM_FRIENDLY_BEAVERS_CHANNEL, beaverCount + 1);
						}
					} else if(beaverCount < 3 && rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL) < 10) {
						if (createUnit(RobotType.BEAVER, false)) {
							rc.broadcast(NUM_FRIENDLY_BEAVERS_CHANNEL, beaverCount + 1);
						}
					}
					
					break;
				
				case MINER:
					attackEnemyZero();
					mineAndMove();
					break;

				case MINERFACTORY:
					if (rc.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL) < 1) {
						createUnit(RobotType.MINER, false);
					} else if (rc.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL) < 10 &&
							rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL) < 5)
					break;

				case TOWER:
					attackEnemyZero();
					break;

				default:
					break;
				}
				
				if (thisRobotType == RobotType.HQ
						|| thisRobotType == RobotType.DRONE) {
					transferSupplies();
				}

				// Calculations for structures only!
				if ((!thisRobotType.needsSupply())
						&& rc.readBroadcast(TERRAIN_ANALYZED_CHANNEL) != 1) {
					analyzeMap();
				}

			} catch (GameActionException e) {
				e.printStackTrace();
			}

			rc.yield(); // End the robot's turn to save bytecode

		}
	}

	private static void droneCircle() throws GameActionException {
		if (rc.isCoreReady()) {
			MapLocation currentLocation = rc.getLocation();
			MapLocation possNextLocation1 = currentLocation.add(facing.rotateLeft());
			MapLocation possNextLocation2 = currentLocation.add(facing.rotateRight());
			
			MapLocation nextLocation = (possNextLocation1.distanceSquaredTo(enemyHQ) < 
					possNextLocation2.distanceSquaredTo(enemyHQ)) ? possNextLocation1 :
						possNextLocation2;
			
			TerrainTile nextTerrain = rc.senseTerrainTile(nextLocation);
			
			if (nextTerrain != TerrainTile.OFF_MAP) {
				moveTowardDestination(nextLocation, false, false, true);
				rc.setIndicatorString(0, "Moving Towards (" + nextLocation.x + ", " + nextLocation.y + ")");
				
			} else {
				facing = facing.opposite();
			}
		}
	}

	private static void checkIfCloseToHQ() {
		if (rc.getLocation().distanceSquaredTo(enemyHQ) <=
				2.25 * enemyHQAttackRadiusSquared) {
			arrivedAtCircle = true;
		}
	}
	
	/**
	 * Determines the true attacking range of HQ based on the number of towers
	 * 
	 * @param towerCount
	 *            - number of towers the team still has standing
	 * @return attack radius squared of HQ that accounts for number of towers
	 *         and splash range (if applicable)
	 */
	private static int getActualHQAttackRadiusSquared(int towerCount) {
		int attackRadiusSquared;

		if (towerCount >= 2) {
			attackRadiusSquared = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;

			if (towerCount >= 5) {
				attackRadiusSquared = (int) Math
						.pow(Math
								.sqrt(GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED)
								+ Math.sqrt(GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED),
								2);
			}

		} else {
			attackRadiusSquared = RobotType.HQ.attackRadiusSquared;
		}

		return attackRadiusSquared;
	}

	/**
	 * directs drones to patrol the borderline between the two HQ's
	 *
	 * @throws GameActionException
	 */
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

	private static boolean targetEnemyInvaders() throws GameActionException {
		RobotInfo[] incomingEnemies = rc.senseNearbyRobots(friendlyHQ,
				(int) Math.pow(halfwayDistance, 2), Enemy);

		if (incomingEnemies.length > 0) {
			if (thisRobotType != RobotType.LAUNCHER) {
				return moveTowardDestination(incomingEnemies[0].location, true,
						false, false);

			} else {
				return moveTowardDestination(incomingEnemies[0].location,
						false, false, true);
			}

		} else {
			return false;
		}
	}

	/**
	 * Directs drones to attack enemy miners, beavers, and structure, all of
	 * which serve as the foundation for any enemy operations
	 * 
	 * @return true if an enemy miner/beaver/structure has been located, false
	 *         otherwise
	 * @throws GameActionException
	 */
	private static boolean targetEnemyMinersAndStructures()
			throws GameActionException {
		RobotInfo[] allEnemies = rc.senseNearbyRobots(enemyHQ,
				Integer.MAX_VALUE, Enemy);

		for (RobotInfo e : allEnemies) {
			// Target miners, beavers, and structures (they do not need
			// to maintain a supply, and they mostly cannot attack)
			if (e.type == RobotType.MINER || e.type == RobotType.BEAVER
					|| e.type.supplyUpkeep == 0) {
				moveTowardDestination(e.location, false, false, true, true);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Attacks the first enemy in the list.
	 *
	 * @return true if an attack was successfully carried out, false otherwise
	 * @throws GameActionException
	 */
	private static boolean attackEnemyZero() throws GameActionException {
		// TODO: find the best (not the first) enemy to attack
		if (rc.isWeaponReady()) {
			int attackRadiusSquared;
			MapLocation thisRobotLocation = rc.getLocation();

			attackRadiusSquared = (thisRobotType == RobotType.HQ) ? friendlyHQAttackRadiusSquared
					: thisRobotType.attackRadiusSquared;

			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(thisRobotLocation,
					attackRadiusSquared, Enemy);

			if (nearbyEnemies.length > 0) {
				MapLocation enemyZeroLocation = nearbyEnemies[0].location;

				if (rc.canAttackLocation(enemyZeroLocation)) {
					rc.attackLocation(enemyZeroLocation);
					return true;

					// Check if the attacker is HQ and the target is in splash
					// range
				} else if (thisRobotType == RobotType.HQ
						&& friendlyTowers.length >= 5) {
					Direction towardsEnemyZero = friendlyHQ
							.directionTo(enemyZeroLocation);
					MapLocation splashEnemyZeroLocation = friendlyHQ
							.subtract(towardsEnemyZero);

					if (rc.canAttackLocation(splashEnemyZeroLocation)) {
						rc.attackLocation(splashEnemyZeroLocation);
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Returns whether a location is considered safe.
	 *
	 * @param loc
	 *            - the location being tested.
	 * @param onlyHQAndTowers
	 *            - considers only HQ and Tower range unsafe.
	 * @param checkFriendlyMissiles
	 *            - considers also being within friendly missile range to be
	 *            unsafe
	 * @return - True if the location is safe, false if it is not.
	 */
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

		// Check if robot is in attacking range of any enemy towers
		if (enemyHQ.distanceSquaredTo(loc) <= enemyHQAttackRadiusSquared) {
			return false;
		}

		// Check if robot is in attacking range of any enemy towers
		for (MapLocation enemyTower : enemyTowers) {
			if (enemyTower.distanceSquaredTo(loc) <= RobotType.TOWER.attackRadiusSquared) {
				return false;
			}
		}

		if (onlyHQAndTowers && !checkFriendlyMissiles) {
			return true;
		}

		// Check if robot is in attacking range of any enemy robots
		if (!onlyHQAndTowers) {
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(
					thisRobotType.sensorRadiusSquared, Enemy);

			for (RobotInfo r : nearbyRobots) {
				if (r.location.distanceSquaredTo(loc) <= r.type.attackRadiusSquared
						&& !(ignoreMinersBeavers && (r.type == RobotType.BEAVER || r.type == RobotType.MINER))) {
					return false;
				}
			}
		}

		/*
		 * Check if robot is within explosion range of any friendly missiles
		 */

		if (checkFriendlyMissiles) {
			RobotInfo[] closeEnemies = rc.senseNearbyRobots(loc, 2, Friend);
			for (RobotInfo r : closeEnemies) {
				if (r.type == RobotType.MISSILE) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Generates a random, valid direction.
	 *
	 * @return the generated direction
	 */
	private static Direction getRandomDirection() {
		return directions[rand.nextInt(8)];
	}

	/**
	 * Moves robot around randomly.
	 *
	 * @throws GameActionException
	 */
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

	/**
	 * Directs a robot toward a given destination.
	 *
	 * @param startLoc
	 *            - starting location from which to move toward the target
	 *            destination in question; default is the current location of
	 *            the robot calling the function
	 * @param dest
	 *            - the target location
	 * @param ignoreSafety
	 *            - boolean to determine whether to call isSafe
	 * @param onlyHQAndTowers
	 *            - checks only HQ and Towers
	 * @param checkFriendlyMissiles
	 *            - considers also being within friendly missile range to be
	 *            unsafe
	 *
	 * @return true if there is a direction that the robot can move towards the
	 *         given destination, false otherwise
	 * @throws GameActionException
	 */

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
						facing = possDirection;
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Searches for enemies that have crossed the halfway point between the two
	 * HQ's and tries to attack them; otherwise, moves around randomly.
	 *
	 * @throws GameActionException
	 */
	private static void defendAndMove() throws GameActionException {

		// TODO: create ordered list that allows "stronger" robots to attack
		// "weaker" robots
		// TODO: create target variable that allows robots to track down the
		// invading robot in
		// question; otherwise, look for another one

		RobotInfo[] incomingEnemies = rc.senseNearbyRobots(friendlyHQ,
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

		} else {
			patrolBorder();
		}
	}

	/**
	 * Determines a direction in which a miner robot (either a BEAVER or MINER)
	 * can mine the most amount of ore.
	 *
	 * @throws GameActionException
	 */
	private static void locateBestOre() throws GameActionException {
		if (rc.isCoreReady()) {
			MapLocation currentLocation = rc.getLocation();

//			int straightRadius = (int) Math.sqrt(thisRobotType.sensorRadiusSquared); // value = 4
//			int diagonalRadius = (int) Math.sqrt(thisRobotType.sensorRadiusSquared / 2.0); // value = 3
			
			double bestOreCount = 0.0;
			MapLocation bestDestination = null;

			MapLocation squareOne;
			MapLocation squareTwo;
			MapLocation squareThree;
			MapLocation squareFour;
			
			for (Direction possDirection : directions) {
				if (directionToInt(possDirection)%2 == 1) { // This is a diagonal direction
					squareOne = currentLocation.add(possDirection, 1);
					squareTwo = currentLocation.add(possDirection, 2);
					squareThree = currentLocation.add(possDirection, 3);
					squareFour = null;
					
				} else { // This is a cardinal direction
					squareOne = currentLocation.add(possDirection, 1);
					squareTwo = currentLocation.add(possDirection, 2);
					squareThree = currentLocation.add(possDirection, 3);
					squareFour = currentLocation.add(possDirection, 4);
				}

				double totalOreCount = 0.0;
				
				// You can't mine ore at a square that is occupied
				// by a structure

				if (!isOccupiedByStructure(squareOne)) {
					totalOreCount += rc.senseOre(squareOne);
				}
				
				if (!isOccupiedByStructure(squareTwo)) {
					totalOreCount += rc.senseOre(squareTwo);
				}
				
				if (!isOccupiedByStructure(squareThree)) {
					totalOreCount += rc.senseOre(squareThree);
				}
			
				if (!isOccupiedByStructure(squareFour)) {
					totalOreCount += rc.senseOre(squareFour);
				}
				
				if (totalOreCount > bestOreCount) {
					bestOreCount = totalOreCount;
					bestDestination = squareThree;
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

	/**
	 * Checks whether a square is occupied by a structure or not; this function
	 * ASSUMES that the location passed in can be sensed by the robot
	 * 
	 * @param loc
	 * @return true if the square is occupied by a structure
	 * @throws GameActionException
	 */
	private static boolean isOccupiedByStructure (MapLocation loc)
			throws GameActionException {
		
		if (loc == null) {
			return true; // vacuously true
		}
		
		// This method is only called by locateBestOre, so the result
		// of calling canSenseLocation(loc) should return true
		
		RobotInfo squareInfo = rc.senseRobotAtLocation(loc);
		
		if(squareInfo == null) {
			return false;
		}
		
		// Structures do not have any supply upkeep
		return squareInfo.type.supplyUpkeep == 0;
	}
	
	/**
	 * Mines at current location and then tries to look for more ore.
	 *
	 * @throws GameActionException
	 */
	private static void mineAndMove() throws GameActionException {
		if (rc.senseOre(rc.getLocation()) > 1) { // if there is ore, try to mine
			if (rc.isCoreReady() && rc.canMine()) {
				rc.mine();
			}
		} else { // otherwise, look for ore
			locateBestOre();
		}
	}

	/**
	 * Gets the corresponding index for each valid Direction
	 *
	 * @param d
	 *            - the direction being indexed
	 * @return integer corresponding to a valid Direction
	 */
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

	/**
	 * Spawns or builds a robot.
	 *
	 * @param roboType
	 *            - the type of robot being built/spawned
	 * @param build
	 *            - True if building, false if spawning
	 * @return - True if building/spawning succeeded
	 * @throws GameActionException
	 */
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

	/**
	 * Transfer supplies between units.
	 *
	 * @throws GameActionException
	 */
	private static void transferSupplies() throws GameActionException {
		// TODO: Do we want to have a global ordering on robots? So that
		// robots may decide to "sacrifice" themselves for the sake of a
		// stronger, more able robot?

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),
				GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Friend);
		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;

		MapLocation suppliesToThisLocation = null;

		for (RobotInfo ri : nearbyAllies) {
			if (ri.type.needsSupply() && ri.supplyLevel < lowestSupply
					&& thisRobotType != ri.type) {
				lowestSupply = ri.supplyLevel;
				transferAmount = (rc.getSupplyLevel() - lowestSupply) / 2;
				suppliesToThisLocation = ri.location;
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
	
	/***
	 * Collects and broadcasts the number of all unit types.
	 *
	 * @throws GameActionException
	 */
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
		int friendlyLoopStart = friendlyChunkSize * roundNumMod;
		// Make sure to read the whole array
		int friendlyLoopEnd = (roundNumMod == 3) ? friendlyRobots.length
				: friendlyChunkSize * (roundNumMod + 1);

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
			break;
		}

	}
	
	/**
	 * Counts the number of void and normal squares as well as determines the
	 * overall dimensions of the board.
	 * 
	 * @throws GameActionException
	 */
	private static void analyzeMap() throws GameActionException {
		if (rc.readBroadcast(DIMENSIONS_FOUND_CHANNEL) == 0) {
			int xmin = rc.readBroadcast(XMIN_VALUE_CHANNEL);
			int ymin = rc.readBroadcast(YMIN_VALUE_CHANNEL);

			while (rc.senseTerrainTile(new MapLocation(xmin, ymin)) != TerrainTile.OFF_MAP
					&& Math.max(friendlyHQ.x, enemyHQ.x) - xmin < GameConstants.MAP_MAX_WIDTH
					&& Math.max(friendlyHQ.y, enemyHQ.y) - ymin < GameConstants.MAP_MAX_HEIGHT) {
				xmin--;
				ymin--;

				if (Clock.getBytecodesLeft() < 100) {
					rc.broadcast(XMIN_VALUE_CHANNEL, xmin);
					rc.broadcast(YMIN_VALUE_CHANNEL, ymin);
					return;
				}
			}

			if (rc.senseTerrainTile(new MapLocation(xmin + 1, ymin)) == TerrainTile.OFF_MAP) {
				ymin++;

				while (rc.senseTerrainTile(new MapLocation(xmin, ymin)) != TerrainTile.OFF_MAP
						&& Math.max(friendlyHQ.x, enemyHQ.x) - xmin < GameConstants.MAP_MAX_WIDTH) {
					xmin--;

					if (Clock.getBytecodesLeft() < 100) {
						rc.broadcast(XMIN_VALUE_CHANNEL, xmin);
						rc.broadcast(YMIN_VALUE_CHANNEL, ymin);
						return;
					}
				}
				xmin++;

			} else if (rc.senseTerrainTile(new MapLocation(xmin, ymin + 1)) == TerrainTile.OFF_MAP) {
				xmin++;

				while (rc.senseTerrainTile(new MapLocation(xmin, ymin)) != TerrainTile.OFF_MAP
						&& Math.max(friendlyHQ.y, enemyHQ.y) - ymin < GameConstants.MAP_MAX_HEIGHT) {
					ymin--;

					if (Clock.getBytecodesLeft() < 100) {
						rc.broadcast(XMIN_VALUE_CHANNEL, xmin);
						rc.broadcast(YMIN_VALUE_CHANNEL, ymin);
						return;
					}
				}
				ymin++;
			}

			rc.broadcast(XMIN_VALUE_CHANNEL, xmin);
			rc.broadcast(YMIN_VALUE_CHANNEL, ymin);
			rc.broadcast(DIMENSIONS_FOUND_CHANNEL, 1); // '1' --> xmin and ymin
														// have been found

			rc.broadcast(CURRENT_XPOS_CHANNEL, xmin);
			rc.broadcast(CURRENT_YPOS_CHANNEL, ymin);

		} else if (rc.readBroadcast(DIMENSIONS_FOUND_CHANNEL) == 1) {
			int xmax = rc.readBroadcast(XMAX_VALUE_CHANNEL);
			int ymax = rc.readBroadcast(YMAX_VALUE_CHANNEL);

			while (rc.senseTerrainTile(new MapLocation(xmax, ymax)) != TerrainTile.OFF_MAP
					&& xmax - Math.min(friendlyHQ.x, enemyHQ.x) < GameConstants.MAP_MAX_WIDTH
					&& ymax - Math.min(friendlyHQ.y, enemyHQ.y) < GameConstants.MAP_MAX_HEIGHT) {
				xmax++;
				ymax++;

				if (Clock.getBytecodesLeft() < 100) {
					rc.broadcast(XMAX_VALUE_CHANNEL, xmax);
					rc.broadcast(YMAX_VALUE_CHANNEL, ymax);
					return;
				}
			}

			if (rc.senseTerrainTile(new MapLocation(xmax - 1, ymax)) == TerrainTile.OFF_MAP) {
				ymax--;

				while (rc.senseTerrainTile(new MapLocation(xmax, ymax)) != TerrainTile.OFF_MAP
						&& xmax - Math.min(friendlyHQ.x, enemyHQ.x) < GameConstants.MAP_MAX_WIDTH) {
					xmax++;

					if (Clock.getBytecodesLeft() < 100) {
						rc.broadcast(XMAX_VALUE_CHANNEL, xmax);
						rc.broadcast(YMAX_VALUE_CHANNEL, ymax);
						return;
					}
				}
				xmax--;

			} else if (rc.senseTerrainTile(new MapLocation(xmax, ymax - 1)) == TerrainTile.OFF_MAP) {
				xmax--;

				while (rc.senseTerrainTile(new MapLocation(xmax, ymax)) != TerrainTile.OFF_MAP
						&& ymax - Math.min(friendlyHQ.y, enemyHQ.y) < GameConstants.MAP_MAX_WIDTH) {
					ymax++;

					if (Clock.getBytecodesLeft() < 100) {
						return;
					}
				}
				ymax--;

				rc.broadcast(XMAX_VALUE_CHANNEL, xmax);
				rc.broadcast(YMAX_VALUE_CHANNEL, ymax);
				rc.broadcast(DIMENSIONS_FOUND_CHANNEL, 2); // '2' --> xmin,
															// xmax, ymin, and
															// ymax have been
															// found
			}
		}

		if (Clock.getBytecodesLeft() < 100) {
			return;
		}

		int xpos = rc.readBroadcast(CURRENT_XPOS_CHANNEL);
		int ypos = rc.readBroadcast(CURRENT_YPOS_CHANNEL);

		int xmax = rc.readBroadcast(XMAX_VALUE_CHANNEL);
		int ymin = rc.readBroadcast(YMIN_VALUE_CHANNEL);
		int ymax = rc.readBroadcast(YMAX_VALUE_CHANNEL);

		int normalSquareCount = rc.readBroadcast(NORMAL_TERRAIN_COUNT_CHANNEL);
		int voidSquareCount = rc.readBroadcast(VOID_TERRAIN_COUNT_CHANNEL);

		while (xpos <= xmax) {
			if (rc.senseTerrainTile(new MapLocation(xpos, ypos)) == TerrainTile.NORMAL) {
				normalSquareCount++;

			} else if (rc.senseTerrainTile(new MapLocation(xpos, ypos)) == TerrainTile.VOID) {
				voidSquareCount++;
			}

			ypos++;

			if (ypos > ymax) {
				ypos = ymin;
				xpos++;
			}

			if (Clock.getBytecodesLeft() < 150) {
				rc.broadcast(CURRENT_XPOS_CHANNEL, xpos);
				rc.broadcast(CURRENT_YPOS_CHANNEL, ypos);

				rc.broadcast(NORMAL_TERRAIN_COUNT_CHANNEL, normalSquareCount);
				rc.broadcast(VOID_TERRAIN_COUNT_CHANNEL, voidSquareCount);
				return;
			}
		}

		rc.broadcast(NORMAL_TERRAIN_COUNT_CHANNEL, normalSquareCount);
		rc.broadcast(VOID_TERRAIN_COUNT_CHANNEL, voidSquareCount);
		rc.broadcast(TERRAIN_ANALYZED_CHANNEL, 1); // '1' --> entire map has
													// been analyzed
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
	 * 70-99: Miscellaneous
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
	private static final int NUM_FRIENDLY_LAUNCHERS_CHANNEL = 18;
	private static final int NUM_FRIENDLY_MISSILES_CHANNEL = 19;

	// Map Analysis
	private static final int DIMENSIONS_FOUND_CHANNEL = 2001;
	private static final int XMIN_VALUE_CHANNEL = 2002;
	private static final int XMAX_VALUE_CHANNEL = 2003;
	private static final int YMIN_VALUE_CHANNEL = 2004;
	private static final int YMAX_VALUE_CHANNEL = 2005;
	private static final int CURRENT_XPOS_CHANNEL = 2006;
	private static final int CURRENT_YPOS_CHANNEL = 2007;
	private static final int TERRAIN_ANALYZED_CHANNEL = 2008;
	private static final int NORMAL_TERRAIN_COUNT_CHANNEL = 2009;
	private static final int VOID_TERRAIN_COUNT_CHANNEL = 2010;
}
