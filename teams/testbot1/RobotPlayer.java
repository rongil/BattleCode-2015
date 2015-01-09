// Offense Idea: Swarm

package testbot1;

import java.util.HashMap;
import java.util.Random;

import battlecode.common.*;

/**
 * Keeping everything static to save the bytecode from the this statement
 * instances produce!
 */
public class RobotPlayer {

	/**************************************************************************
	 * BROADCAST_CHANNELS
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
	 * ------------------------------------------------------------------------
	 * 100-999: Coordinate Planning + Rallying Points
	 * ------------------------------------------------------------------------
	 * 1000-1999: Offense/Defense Signals (e.g. Swarm)
	 * ------------------------------------------------------------------------
	 * 2000-2999: Priority Mappings
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
	private static final int GENERAL_SWARM_LOCATION_X_CHANNEL = 200;
	private static final int GENERAL_SWARM_LOCATION_Y_CHANNEL = 210;
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
	private static final int GENERAL_SWARM_CHANNEL = 1000;
	private static final int BEAVER_SWARM_CHANNEL = 1001;
	private static final int MINER_SWARM_CHANNEL = 1002;
	private static final int COMPUTER_SWARM_CHANNEL = 1003;
	private static final int SOLDIER_SWARM_CHANNEL = 1004;
	private static final int BASHER_SWARM_CHANNEL = 1005;
	private static final int DRONE_SWARM_CHANNEL = 1006;
	private static final int TANK_SWARM_CHANNEL = 1007;
	private static final int COMMANDER_SWARM_CHANNEL = 1008;
	private static final int MISSILE_SWARM_CHANNEL = 1009;

	// ------------------------------------------------------------------------
	// Action Constants
	// ------------------------------------------------------------------------
	private static final int NO_ACTION = 0;
	private static final int GO_TO_LOCATION = 1;

	// ^^^^^^^^^^^^^^^^^^ BROADCAST CONSTANTS END HERE ^^^^^^^^^^^^^^^^^^^^^^^^

	private static Direction facing;
	private static Direction[] directions = { Direction.NORTH,
			Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
			Direction.NORTH_WEST };
	private static Random rand; /*
								 * This will help to distinguish each robot
								 * otherwise, each robot will behave exactly the
								 * same.

								 */
	
	private static Team Friend;
	private static Team Enemy;
	private static RobotController rc;

	private static HashMap<RobotType, Integer> attackPriorityMap;

	private static int roundNum;
	private static RobotInfo[] friendlyRobots;
	private static RobotInfo[] enemyRobots;

	private static MapLocation myHQ;
	private static MapLocation enemyHQ;

