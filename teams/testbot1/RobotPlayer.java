package testbot1;

import battlecode.common.*;

import java.util.Random;

/**
 * Keeping everything static to save the bytecode from the this statement
 * instances produce!
 */
public class RobotPlayer {

	/**************************************************************************
	 * BROADCAST _CHANNELS
	 * ------------------------------------------------------------------------
	 * NOTE: Building/Units order follows directly from online documentation.
	 * ------------------------------------------------------------------------
	 * 0-9: Friendly Buildings
	 * ------------------------------------------------------------------------
	 * 10-19: Friendly Units
	 * ------------------------------------------------------------------------
	 * 20-29: Enemy Buildings
	 * ------------------------------------------------------------------------
	 * 30-39: Enemy Units
	 *************************************************************************/
	// Friendly Buildings Channels
	private static final int NUM_FRIENDLY_SUPPLY_DEPOT_CHANNEL = 1;
	private static final int NUM_FRIENDLY_MINER_FACTORY_CHANNEL = 2;
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

				switch (rc.getType()) {
				case HQ:
					/**********************************************************
					 * Update unit counts every so often!
					 *********************************************************/
					if (Clock.getRoundNum() % 20 == 0) {
						updateUnitCounts();
					}
					attackEnemyZero(); /*
										 * we can this method first before
										 * spawning beavers because we probably
										 * will want to spawn more efficient
										 * creatures such as miners; in
										 * addition, spawning introduces
										 * attacking delay
										 */
					// Limit number of beavers
					if (rc.readBroadcast(NUM_FRIENDLY_BEAVERS_CHANNEL) < 10) {
						spawnUnit(RobotType.BEAVER);
					}
				case AEROSPACELAB:
					spawnUnit(RobotType.LAUNCHER);
					break;
				case BARRACKS:
					if (fate < .7) {
						spawnUnit(RobotType.SOLDIER);
					} else {
						spawnUnit(RobotType.BASHER);
					}
					break;
				case BASHER:
					// BASHERs attack automatically, so let's just move around
					// mostly randomly
					moveAround();
					break;
				case BEAVER:
					attackEnemyZero();

					if (Clock.getRoundNum() < 700) {
						buildUnit(RobotType.MINERFACTORY);
					}

					/*
					 * P(TRAININGFIELD) = 0.025 
					 * P(TECHNOLOGYINSTITUTE) = 0.025
					 * P(HANDWASHSTATION) = 0.05
					 * P(MINERFACTORY) = 0.1
					 * P(SUPPLYDEPOT) = 0.15
					 * P(TANKFACTORY) = 0.15
					 * P(HELIPAD) = 0.15
					 * P(AEROSPACELAB) = 0.15
					 * P(BARRACKS) = 0.2
					 */

					else if (fate < 0.025) {
						buildUnit(RobotType.TRAININGFIELD);
					} else if (0.025 <= fate && fate < 0.05) {
						buildUnit(RobotType.TECHNOLOGYINSTITUTE);
					} else if (0.05 <= fate && fate < 0.1) {
						buildUnit(RobotType.HANDWASHSTATION);
					} else if (0.1 <= fate && fate < 0.2) {
						buildUnit(RobotType.MINERFACTORY);
					} else if (0.2 <= fate && fate < 0.35) {
						buildUnit(RobotType.SUPPLYDEPOT);
					} else if (0.35 <= fate && fate < 0.5) {
						buildUnit(RobotType.TANKFACTORY);
					} else if (0.5 <= fate && fate < 0.65) {
						buildUnit(RobotType.HELIPAD);
					} else if (0.65 <= fate && fate < 0.8) {
						buildUnit(RobotType.AEROSPACELAB);
					} else {
						buildUnit(RobotType.BARRACKS);
					}

					mineAndMove();

					break;
				case COMMANDER:
					break;
				case COMPUTER:
					break;
				case DRONE:
					attackEnemyZero();
					moveAround();
					break;
				case HANDWASHSTATION:
					break;
				case HELIPAD:
					spawnUnit(RobotType.DRONE);
					break;
				case LAUNCHER:
					rc.launchMissile(getRandomDirection()); // TODO: Fix later
					break;
				case MINER:
					attackEnemyZero();
					mineAndMove();
					break;
				case MINERFACTORY:
					spawnUnit(RobotType.MINER);
					break;
				case MISSILE:
					rc.explode();
					break;
				case SOLDIER:
					attackEnemyZero(); // soldiers attack, not mine
					moveAround(); /*
								 * POSSIBLE OPTIMIZATION: chase enemies In
								 * addition, soldiers need to attack towers
								 * eventually, so they will have to move within
								 * attacking range of the towers, which is not
								 * possible under moveAround()
								 */
					break;
				case SUPPLYDEPOT:
					break;
				case TANK:
					attackEnemyZero();
					moveAround();
					break;
				case TANKFACTORY:
					spawnUnit(RobotType.TANK);
					break;
				case TECHNOLOGYINSTITUTE:
					break;
				case TOWER:
					attackEnemyZero(); // basic attacking method
					break;
				case TRAININGFIELD:
					break;
				default:
					break;
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
		Direction testDir = getRandomDirection();

		for (int turnCount = 0; turnCount < 8; turnCount++) {
			if (rc.isCoreReady() && rc.canSpawn(testDir, roboType)) {
				MapLocation spawnLoc = rc.getLocation().add(testDir);

				if (isSafe(spawnLoc)) {
					rc.spawn(testDir, roboType);
					break;
				}
			} else {
				testDir = testDir.rotateLeft();
			}
		}
	}

