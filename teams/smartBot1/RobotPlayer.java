package smartBot1;

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

	private static int friendlyHQCurrentDirectionIndex;
	private static int halfwayDistance;
	
	private static Direction facing;
	private static Direction[] directions = { Direction.NORTH,
		Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
		Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
		Direction.NORTH_WEST };
	
	// Missile only
	private static int turnsRemaining;
	
	// HQ only
	private static int xmin;
	private static int xmax;
	private static int ymin;
	private static int ymax;
	
	private static int xpos;
	private static int ypos;
	
	private static int voidSquareCount;
	private static int normalSquareCount;
	
	private static boolean done = false;
	
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
		friendlyHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		friendlyTowers = rc.senseTowerLocations();
		enemyTowers = rc.senseEnemyTowerLocations();
		
		if(thisRobotType == RobotType.HQ){
			xmin = Math.min(friendlyHQ.x, enemyHQ.x);
			xmax = Math.max(friendlyHQ.x, enemyHQ.x);
			
			ymin = Math.min(friendlyHQ.y, enemyHQ.y);
			ymax = Math.max(friendlyHQ.y, enemyHQ.y);
			
			xpos = -1;
			ypos = -1;
		}
		
		if(thisRobotType == RobotType.MISSILE){
			turnsRemaining = GameConstants.MISSILE_LIFESPAN;
		}
		
		friendlyHQCurrentDirectionIndex = 0;
		
		// Slightly less to avoid tower issues
		halfwayDistance = (int) Math.pow((0.9 / 2.0) * Math.pow(friendlyHQ.distanceSquaredTo(enemyHQ), 0.5), 2);
		
		rand = new Random(rc.getID());
		facing = getRandomDirection(); // Randomize starting direction
		
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
				friendlyHQAttackRadiusSquared = friendlyTowers.length >= 5 ? GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED
						: RobotType.HQ.attackRadiusSquared;
				enemyHQAttackRadiusSquared = enemyTowers.length >= 5 ? GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED
						: RobotType.HQ.attackRadiusSquared;

				// Choose an action based on the type of robot.
				switch (thisRobotType) {
				
				case AEROSPACELAB:
					break;
				
				case BARRACKS:
					break;
				
				case BASHER:
					attackEnemyZero();
					defendAndMove();
					break;
					
				case BEAVER:
					// Number of units
					int numFriendlyMinerFactories = rc
							.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL);
					int numFriendlyHelipads = rc
							.readBroadcast(NUM_FRIENDLY_HELIPAD_CHANNEL);

					// Building Order/Preferences
					if (roundNum < 1800 && numFriendlyMinerFactories < 2) {
						createUnit(RobotType.MINERFACTORY, true);
					} else if (numFriendlyHelipads < 1) {
						createUnit(RobotType.HELIPAD, true);
					} else {
						locateBestOre();
					}
					
					break;
				
				case COMMANDER:
					break;
				
				case COMPUTER:
					break;
				
				case DRONE:
					break;
				
				case HANDWASHSTATION:
					break;
				
				case HELIPAD:
					if (rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL) < 20) {
						createUnit(RobotType.DRONE, false);
					}
					break;
				
				case HQ:
					/**********************************************************
					 * HQ Calculations over various rounds
					 * ========================================================
					 * Unit Counting -------------------------------> 5 Rounds
					 * Tower Strength -------------------------------> 1 Round
					 *********************************************************/
					if (roundNum % 5 < 5) {
						// Update unit counts every so often!
						updateUnitCounts();
					}
					if (roundNum % 5 == 4) {
						// Check tower strength!
						analyzeTowerStrength();
					}

					/*
					 * We call this method before spawning beavers because we
					 * probably will want to spawn more efficient creatures such
					 * as miners; in addition, spawning introduces attacking
					 * delay.
					 */
					attackEnemyZero();

					// Maintain only a few beavers
					if (rc.readBroadcast(NUM_FRIENDLY_BEAVERS_CHANNEL) < 2) {
						createUnit(RobotType.BEAVER, false);
					}
					
					if(!done){
						analyzeMap();
					}
					break;
					
				case LAUNCHER:
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
							&& miningFate <= Math.pow(Math.E,
									-minerCount * 0.07)) {
						createUnit(RobotType.MINER, false);
					}
					break;
				
				case MISSILE:
					if(friendEnemyRatio(null, RobotType.MISSILE.attackRadiusSquared, Enemy)
							>= 1){
						rc.explode();
					}
					
					/* check if towers or HQ might be in reach; if it is the case,
					 * then is quite likely that there is an attack on the tower or HQ
					 * given that launchers are difficult to build and missiles have
					 * very limited life-spans
					 */
					
					MapLocation currentLocation = rc.getLocation();
					
					int maxMovementCount = (int) (turnsRemaining / RobotType.MISSILE.movementDelay);
					int maxRadiusSquared = (int) Math.pow(maxMovementCount + Math.pow(
							RobotType.MISSILE.attackRadiusSquared, 0.5), 2);
							 
					if(currentLocation.distanceSquaredTo(enemyHQ) <= maxRadiusSquared){
						moveTowardDestination(enemyHQ, true, false);
					} else {
						for(MapLocation towerLocation : enemyTowers){
							if(currentLocation.distanceSquaredTo(towerLocation) <= maxRadiusSquared){
								moveTowardDestination(enemyHQ, true, false);
								break;
							}
						}
					}
					
					/* locate best location where there are more enemies than friend robots
					 * and aim to explode at that location
					 */
					
					double bestFriendEnemyRatio = 0.0;
					MapLocation bestTarget = null;
					
					for(int movementIndex = 0; movementIndex < maxMovementCount; movementIndex++){
						for(Direction possDir: directions){
							MapLocation possSquare = currentLocation.add(possDir, movementIndex);
							double possFriendEnemyRatio = friendEnemyRatio(possSquare,
									RobotType.MISSILE.attackRadiusSquared, Enemy);
							if(possFriendEnemyRatio >= bestFriendEnemyRatio){
								bestTarget = possSquare;
								bestFriendEnemyRatio = possFriendEnemyRatio;
							}
						}
					}
					
					if(bestTarget != null){
						moveTowardDestination(bestTarget, true, false);
					}else{
						defendAndMove();
					}
					
					break;
				
				case SOLDIER:
					attackEnemyZero();
					defendAndMove();
					break;
					
				case SUPPLYDEPOT:
					break;
				
				case TANK:
					attackEnemyZero();
					defendAndMove();
					break;
					
				case TANKFACTORY:
					break;
				
				case TECHNOLOGYINSTITUTE:
					break;
				
				case TOWER:
					break;
				
				case TRAININGFIELD:
					break;
				
				default:
					break;

				}

				// Missiles have too low of a bytecode limit to transfer
				// supplies.
				if (thisRobotType != RobotType.MISSILE) {
					/**********************************************************
					 * If robots go low on supplies, they will become less
					 * effective in attacking; HQ supplies goods at a constant
					 * rate + any additional units from having supply depots
					 * built; these units are to be passed from HQ among the
					 * robots in a way such that all robots are sufficiently
					 * supplied.
					 * 
					 * NOTE: Robots that are low on supplies will have a white
					 * square around them.
					 *********************************************************/

					transferSupplies();
				}

			} catch (GameActionException e) {
				e.printStackTrace();
			}

			rc.yield(); // End the robot's turn to save bytecode
			
		}

	}

	/**
	 * Gives the ratio of friend to enemy robots around a given location that
	 * are at most a given distance away
	 * 
	 * @param loc
	 * @param radiusSquared
	 * @param friendTeam
	 * @return
	 */
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

	/**
	 * Attacks the first enemy in the list.
	 * 
	 * @return True if an attack was successfully carried out
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
								attackRadiusSquared
										+ GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED,
								Enemy);

				if (splashRangeEnemies.length > 0) {
					MapLocation actualEnemyZeroLocation = splashRangeEnemies[0].location;
					Direction towardsEnemyZero = friendlyHQ
							.directionTo(actualEnemyZeroLocation);
					MapLocation splashEnemyZeroLocation = friendlyHQ
							.add(towardsEnemyZero);

					if (rc.canAttackLocation(splashEnemyZeroLocation)) {
						rc.attackLocation(splashEnemyZeroLocation);
						return true;
					}

				}
			}
		}
		
		return false;
	}

	/***************************************************************************
	 * Returns whether a location is considered safe.
	 * 
	 * @param loc
	 *            - The location being tested.
	 * @param onlyHQAndTowers
	 *            - Considers only HQ and Tower range unsafe.
	 * @return - True if the location is safe, false if it is not.
	 **************************************************************************/
	private static boolean isSafe(MapLocation loc, boolean onlyHQAndTowers)
			throws GameActionException {
		TerrainTile locTerrain = rc.senseTerrainTile(loc);

		if (locTerrain != TerrainTile.NORMAL) {
			if (!(locTerrain == TerrainTile.VOID && (thisRobotType == RobotType.DRONE || thisRobotType == RobotType.MISSILE))) {
				return false;
			}
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

		// Check if any enemies are in range
		if (!onlyHQAndTowers) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(
					thisRobotType.sensorRadiusSquared, Enemy);
			for (RobotInfo r : enemyRobots) {
				if (r.location.distanceSquaredTo(loc) <= r.type.attackRadiusSquared) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Generates a random, valid direction
	 *  
	 * @return
	 */
	private static Direction getRandomDirection() {
		return Direction.values()[(int) rand.nextDouble() * 8];
	}
	
	/**
	 * Moves robot around randomly
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
			boolean tileInFrontSafe = isSafe(tileInFrontLocation, false);
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
	 * Directs a robot toward a given destination
	 * 
	 * @param dest
	 * @param ignoreSafety
	 * @param onlyHQAndTowers
	 * 
	 * @return True if there is a direction that the robot can move
	 * 		   towards the given destination
	 * @throws GameActionException
	 */
	private static boolean moveTowardDestination(MapLocation dest,
			boolean ignoreSafety, boolean onlyHQAndTowers)
			throws GameActionException {
		// TODO: Should we consider including a "crowdedness" heuristic? If so,
		// how do we incorporate our current implementation?

		Direction straight = rc.getLocation().directionTo(dest);
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

					if (ignoreSafety || isSafe(possSquare, onlyHQAndTowers)) {
						rc.move(possDirection);
						return true;
					}
				}
			}
		}

		return false;
	}

	private static void defendAndMove() throws GameActionException{
		/**
		 * Searches for enemies that have crossed the halfway point between the two
		 * HQ's and tries to attack them; otherwise, moves around randomly
		 * 
		 * @throws GameActionException
		 */
		
		// TODO: create ordered list that allows "stronger" robots to attack "weaker" robots
		// TODO: create target variable that allows robots to track down the invading robot in
		//		 question; otherwise, look for another one
		
		RobotInfo[] incomingEnemies = rc.senseNearbyRobots(friendlyHQ, halfwayDistance, Enemy);
		boolean weaponReady = rc.isWeaponReady();
		MapLocation targetLocation = null;
		
		for(RobotInfo robot : incomingEnemies){
			if(weaponReady && rc.canAttackLocation(robot.location)){
				rc.attackLocation(robot.location);
				return;
				
			}else if(targetLocation == null){
				targetLocation = robot.location;
			}
		}
		
		if(targetLocation != null){
			moveTowardDestination(targetLocation, false, false);
		}else{
			moveAround();
		}
	}
	
	/**
	 * Determines a direction in which a miner robot (either a BEAVER or MINER) can
	 * mine the most amount of ore
	 *  
	 * @throws GameActionException
	 */
	private static void locateBestOre() throws GameActionException {
		if (rc.isCoreReady()) {
			MapLocation currentLocation = rc.getLocation();

			int radius = (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);

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
				} else if (totalOreCount == bestOreCount
						&& bestDestination != null) {
					bestDestination = (rand.nextDouble() > 0.5) ? bestDestination
							: squareFour;
				}
			}

			if (bestDestination != null) {
				moveTowardDestination(bestDestination, false, false);
			} else {
				// TODO: Keep track of last destination traveled to by a miner?
				moveAround();
			}
		}
	}

	/**
	 * Mines at current location and then tries to look for more ore
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

	/**************************************************************************
	 * Spawns or builds a robot.
	 * 
	 * @param roboType
	 *            - the type of robot being built/spawned
	 * @param build
	 *            - True if building, false if spawning
	 * @return - True if building/spawning succeeded
	 * @throws GameActionException
	 *************************************************************************/
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
							&& isSafe(testLoc, false)) {
						rc.build(testDir, roboType);
						return true;
					}
				} else { // spawning
					if (rc.canSpawn(testDir, roboType)
							&& isSafe(testLoc, false)) {
						rc.spawn(testDir, roboType);
						return true;
					}
				}

				testDir = goLeft ? testDir.rotateLeft() : testDir.rotateRight();
			}
		}

		return false;
	}

	/**************************************************************************
	 * Transfer supplies between units.
	 * 
	 * @throws GameActionException
	 *************************************************************************/
	private static void transferSupplies() throws GameActionException {
		// TODO: Do we want to have a global ordering on robots? So that
		// robots may decide to "sacrifice" themselves for the sake of a
		// stronger, more able robot?

		if (!thisRobotType.needsSupply()) {
			RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),
					GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Friend);
			double lowestSupply = rc.getSupplyLevel();
			double transferAmount = 0;

			MapLocation suppliesToThisLocation = null;

			for (RobotInfo ri : nearbyAllies) {
				if ((ri.type == RobotType.BEAVER
						|| ri.type == RobotType.COMPUTER
						|| ri.type == RobotType.SOLDIER
						|| ri.type == RobotType.BASHER
						|| ri.type == RobotType.MINER
						|| ri.type == RobotType.DRONE
						|| ri.type == RobotType.TANK
						|| ri.type == RobotType.COMMANDER || ri.type == RobotType.LAUNCHER)
						&& ri.supplyLevel < lowestSupply) {
					lowestSupply = ri.supplyLevel;
					transferAmount = (rc.getSupplyLevel() - lowestSupply) / 2;
					suppliesToThisLocation = ri.location;
				}
			}

			if (suppliesToThisLocation != null) {
				rc.transferSupplies((int) transferAmount,
						suppliesToThisLocation);
			}
		}
	}

	/**
	 * Analyzes the strength of the towers in this particular map based on their
	 * quantity and proximity to each other and HQ.
	 * 
	 * @throws GameActionException
	 */
	private static void analyzeTowerStrength() throws GameActionException {
		int towerStrength = 0;

		// One or no towers -> very weak. Keep at 0.
		// Otherwise measure strength based on closeness.
		for (int i = 0; i < friendlyTowers.length; ++i) {
			if (friendlyTowers[i].distanceSquaredTo(friendlyHQ) < 48) {
				towerStrength += 2; /*
									 * HQ can inflict thrice the damage but has
									 * the same delay compared to a tower (except
									 * when tower count is less than 5, in which
									 * case, it has double the delay)
									 */
			}
			for (int j = i; j < friendlyTowers.length; ++j) {
				if (friendlyTowers[i].distanceSquaredTo(friendlyTowers[j]) < 48) {
					towerStrength += 1;
				}
			}
		}

		rc.broadcast(TOWER_STRENGTH_CHANNEL, towerStrength);
	}

	/**
	 * Counts the number of void and normal squares as well as determines the overall
	 * dimensions of the board
	 * 
	 * @throws GameActionException
	 */
	private static void analyzeMap() throws GameActionException{
		while(rc.senseTerrainTile(new MapLocation(xmin, ymin)) != TerrainTile.OFF_MAP){
			xmin--;
			ymin--;
			
			if(Clock.getBytecodesLeft() < 100){
				return;
			}
		}
		
		if(rc.senseTerrainTile(new MapLocation(xmin + 1, ymin)) == TerrainTile.OFF_MAP){
			ymin++;
			
			while(rc.senseTerrainTile(new MapLocation(xmin, ymin)) != TerrainTile.OFF_MAP){
				xmin--;
				
				if(Clock.getBytecodesLeft() < 100){
					return;
				}
			}
			
			xmin++;
			
		}else if(rc.senseTerrainTile(new MapLocation(xmin, ymin + 1)) == TerrainTile.OFF_MAP){
			xmin++;
			
			while(rc.senseTerrainTile(new MapLocation(xmin, ymin)) != TerrainTile.OFF_MAP){
				ymin--;
				
				if(Clock.getBytecodesLeft() < 100){
					return;
				}
			}
			
			ymin++;
		}
		
		while(rc.senseTerrainTile(new MapLocation(xmax, ymax)) != TerrainTile.OFF_MAP){
			xmax++;
			ymax++;
			
			if(Clock.getBytecodesLeft() < 100){
				return;
			}
		}
		
		if(rc.senseTerrainTile(new MapLocation(xmax - 1, ymax)) == TerrainTile.OFF_MAP){
			ymax--;
			
			while(rc.senseTerrainTile(new MapLocation(xmax, ymax)) != TerrainTile.OFF_MAP){
				xmax++;
				
				if(Clock.getBytecodesLeft() < 100){
					return;
				}
			}
			
			xmax--;
			
		}else if(rc.senseTerrainTile(new MapLocation(xmax, ymax - 1)) == TerrainTile.OFF_MAP){
			xmax--;
			
			while(rc.senseTerrainTile(new MapLocation(xmax, ymax)) != TerrainTile.OFF_MAP){
				ymax++;
				
				if(Clock.getBytecodesLeft() < 100){
					return;
				}
			}
			
			ymax--;
		}
		
		rc.broadcast(MAP_WIDTH_CHANNEL, xmax - xmin);
		rc.broadcast(MAP_HEIGHT_CHANNEL, ymax - ymin);
		
		if(Clock.getBytecodesLeft() < 100){
			return;
		}
		
		xpos = xmin;
		ypos = ymin;

		while(xpos <= xmax){
			if(rc.senseTerrainTile(new MapLocation(xpos, ypos)) == TerrainTile.NORMAL){
				normalSquareCount++;
			}else if(rc.senseTerrainTile(new MapLocation(xpos, ypos)) == TerrainTile.VOID){
				voidSquareCount++;
			}
			
			ypos++;
			
			if(ypos > ymax){
				ypos = ymin;
				xpos++;
			}
			
			if(Clock.getBytecodesLeft() < 100){
				return;
			}
		}
		
		rc.broadcast(MAP_NORMAL_SQUARES_CHANNEL, normalSquareCount);
		rc.broadcast(MAP_VOID_SQUARES_CHANNEL, voidSquareCount);
		
		done = true;
	}
	
	/***************************************************************************
	 * Broadcasts information about swarming to the troops specified in types.
	 * 
	 * @param types
	 *            - The troops reading the swarming information.
	 * @param action
	 *            - The action to be taken.
	 * @param location
	 *            - The location to go to (if not null).
	 * @throws GameActionException
	 **************************************************************************/
	private static void broadcastSwarmInfo(RobotType[] types, int action,
			MapLocation location) throws GameActionException {

		// Broadcast location only if a new one was provided
		if (location != null) {
			int x = location.x;
			int y = location.y;
			for (RobotType type : types) {
				switch (type) {
				case BASHER:
					rc.broadcast(BASHER_SWARM_CHANNEL, action);
					rc.broadcast(BASHER_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(BASHER_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case BEAVER:
					rc.broadcast(BEAVER_SWARM_CHANNEL, action);
					rc.broadcast(BEAVER_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(BEAVER_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case COMMANDER:
					rc.broadcast(COMMANDER_SWARM_CHANNEL, action);
					rc.broadcast(COMMANDER_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(COMMANDER_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case COMPUTER:
					rc.broadcast(COMPUTER_SWARM_CHANNEL, action);
					rc.broadcast(COMPUTER_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(COMPUTER_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case DRONE:
					rc.broadcast(DRONE_SWARM_CHANNEL, action);
					rc.broadcast(DRONE_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(DRONE_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case MINER:
					rc.broadcast(MINER_SWARM_CHANNEL, action);
					rc.broadcast(MINER_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(MINER_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case MISSILE:
					rc.broadcast(MISSILE_SWARM_CHANNEL, action);
					rc.broadcast(MISSILE_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(MISSILE_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case SOLDIER:
					rc.broadcast(SOLDIER_SWARM_CHANNEL, action);
					rc.broadcast(SOLDIER_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(SOLDIER_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				case TANK:
					rc.broadcast(TANK_SWARM_CHANNEL, action);
					rc.broadcast(TANK_SWARM_LOCATION_X_CHANNEL, x);
					rc.broadcast(TANK_SWARM_LOCATION_Y_CHANNEL, y);
					break;
				default:
					break;

				}
			}
		} else {
			for (RobotType type : types) {
				switch (type) {
				case BASHER:
					rc.broadcast(BASHER_SWARM_CHANNEL, action);
					break;
				case BEAVER:
					rc.broadcast(BEAVER_SWARM_CHANNEL, action);
					break;
				case COMMANDER:
					rc.broadcast(COMMANDER_SWARM_CHANNEL, action);
					break;
				case COMPUTER:
					rc.broadcast(COMPUTER_SWARM_CHANNEL, action);
					break;
				case DRONE:
					rc.broadcast(DRONE_SWARM_CHANNEL, action);
					break;
				case MINER:
					rc.broadcast(MINER_SWARM_CHANNEL, action);
					break;
				case MISSILE:
					rc.broadcast(MISSILE_SWARM_CHANNEL, action);
					break;
				case SOLDIER:
					rc.broadcast(SOLDIER_SWARM_CHANNEL, action);
					break;
				case TANK:
					rc.broadcast(TANK_SWARM_CHANNEL, action);
					break;
				default:
					break;

				}
			}
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
		int friendlyLoopEnd = roundNumMod == 4 ? friendlyRobots.length
				: friendlyChunkSize * (roundNumMod + 1);
		int enemyLoopStart = enemyChunkSize * roundNumMod;
		// Make sure to read the whole array
		int enemyLoopEnd = roundNumMod == 4 ? enemyRobots.length
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

	// Coordinate Planning + Rallying Points
	private static final int HQ_RADIUS_CHANNEL = 100;
	// TODO: Use a single channel and some int manipulation to reduce number of
	// channels for location broadcasting
	private static final int BEAVER_SWARM_LOCATION_X_CHANNEL = 201;
	private static final int BEAVER_SWARM_LOCATION_Y_CHANNEL = 211;
	private static final int MINER_SWARM_LOCATION_X_CHANNEL = 202;
	private static final int MINER_SWARM_LOCATION_Y_CHANNEL = 212;
	private static final int COMPUTER_SWARM_LOCATION_X_CHANNEL = 203;
	private static final int COMPUTER_SWARM_LOCATION_Y_CHANNEL = 213;
	private static final int SOLDIER_SWARM_LOCATION_X_CHANNEL = 204;
	private static final int SOLDIER_SWARM_LOCATION_Y_CHANNEL = 214;
	private static final int BASHER_SWARM_LOCATION_X_CHANNEL = 205;
	private static final int BASHER_SWARM_LOCATION_Y_CHANNEL = 215;
	private static final int DRONE_SWARM_LOCATION_X_CHANNEL = 206;
	private static final int DRONE_SWARM_LOCATION_Y_CHANNEL = 216;
	private static final int TANK_SWARM_LOCATION_X_CHANNEL = 207;
	private static final int TANK_SWARM_LOCATION_Y_CHANNEL = 217;
	private static final int COMMANDER_SWARM_LOCATION_X_CHANNEL = 208;
	private static final int COMMANDER_SWARM_LOCATION_Y_CHANNEL = 218;
	private static final int MISSILE_SWARM_LOCATION_X_CHANNEL = 209;
	private static final int MISSILE_SWARM_LOCATION_Y_CHANNEL = 219;

	// Offensive + Defensive Signals
	private static final int BEAVER_SWARM_CHANNEL = 1001;
	private static final int MINER_SWARM_CHANNEL = 1002;
	private static final int COMPUTER_SWARM_CHANNEL = 1003;
	private static final int SOLDIER_SWARM_CHANNEL = 1004;
	private static final int BASHER_SWARM_CHANNEL = 1005;
	private static final int DRONE_SWARM_CHANNEL = 1006;
	private static final int TANK_SWARM_CHANNEL = 1007;
	private static final int COMMANDER_SWARM_CHANNEL = 1008;
	private static final int MISSILE_SWARM_CHANNEL = 1009;

	// Map Analysis
	private static final int TOWER_STRENGTH_CHANNEL = 2000;
	private static final int MAP_WIDTH_CHANNEL = 2001;
	private static final int MAP_HEIGHT_CHANNEL = 2002;
	private static final int MAP_VOID_SQUARES_CHANNEL = 2003;
	private static final int MAP_NORMAL_SQUARES_CHANNEL = 2004;
	
	// ------------------------------------------------------------------------
	// Action Constants
	// ------------------------------------------------------------------------
	private static final int NO_ACTION = 0;
	private static final int GO_TO_LOCATION = 1;

}