	public static void run(RobotController myRC) {

		rc = myRC;
		Friend = rc.getTeam();
		Enemy = Friend.opponent();
		myHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();

		boolean skipFirstRound = true;

		skipFirstRound = initializeAttackPriorityMap();

		rand = new Random(rc.getID());
		facing = getRandomDirection(); // Randomize starting direction

		// Warning: If the run method ends, the robot dies!
		while (true) {
			try {
				// Avoid too much computation if initializing anything.
				if (skipFirstRound) {
					skipFirstRound = false;
					rc.yield();
				}

				// Random number from 0 to 1 for probabilistic decisions
				double fate = rand.nextDouble();
				// Get the round number
				roundNum = Clock.getRoundNum();
				// Get robot type
				RobotType type = rc.getType();
				// General robot swarm variable
				int allRobotSwarm;

				switch (type) {
				case HQ:
					/**********************************************************
					 * Update unit counts every so often! Distributes the
					 * counting over a few consecutive rounds.
					 *********************************************************/
					if (roundNum % 5 < 5) { // NOTE: This can be adjusted to
											// give slots to other computations,
											// such as optimal path finding.
						updateUnitCounts();
					}

					/**********************************************************
					 * Robot Army Coordination Signals
					 *********************************************************/

					// Start Offensive Swarm Conditions | Friendly(>=) Enemy(<)
					// ========================================================
					// Total Robot Difference ----------------------------> 200
					// Round 1800+ AND Number of Friendly Soldiers -------> 30
					// ________________OR Number of Friendly Bashers -----> 30
					// ________________OR Number of Friendly Miners ------> 30
					// ________________OR Number of Friendly Tanks -------> 15

					int currentNumFriendlyBashers = rc
							.readBroadcast(NUM_FRIENDLY_BASHERS_CHANNEL);
					int currentNumFriendlySoldiers = rc
							.readBroadcast(NUM_FRIENDLY_SOLDIERS_CHANNEL);
					int currentNumFriendlyTanks = rc
							.readBroadcast(NUM_FRIENDLY_TANKS_CHANNEL);
					int currentNumFriendlyDrones = rc
							.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL);
					int currentNumFriendlyMissiles = rc
							.readBroadcast(NUM_FRIENDLY_MISSILES_CHANNEL);
					int currentNumFriendlyMiners = rc
							.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL);
					MapLocation[] towers = rc.senseEnemyTowerLocations();
					if (friendlyRobots.length - enemyRobots.length >= 200
							|| (roundNum > 1800 && (currentNumFriendlySoldiers > 30
									|| currentNumFriendlyBashers > 30
									|| currentNumFriendlyTanks > 15 || currentNumFriendlyMiners > 30))) {
						towers = rc.senseEnemyTowerLocations();
						MapLocation location = enemyHQ; // Default
						if (towers.length > 2) { // Leave at most 2 towers
							location = towers[0];
						}

						RobotType[] types = { RobotType.BASHER,
								RobotType.COMMANDER, RobotType.DRONE,
								RobotType.MINER, RobotType.MISSILE,
								RobotType.SOLDIER, RobotType.TANK };
						broadcastSwarmInfo(types, GO_TO_LOCATION, location);

						// Stop Offensive Swarm Conditions (<):
						// =====================================================
						// Total Robot Difference -------------------------> -30
						// Number of Friendly Soldiers --------------------> 5
						// ___AND Number of Friendly Miners ---------------> 15
						// ___AND Number of Friendly Tanks Remaining ------> 1
						//
					} else if (rc.readBroadcast(NUM_FRIENDLY_SOLDIERS_CHANNEL) < 5
							&& rc.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL) < 15
							&& rc.readBroadcast(NUM_FRIENDLY_TANKS_CHANNEL) < 1) {
						RobotType[] types = { RobotType.BASHER,
								RobotType.COMMANDER, RobotType.DRONE,
								RobotType.MINER, RobotType.MISSILE,
								RobotType.SOLDIER, RobotType.TANK };
						broadcastSwarmInfo(types, NO_ACTION, null);

					}

					/*
					 * We call this method before spawning beavers because we
					 * probably will want to spawn more efficient creatures such
					 * as miners; in addition, spawning introduces attacking
					 * delay.
					 */
					attackEnemyZero();

					// Limited beaver spawn
					if (rc.readBroadcast(NUM_FRIENDLY_BEAVERS_CHANNEL) < 10) {
						spawnUnit(RobotType.BEAVER);
					}
				case AEROSPACELAB:
					spawnUnit(RobotType.LAUNCHER);
					break;
				case BARRACKS:
					if (fate < .9) {
						spawnUnit(RobotType.SOLDIER);
					} else {
						spawnUnit(RobotType.BASHER);
					}
					break;
				case BASHER:
					attackEnemyZero();
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int basherSwarm = rc.readBroadcast(BASHER_SWARM_CHANNEL);
					if (basherSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(BASHER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(BASHER_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						// BASHERs attack automatically, so let's just move
						// around mostly randomly.
						// moveAround();
						flyOnBoundary();
					}
					break;
				case BEAVER:
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int beaverSwarm = rc.readBroadcast(BEAVER_SWARM_CHANNEL);
					attackEnemyZero();
					if (beaverSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(BEAVER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(BEAVER_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					}

					/**********************************************************
					 * Probability List
					 * --------------------------------------------------------
					 * P(TRAININGFIELD) = 0.025
					 * --------------------------------------------------------
					 * P(TECHNOLOGYINSTITUTE) = 0.025
					 * --------------------------------------------------------
					 * P(HANDWASHSTATION) = 0.05
					 * --------------------------------------------------------
					 * P(MINERFACTORY) = Non-probabilistic
					 * --------------------------------------------------------
					 * P(SUPPLYDEPOT) = 0.15
					 * --------------------------------------------------------
					 * P(TANKFACTORY) = 0.3
					 * --------------------------------------------------------
					 * P(HELIPAD) = 0.15
					 * --------------------------------------------------------
					 * P(AEROSPACELAB) = 0.15
					 * -------------------------------------------------------
					 * P(BARRACKS) = 0.2
					 *********************************************************/

					// Limit the number of miner factories
					if(measureCrowdedNess(rc.getLocation(), 4) < 10){
						if (roundNum < 500
								&& rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 3) {
							buildUnit(RobotType.MINERFACTORY);
						} else if (roundNum < 600) {
							if (rc.readBroadcast(NUM_FRIENDLY_BARRACKS_CHANNEL) < 1) {
								buildUnit(RobotType.BARRACKS);
							} else {
								buildUnit(RobotType.TANKFACTORY);
							}
						} else if (rc
								.readBroadcast(NUM_FRIENDLY_TECHINSTITUTE_CHANNEL) < 1) {
							boolean success = buildUnit(RobotType.TECHNOLOGYINSTITUTE);
							if (success) {
								rc.broadcast(NUM_FRIENDLY_TRAININGFIELD_CHANNEL, 1);
							}
						} else if (rc
								.readBroadcast(NUM_FRIENDLY_TRAININGFIELD_CHANNEL) < 1) {
							boolean success = buildUnit(RobotType.TRAININGFIELD);
							if (success) {
								rc.broadcast(NUM_FRIENDLY_TRAININGFIELD_CHANNEL, 1);
							}
						} else if (0.05 <= fate && fate < 0.06) {
							buildUnit(RobotType.HANDWASHSTATION);
						} else if (0.1 <= fate && fate < 0.2) {
							if (rc.readBroadcast(NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL) < 10) {
								buildUnit(RobotType.SUPPLYDEPOT);
							}
						} else if (roundNum < 1500
								&& rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 3) {
							buildUnit(RobotType.MINERFACTORY);
						} else if (0.2 <= fate && fate < 0.45) {
							if (rc.readBroadcast(NUM_FRIENDLY_TANKFACTORY_CHANNEL) < 3) {
								buildUnit(RobotType.TANKFACTORY);
							}
						} else if (0.45 <= fate && fate < 0.55) {
							buildUnit(RobotType.HELIPAD);
						} else if (0.65 <= fate && fate < 0.8) {
							buildUnit(RobotType.AEROSPACELAB);
						} else if (0.8 <= fate
								|| rc.readBroadcast(NUM_FRIENDLY_BARRACKS_CHANNEL) < 5) {
							buildUnit(RobotType.BARRACKS);
						}
					}
				
					mineAndMove();

					break;
				case COMMANDER:
					attackEnemyZero();
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int commanderSwarm = rc
							.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (commanderSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(COMMANDER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(COMMANDER_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						// moveAround();
						flyOnBoundary();
					}
					break;
				case COMPUTER:
					// allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					// int computerSwarm = rc
					// .readBroadcast(COMMANDER_SWARM_CHANNEL);
					// if (computerSwarm == GO_TO_LOCATION) {
					// int x = rc
					// .readBroadcast(COMPUTER_SWARM_LOCATION_X_CHANNEL);
					// int y = rc
					// .readBroadcast(COMPUTER_SWARM_LOCATION_Y_CHANNEL);
					// tryMove(rc.getLocation().directionTo(
					// new MapLocation(x, y)));
					// } else if (allRobotSwarm == GO_TO_LOCATION) {
					// int x = rc
					// .readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
					// int y = rc
					// .readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
					// tryMove(rc.getLocation().directionTo(
					// new MapLocation(x, y)));
					//
					// } else {
					// moveAround();
					// }
					break;
				case DRONE:
					attackEnemyZero();
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int droneSwarm = rc.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (droneSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(DRONE_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(DRONE_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						// moveAround();
						MapLocation[] towerLocations = rc.senseTowerLocations();
						if (rc.isCoreReady()) {
							if (rand.nextDouble() > 0.1
									|| towerLocations.length == 0) {
								flyOnBoundary();
							} else {
								int towerNumber = rand
										.nextInt(towerLocations.length);
								Direction towerDirection = bugNav(towerLocations[towerNumber]);

								if (towerDirection != Direction.NONE) {
									rc.move(bugNav(towerLocations[towerNumber]));
								}
							}
						}
					}
					break;
				case HANDWASHSTATION:
					// Wash hands.
					break;
				case HELIPAD:
					spawnUnit(RobotType.DRONE);
					break;
				case LAUNCHER:
					// TODO: Fix missile launching
					rc.launchMissile(getRandomDirection());
					break;
				case MINER:
					attackEnemyZero();
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int minerSwarm = rc.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (minerSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(MINER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(MINER_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						mineAndMove();
					}
					break;
				case MINERFACTORY:
					int minerCount = rc
							.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL);
					double miningFate = rand.nextDouble();
					if (roundNum < 1500
							&& miningFate <= Math.pow(Math.E,
									-minerCount * 0.07)) {
						spawnUnit(RobotType.MINER);
					} else if (minerCount < 10) {
						spawnUnit(RobotType.MINER);
					}

					break;
				case MISSILE:
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int missileSwarm = rc
							.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (missileSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(MISSILE_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(MISSILE_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						rc.explode();
					}
					break;
				case SOLDIER:
					attackEnemyZero(); // soldiers attack, not mine
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int soldierSwarm = rc
							.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (soldierSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(SOLDIER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(SOLDIER_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						/*
						 * moveAround(); POSSIBLE OPTIMIZATION: chase enemies In
						 * addition, soldiers need to attack towers eventually,
						 * so they will have to move within attacking range of
						 * the towers, which is not possible under moveAround()
						 */
						// moveAround();
						flyOnBoundary();
					}
					break;
				case SUPPLYDEPOT:
					break;
				case TANK:
					attackEnemyZero();
					allRobotSwarm = rc.readBroadcast(GENERAL_SWARM_CHANNEL);
					int tankSwarm = rc.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (tankSwarm == GO_TO_LOCATION) {
						int x = rc.readBroadcast(TANK_SWARM_LOCATION_X_CHANNEL);
						int y = rc.readBroadcast(TANK_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));
					} else if (allRobotSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(GENERAL_SWARM_LOCATION_Y_CHANNEL);
						tryMove(rc.getLocation().directionTo(
								new MapLocation(x, y)));

					} else {
						// moveAround();
						flyOnBoundary();
					}
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
					// Limit the number of commanders that will be produced in
					// the game (assuming any die) since their cost increases.
					int numCommanders = rc
							.readBroadcast(NUM_FRIENDLY_COMMANDERS_CHANNEL);
					if (!rc.hasCommander() && numCommanders < 3) {
						spawnUnit(RobotType.COMMANDER);
						rc.broadcast(NUM_FRIENDLY_COMMANDERS_CHANNEL,
								numCommanders + 1);
					}
					break;
				default:
					break;
				}

				// Missiles have 500 bytecode limit (the cost of JUST calling
				// transfer supplies).
				if (type != RobotType.MISSILE) {
					/*
					 * If robots go low on supplies, they will become less
					 * effective in attacking; HQ supplies goods at a constant
					 * rate + any additional units from having supply depots
					 * built; these units are to be passed from HQ among the
					 * robots in a way such that all robots are sufficiently
					 * supplied.
					 * 
					 * NOTE: Robots that are low on supplies will have a white
					 * square around them.
					 */

					transferSupplies();
				}

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

	private static int measureCrowdedNess(MapLocation loc, int radiusSquared) {
	    // TODO: make more sophisticated
	    int numBadTiles = 0;
	    
	    if (radiusSquared <= 9) {
	        for (MapLocation location : loc.getAllMapLocationsWithinRadiusSq(loc, radiusSquared)) {
	            if (rc.senseTerrainTile(location) != TerrainTile.NORMAL) {
	                ++numBadTiles;
	            }
	        }
	    }
	    return rc.senseNearbyRobots(loc, radiusSquared, Friend).length + numBadTiles;
	}
	
	private static void flyOnBoundary() throws GameActionException {
		defendHQ();
		moveAround();
	}

	private static void defendHQ() throws GameActionException {
		if (rc.isCoreReady()) {
			// int currentRadiusSquared = rc.readBroadcast(HQ_RADIUS_CHANNEL);
			// MapLocation currentLocation = rc.getLocation();
			// double squaredDistanceDiff =
			// Math.abs(currentLocation.distanceSquaredTo(myHQ)
			// - currentRadiusSquared);
			// if (squaredDistanceDiff > 36.0 * RobotType.HQ.attackRadiusSquared
			// || squaredDistanceDiff < 25.0 * RobotType.HQ.attackRadiusSquared)
			// {
			// Direction rightDirection = myHQ.directionTo(currentLocation);
			// MapLocation targetLocation = myHQ.add(rightDirection, 1);
			//
			// Direction targetDirection = bugNav(targetLocation);
			// if(targetDirection != Direction.NONE){
			// rc.move(targetDirection);
			// }
			// }

			MapLocation currentLocation = rc.getLocation();
			if (currentLocation.distanceSquaredTo(myHQ) > 2 * currentLocation
					.distanceSquaredTo(enemyHQ)) {
				// double turnLittle = rand.nextDouble();
				// Direction currentDirection =
				// currentLocation.directionTo(myHQ);
				//
				// if(turnLittle < 0.33){
				// currentDirection = currentDirection.rotateLeft();
				// }else if(turnLittle > 0.33 & turnLittle < 0.66){
				// currentDirection = currentDirection.rotateRight();
				// }
				//
				// MapLocation targetLocation =
				// currentLocation.add(currentDirection);
				// Direction targetDirection = bugNav(targetLocation);

				Direction targetDirection = bugNav(myHQ);
				if (targetDirection != Direction.NONE) {
					rc.move(targetDirection);
				}
			} else if (currentLocation.distanceSquaredTo(enemyHQ) > 1.1 * currentLocation
					.distanceSquaredTo(myHQ)) {
				// double turnLittle = rand.nextDouble();
				// Direction currentDirection =
				// currentLocation.directionTo(enemyHQ);
				//
				// if(turnLittle < 0.33){
				// currentDirection = currentDirection.rotateLeft();
				// }else if(turnLittle > 0.33 & turnLittle < 0.66){
				// currentDirection = currentDirection.rotateRight();
				// }
				//
				// MapLocation targetLocation =
				// currentLocation.add(currentDirection);
				// Direction targetDirection = bugNav(targetLocation);

				Direction targetDirection = bugNav(enemyHQ);
				if (targetDirection != Direction.NONE) {
					rc.move(targetDirection);
				}
			} else {
				double turnVar = rand.nextDouble();
				MapLocation newLocation;
				Direction newDirection;
				if (turnVar < 0.5) {
					newDirection = currentLocation.directionTo(myHQ)
							.rotateRight().rotateRight();
					newLocation = currentLocation.add(newDirection);
				} else {
					newDirection = currentLocation.directionTo(myHQ)
							.rotateLeft().rotateLeft();
					newLocation = currentLocation.add(newDirection);
				}

				Direction targetDirection = bugNav(newLocation);
				if (targetDirection != Direction.NONE) {
					rc.move(targetDirection);
				}
			}
		}
	}

	private static void broadcastRadiusSquared() throws GameActionException {
		int radiusSquared;
		MapLocation[] towerLocations = rc.senseTowerLocations();

		if (towerLocations.length != 0) {
			double maxTowerDistanceSquared = 0;

			for (MapLocation towerLocation : towerLocations) {
				double TowerHQdistSquared = myHQ
						.distanceSquaredTo(towerLocation);
				maxTowerDistanceSquared = (TowerHQdistSquared > maxTowerDistanceSquared) ? TowerHQdistSquared
						: maxTowerDistanceSquared;
			}

			radiusSquared = (int) maxTowerDistanceSquared;
		} else {
			double soldierCount = (double) rc
					.readBroadcast(NUM_FRIENDLY_SOLDIERS_CHANNEL);
			radiusSquared = (int) Math.pow(soldierCount / (2.0 * Math.PI), 2);
		}

		rc.broadcast(HQ_RADIUS_CHANNEL, radiusSquared);
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

	private static Direction bugNav(MapLocation target)
			throws GameActionException {
		// TODO: Make it possible for robots to enter non-safe squares when they
		// have strength in numbers

		Direction straight = rc.getLocation().directionTo(target);
		MapLocation currentLoc = rc.getLocation();

		Direction[] possDirs = new Direction[8];
		possDirs[0] = straight;
		possDirs[7] = straight.opposite();

		double orderOne = rand.nextDouble();
		double orderTwo = rand.nextDouble();
		double orderThree = rand.nextDouble();

		possDirs[1] = (orderOne > 0.5) ? straight.rotateLeft() : straight
				.rotateRight();
		possDirs[2] = (orderOne > 0.5) ? straight.rotateRight() : straight
				.rotateLeft();

		possDirs[3] = (orderTwo > 0.5) ? straight.rotateLeft().rotateLeft()
				: straight.rotateRight().rotateRight();
		possDirs[4] = possDirs[3].opposite();

		possDirs[5] = (orderThree > 0.5) ? possDirs[7].rotateLeft()
				: possDirs[7].rotateRight();
		possDirs[6] = (orderThree > 0.5) ? possDirs[7].rotateRight()
				: possDirs[7].rotateLeft();

		for (Direction bestDirection : possDirs) {
			MapLocation possSquare = currentLoc.add(bestDirection);
			if (isSafe(possSquare) && rc.canMove(bestDirection)) {
				return bestDirection;
			}
		}

		return Direction.NONE;
	}

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
				Direction bestDirection = bugNav(bestDestination);

				if (bestDirection != Direction.NONE) {
					rc.move(bestDirection);
				}
			} else {
				// moveAround();
				// flyOnBoundary();
				defendHQ();
			}
		}
	}

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
			locateBestOre();
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

	// private static void attackBestEnemy() throws GameActionException {
	// // Don't do anything if the weapon is not ready.
	// if (!rc.isWeaponReady()) {
	// return;
	// }
	//
	// RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
	// rc.getType().attackRadiusSquared, rc.getTeam().opponent());
	// RobotInfo target = null;
	// int currentTargetPriority;
	// int enemyPriority;
	// for (RobotInfo enemy : nearbyEnemies) {
	// if (Clock.getBytecodeNum() > 2000) {
	// break;
	// }
	// if (rc.canAttackLocation(enemy.location)) {
	// if (target != null) {
	// currentTargetPriority = attackPriorityMap.get(target.type);
	// enemyPriority = attackPriorityMap.get(enemy.type);
	// } else {
	// target = enemy;
	// continue;
	// }
	//
	// // TODO: Add more detail in choosing an enemy to attack.
	// if (enemyPriority < currentTargetPriority) {
	// target = enemy;
	// }
	// }
	//
	// }
	// if (target != null) {
	// rc.attackLocation(target.location);
	// }
	// }

	@SuppressWarnings("fallthrough")
	private static boolean initializeAttackPriorityMap() {

		boolean skipFirstRound = false;

		// WARNING: INTENTIONAL FALLTHROUGH IS USED!
		// ---------------------------------------------------
		// Attack Priority Map (only for attacking structures)
		// Lower Number -> Higher Priority
		switch (rc.getType()) {
		case BASHER:
		case BEAVER:
		case COMMANDER:
		case DRONE:
		case HQ:
		case MINER:
		case SOLDIER:
		case TANK:
		case TOWER:
			attackPriorityMap = new HashMap<RobotType, Integer>();
			attackPriorityMap.put(RobotType.AEROSPACELAB, 6);
			attackPriorityMap.put(RobotType.BARRACKS, 4); // Troop production
			attackPriorityMap.put(RobotType.BASHER, 5);
			attackPriorityMap.put(RobotType.BEAVER, 2); // Buildings
			attackPriorityMap.put(RobotType.COMMANDER, 5);
			attackPriorityMap.put(RobotType.COMPUTER, 0); // One HP
			attackPriorityMap.put(RobotType.DRONE, 5);
			attackPriorityMap.put(RobotType.HANDWASHSTATION, 7);
			attackPriorityMap.put(RobotType.HELIPAD, 5);
			attackPriorityMap.put(RobotType.HQ, 0); // Main target
			attackPriorityMap.put(RobotType.LAUNCHER, 5);
			attackPriorityMap.put(RobotType.MINER, 3);
			attackPriorityMap.put(RobotType.MINERFACTORY, 3); // Ore collection
			attackPriorityMap.put(RobotType.MISSILE, 5);
			attackPriorityMap.put(RobotType.SOLDIER, 5);
			attackPriorityMap.put(RobotType.SUPPLYDEPOT, 6);
			attackPriorityMap.put(RobotType.TANK, 5);
			attackPriorityMap.put(RobotType.TANKFACTORY, 5);
			attackPriorityMap.put(RobotType.TECHNOLOGYINSTITUTE, 7);
			attackPriorityMap.put(RobotType.TOWER, 1); // Weaken HQ
			attackPriorityMap.put(RobotType.TRAININGFIELD, 6);
			skipFirstRound = true;
			break;
		default:
			break;

		}

		return skipFirstRound;
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

	private static boolean buildUnit(RobotType roboType)
			throws GameActionException {
		if (rc.getTeamOre() > roboType.oreCost) {
			Direction buildDir = getRandomDirection();

			if (rc.isCoreReady() && rc.canBuild(buildDir, roboType)) {
				rc.build(buildDir, roboType);
				return true;
			}
		}

		return false;
	}

	// This method will attempt to move in Direction d (or as close to it as
	// possible)
	private static void tryMove(Direction d) throws GameActionException {
		if (!rc.isCoreReady()) {
			return;
		}
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
	 * NOTE: These variables are kept down here since they are solely used when
	 * updating quantity values. They need to be maintained outside the method
	 * since they are used across multiple rounds.
	 *************************************************************************/
	// Friendly Buildings
	private static int numFriendlySupplyDepot;
	private static int numFriendlyMinerFactory;
	private static int numFriendlyTechInstitute;
	private static int numFriendlyBarracks;
	private static int numFriendlyHelipad;
	private static int numFriendlyTrainingField;
	private static int numFriendlyTankFactory;
	private static int numFriendlyAerospaceLab;
	private static int numFriendlyHandwashStation;
	// Friendly other units
	private static int numFriendlyBeavers;
	private static int numFriendlyMiners;
	private static int numFriendlyComputers;
	private static int numFriendlySoldiers;
	private static int numFriendlyBashers;
	private static int numFriendlyDrones;
	private static int numFriendlyTanks;
	private static int numFriendlyLaunchers;
	private static int numFriendlyMissiles;
	// Enemy Buildings
	private static int numEnemySupplyDepot;
	private static int numEnemyMinerFactory;
	private static int numEnemyTechInstitute;
	private static int numEnemyBarracks;
	private static int numEnemyHelipad;
	private static int numEnemyTrainingField;
	private static int numEnemyTankFactory;
	private static int numEnemyAerospaceLab;
	private static int numEnemyHandwashStation;
	// Enemy other units
	private static int numEnemyBeavers;
	private static int numEnemyMiners;
	private static int numEnemyComputers;
	private static int numEnemySoldiers;
	private static int numEnemyBashers;
	private static int numEnemyDrones;
	private static int numEnemyTanks;
	private static int numEnemyCommanders;
	private static int numEnemyLaunchers;
	private static int numEnemyMissiles;

	@SuppressWarnings("fallthrough")
	private static void updateUnitCounts() throws GameActionException {

		// Run part of the work on each round
		int roundNumMod = roundNum % 20;
		if (roundNumMod == 0 || friendlyRobots == null || enemyRobots == null) {
			// Collect all robots into separate RobotInfo arrays.
			friendlyRobots = rc.senseNearbyRobots(999999, rc.getTeam());
			enemyRobots = rc.senseNearbyRobots(999999, rc.getTeam().opponent());
		}
		int friendlyChunkSize = (int) Math.floor(friendlyRobots.length / 4);
		int enemyChunkSize = (int) Math.floor(enemyRobots.length / 4);
		int friendlyLoopStart = friendlyChunkSize * roundNumMod;
		int friendlyLoopEnd = friendlyChunkSize * (roundNumMod + 1);
		int enemyLoopStart = enemyChunkSize * roundNumMod;
		int enemyLoopEnd = enemyChunkSize * (roundNumMod + 1);

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
			numFriendlySupplyDepot = 0;
			numFriendlyMinerFactory = 0;
			numFriendlyTechInstitute = 0;
			numFriendlyBarracks = 0;
			numFriendlyHelipad = 0;
			numFriendlyTrainingField = 0;
			numFriendlyTankFactory = 0;
			numFriendlyAerospaceLab = 0;
			numFriendlyHandwashStation = 0;
			// Friendly other units
			numFriendlyBeavers = 0;
			numFriendlyMiners = 0;
			numFriendlyComputers = 0;
			numFriendlySoldiers = 0;
			numFriendlyBashers = 0;
			numFriendlyDrones = 0;
			numFriendlyTanks = 0;
			numFriendlyLaunchers = 0;
			numFriendlyMissiles = 0;
			// Enemy Buildings
			numEnemySupplyDepot = 0;
			numEnemyMinerFactory = 0;
			numEnemyTechInstitute = 0;
			numEnemyBarracks = 0;
			numEnemyHelipad = 0;
			numEnemyTrainingField = 0;
			numEnemyTankFactory = 0;
			numEnemyAerospaceLab = 0;
			numEnemyHandwashStation = 0;
			// Enemy other units
			numEnemyBeavers = 0;
			numEnemyMiners = 0;
			numEnemyComputers = 0;
			numEnemySoldiers = 0;
			numEnemyBashers = 0;
			numEnemyDrones = 0;
			numEnemyTanks = 0;
			numEnemyCommanders = 0;
			numEnemyLaunchers = 0;
			numEnemyMissiles = 0;
		case 1:
		case 2:
		case 3:
			// Friendly Robot Loop
			for (int i = friendlyLoopStart; i < friendlyLoopEnd; ++i) {
				switch (friendlyRobots[i].type) {
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

			// Enemy Robot Loop
			for (int j = enemyLoopStart; j < enemyLoopEnd; ++j) {
				switch (enemyRobots[j].type) {
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
				case HQ: // No need to count HQ! break; case LAUNCHER:
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
			break;
		case 4:
			// Friendly Buildings Broadcasts
			rc.broadcast(NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL,
					numFriendlySupplyDepot);
			rc.broadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL,
					numFriendlyMinerFactory);
			rc.broadcast(NUM_FRIENDLY_TECHINSTITUTE_CHANNEL,
					numFriendlyTechInstitute);
			rc.broadcast(NUM_FRIENDLY_BARRACKS_CHANNEL, numFriendlyBarracks);
			rc.broadcast(NUM_FRIENDLY_HELIPAD_CHANNEL, numFriendlyHelipad);
			rc.broadcast(NUM_FRIENDLY_TRAININGFIELD_CHANNEL,
					numFriendlyTrainingField);
			rc.broadcast(NUM_FRIENDLY_TANKFACTORY_CHANNEL,
					numFriendlyTankFactory);
			rc.broadcast(NUM_FRIENDLY_AEROSPACELAB_CHANNEL,
					numFriendlyAerospaceLab);
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
			rc.broadcast(NUM_FRIENDLY_LAUNCHERS_CHANNEL, numFriendlyLaunchers);
			rc.broadcast(NUM_FRIENDLY_MISSILES_CHANNEL, numFriendlyMissiles);
			// Enemy Buildings Broadcasts
			rc.broadcast(NUM_ENEMY_SUPPLYDEPOT_CHANNEL, numEnemySupplyDepot);
			rc.broadcast(NUM_ENEMY_MINERFACTORY_CHANNEL, numEnemyMinerFactory);
			rc.broadcast(NUM_ENEMY_TECHINSTITUTE_CHANNEL, numEnemyTechInstitute);
			rc.broadcast(NUM_ENEMY_BARRACKS_CHANNEL, numEnemyBarracks);
			rc.broadcast(NUM_ENEMY_HELIPAD_CHANNEL, numEnemyHelipad);
			rc.broadcast(NUM_ENEMY_TRAININGFIELD_CHANNEL, numEnemyTrainingField);
			rc.broadcast(NUM_ENEMY_TANKFACTORY_CHANNEL, numEnemyTankFactory);
			rc.broadcast(NUM_ENEMY_AEROSPACELAB_CHANNEL, numEnemyAerospaceLab);
			rc.broadcast(NUM_ENEMY_HANDWASHSTATION_CHANNEL,
					numEnemyHandwashStation);
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
			break;
		}

	}
}