	private static Direction getRandomDirection() {
		return Direction.values()[(int) rand.nextDouble() * 8];
	}

	private static void moveAround() throws GameActionException {
		if (rand.nextDouble() < 0.05) {
			if (rand.nextDouble() < 0.5) {
				facing = facing.rotateLeft(); // 45 degree turn
			} else {
				facing = facing.rotateRight();
			}
		}
		if (rc.isCoreReady() && rc.canMove(facing)) {
			MapLocation tileInFrontLocation = rc.getLocation().add(facing);
			boolean tileInFrontSafe = isSafe(tileInFrontLocation);
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

	private static boolean isSafe(MapLocation loc) {
		TerrainTile locTerrain = rc.senseTerrainTile(loc);
		RobotType roboType = rc.getType();
		boolean safeSquare = true;

		if (locTerrain != TerrainTile.NORMAL) {
			if (!(locTerrain == TerrainTile.VOID && (roboType == RobotType.DRONE || roboType == RobotType.MISSILE))) {
				safeSquare = false;
			}
		}

		if (!safeSquare) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(
					roboType.sensorRadiusSquared, Enemy);
			for (RobotInfo r : enemyRobots) {
				if (r.location.distanceSquaredTo(loc) <= r.type.attackRadiusSquared) {
					safeSquare = false;
					break;
				}
			}
		}

		return safeSquare;
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

	private static void buildUnit(RobotType roboType)
			throws GameActionException {
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

	private static void updateUnitCounts() throws GameActionException {
		// Collect all robots into separate RobotInfo arrays.
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(999999, rc.getTeam()
				.opponent());

		// FRIENDLY
		// Buildings
		int numFriendlySupplyDepot = 0;
		int numFriendlyMinerFactory = 0;
		int numFriendlyTechInstitute = 0;
		int numFriendlyBarracks = 0;
		int numFriendlyHelipad = 0;
		int numFriendlyTrainingField = 0;
		int numFriendlyTankFactory = 0;
		int numFriendlyAerospaceLab = 0;
		int numFriendlyHandwashStation = 0;
		// Other units
		int numFriendlyBeavers = 0;
		int numFriendlyMiners = 0;
		int numFriendlyComputers = 0;
		int numFriendlySoldiers = 0;
		int numFriendlyBashers = 0;
		int numFriendlyDrones = 0;
		int numFriendlyTanks = 0;
		int numFriendlyCommanders = 0;
		int numFriendlyLaunchers = 0;
		int numFriendlyMissiles = 0;

		// ENEMY
		// Buildings
		int numEnemySupplyDepot = 0;
		int numEnemyMinerFactory = 0;
		int numEnemyTechInstitute = 0;
		int numEnemyBarracks = 0;
		int numEnemyHelipad = 0;
		int numEnemyTrainingField = 0;
		int numEnemyTankFactory = 0;
		int numEnemyAerospaceLab = 0;
		int numEnemyHandwashStation = 0;
		// Other units
		int numEnemyBeavers = 0;
		int numEnemyMiners = 0;
		int numEnemyComputers = 0;
		int numEnemySoldiers = 0;
		int numEnemyBashers = 0;
		int numEnemyDrones = 0;
		int numEnemyTanks = 0;
		int numEnemyCommanders = 0;
		int numEnemyLaunchers = 0;
		int numEnemyMissiles = 0;

		/*
		 * Our Robots
		 */
		for (RobotInfo r : myRobots) {
			switch(r.type) {
			case AEROSPACELAB:
				++numFriendlyAerospaceLab;
				break;
			case BARRACKS:
				++numFriendlyBarracks;
				break;
			case BASHER:
				++numFriendlyBashers;
				break;
			case BEAVER:
				++numFriendlyBeavers;
				break;
			case COMMANDER:
				++numFriendlyCommanders;
				break;
			case COMPUTER:
				++numFriendlyComputers;
				break;
			case DRONE:
				++numFriendlyDrones;
				break;
			case HANDWASHSTATION:
				++numFriendlyHandwashStation;
				break;
			case HELIPAD:
				++numFriendlyHelipad;
				break;
			case HQ:
				// No need to count HQ!
				break;
			case LAUNCHER:
				++numFriendlyLaunchers;
				break;
			case MINER:
				++numFriendlyMiners;
				break;
			case MINERFACTORY:
				++numFriendlyMinerFactory;
				break;
			case MISSILE:
				++numFriendlyMissiles;
				break;
			case SOLDIER:
				++numFriendlySoldiers;
				break;
			case SUPPLYDEPOT:
				++numFriendlySupplyDepot;
				break;
			case TANK:
				++numFriendlyTanks;
				break;
			case TANKFACTORY:
				++numFriendlyTankFactory;
				break;
			case TECHNOLOGYINSTITUTE:
				++numFriendlyTechInstitute;
				break;
			case TOWER:
				break;
			case TRAININGFIELD:
				++numFriendlyTrainingField;
				break;
			default:
				break;
			
			}
		}
		// Friendly Buildings Broadcasts
		rc.broadcast(NUM_FRIENDLY_SUPPLY_DEPOT_CHANNEL, numFriendlySupplyDepot);
		rc.broadcast(NUM_FRIENDLY_MINER_FACTORY_CHANNEL,
				numFriendlyMinerFactory);
		rc.broadcast(NUM_FRIENDLY_TECHINSTITUTE_CHANNEL,
				numFriendlyTechInstitute);
		rc.broadcast(NUM_FRIENDLY_BARRACKS_CHANNEL, numFriendlyBarracks);
		rc.broadcast(NUM_FRIENDLY_HELIPAD_CHANNEL, numFriendlyHelipad);
		rc.broadcast(NUM_FRIENDLY_TRAININGFIELD_CHANNEL,
				numFriendlyTrainingField);
		rc.broadcast(NUM_FRIENDLY_TANKFACTORY_CHANNEL, numFriendlyTankFactory);
		rc.broadcast(NUM_FRIENDLY_AEROSPACELAB_CHANNEL, numFriendlyAerospaceLab);
		rc.broadcast(NUM_FRIENDLY_HANDWASHSTATION_CHANNEL,
				numFriendlyHandwashStation);
		// Friendly Units Broadcasts
		rc.broadcast(NUM_FRIENDLY_BEAVERS_CHANNEL, numFriendlyBeavers);
		rc.broadcast(NUM_FRIENDLY_MINERS_CHANNEL, numFriendlyMiners);
		rc.broadcast(NUM_FRIENDLY_COMPUTERS_CHANNEL, numFriendlyComputers);
		rc.broadcast(NUM_FRIENDLY_SOLDIERS_CHANNEL, numFriendlySoldiers);
		rc.broadcast(NUM_FRIENDLY_BASHERS_CHANNEL, numFriendlyBashers);
		rc.broadcast(NUM_FRIENDLY_DRONES_CHANNEL, numFriendlyDrones);
		rc.broadcast(NUM_FRIENDLY_TANKS_CHANNEL, numFriendlyTanks);
		rc.broadcast(NUM_FRIENDLY_COMMANDERS_CHANNEL, numFriendlyCommanders);
		rc.broadcast(NUM_FRIENDLY_LAUNCHERS_CHANNEL, numFriendlyLaunchers);
		rc.broadcast(NUM_FRIENDLY_MISSILES_CHANNEL, numFriendlyMissiles);

		/**
		 * Enemy Robots
		 */
		for (RobotInfo r : enemyRobots) {
			switch (r.type) {
			case AEROSPACELAB:
				++numEnemyAerospaceLab;
				break;
			case BARRACKS:
				++numEnemyBarracks;
				break;
			case BASHER:
				++numEnemyBashers;
				break;
			case BEAVER:
				++numEnemyBeavers;
				break;
			case COMMANDER:
				++numEnemyCommanders;
				break;
			case COMPUTER:
				++numEnemyComputers;
				break;
			case DRONE:
				++numEnemyDrones;
				break;
			case HANDWASHSTATION:
				++numEnemyHandwashStation;
				break;
			case HELIPAD:
				++numEnemyHelipad;
				break;
			case HQ:
				// No need to count HQ!
				break;
			case LAUNCHER:
				++numEnemyLaunchers;
				break;
			case MINER:
				++numEnemyMiners;
				break;
			case MINERFACTORY:
				++numEnemyMinerFactory;
				break;
			case MISSILE:
				++numEnemyMissiles;
				break;
			case SOLDIER:
				++numEnemySoldiers;
				break;
			case SUPPLYDEPOT:
				++numEnemySupplyDepot;
				break;
			case TANK:
				++numEnemyTanks;
				break;
			case TANKFACTORY:
				++numEnemyTankFactory;
				break;
			case TECHNOLOGYINSTITUTE:
				++numEnemyTechInstitute;
				break;
			case TOWER:
				break;
			case TRAININGFIELD:
				++numEnemyTrainingField;
				break;
			default:
				break;
			
			}
		}

		// Enemy Buildings Broadcasts
		rc.broadcast(NUM_ENEMY_SUPPLYDEPOT_CHANNEL, numEnemySupplyDepot);
		rc.broadcast(NUM_ENEMY_MINERFACTORY_CHANNEL, numEnemyMinerFactory);
		rc.broadcast(NUM_ENEMY_TECHINSTITUTE_CHANNEL, numEnemyTechInstitute);
		rc.broadcast(NUM_ENEMY_BARRACKS_CHANNEL, numEnemyBarracks);
		rc.broadcast(NUM_ENEMY_HELIPAD_CHANNEL, numEnemyHelipad);
		rc.broadcast(NUM_ENEMY_TRAININGFIELD_CHANNEL, numEnemyTrainingField);
		rc.broadcast(NUM_ENEMY_TANKFACTORY_CHANNEL, numEnemyTankFactory);
		rc.broadcast(NUM_ENEMY_AEROSPACELAB_CHANNEL, numEnemyAerospaceLab);
		rc.broadcast(NUM_ENEMY_HANDWASHSTATION_CHANNEL, numEnemyHandwashStation);
		// Enemy Units Broadcasts
		rc.broadcast(NUM_ENEMY_BEAVERS_CHANNEL, numEnemyBeavers);
		rc.broadcast(NUM_ENEMY_MINERS_CHANNEL, numEnemyMiners);
		rc.broadcast(NUM_ENEMY_COMPUTERS_CHANNEL, numEnemyComputers);
		rc.broadcast(NUM_ENEMY_SOLDIERS_CHANNEL, numEnemySoldiers);
		rc.broadcast(NUM_ENEMY_BASHERS_CHANNEL, numEnemyBashers);
		rc.broadcast(NUM_ENEMY_DRONES_CHANNEL, numEnemyDrones);
		rc.broadcast(NUM_ENEMY_TANKS_CHANNEL, numEnemyTanks);
		rc.broadcast(NUM_ENEMY_COMMANDERS_CHANNEL, numEnemyCommanders);
		rc.broadcast(NUM_ENEMY_LAUNCHERS_CHANNEL, numEnemyLaunchers);
		rc.broadcast(NUM_ENEMY_MISSILES_CHANNEL, numEnemyMissiles);
	}
}