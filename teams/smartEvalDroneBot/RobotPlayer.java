package smartEvalDroneBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import battlecode.common.*;

/**
 * Keeping everything static to save the bytecode from the this statement
 * instances produce!
 */
public class RobotPlayer {

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

	private static MapLocation assignment = null;
	private static MapLocation checkpoint = null;
	private static boolean mobilized = false;
	private static int xmin, xmax, ymin, ymax;
	private static int xpos, ypos;

	private static int totalNormal, totalVoid, totalProcessed;
	private static int towerStrength;

	private static boolean doneAnalyzing = false;
	private static double normRatio;

	/**************************************************************************
	 * For drones only!
	 *************************************************************************/
	private static List<MapLocation> droneShieldLocations;
	private static List<MapLocation> droneAttackCircleLocations;
	private static int currentDroneDirectionIndex = 0;
	private static int droneCircleRound = 0;
	private static boolean droneDefender;
	private static int halfwayDistance;
	private static MapLocation lastLocation = null;
	
	// ************************************************************************

	/**
	 * 
	 * @throws GameActionException
	 */
	private static void mobilize() throws GameActionException {
		if (!mobilized) {
			MapLocation currentLocation = rc.getLocation();

			if (checkpoint == null) {
				checkpoint = (rand.nextDouble() > 0.5) ? new MapLocation(
						-12899, 13140) : new MapLocation(-12928, 13149);
				moveTowardDestination(checkpoint, true, false, false);

			} else if (currentLocation.distanceSquaredTo(checkpoint) < 10) {
				mobilized = true;
				moveAround();
			} else {
				moveTowardDestination(checkpoint, true, false, false);
			}
		} else {
			moveAround();
		}
	}

	/**
	 * 
	 * @param myRC
	 * @throws GameActionException
	 */
	public static void run(RobotController myRC) throws GameActionException {

		rc = myRC;
		Friend = rc.getTeam();
		Enemy = Friend.opponent();
		myHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();

		getMapDimensions();
		xpos = xmin;
		ypos = ymin;

		boolean skipFirstRound = true;

		skipFirstRound = initializeAttackPriorityMap();

		rand = new Random(rc.getID());
		facing = getRandomDirection(); // Randomize starting direction

		// For drones only!
		if (rc.getType() == RobotType.DRONE || rc.getType() == RobotType.TANK) {
			// Slightly less to avoid tower issues
			halfwayDistance = (int) (0.9 * myHQ.distanceSquaredTo(enemyHQ) / 2);
			if (Clock.getRoundNum() < 400) {
				droneDefender = true;
			} else {
				// Can be adjusted if we wanted attacking drones.
				droneDefender = rand.nextDouble() < 1 ? true : false;
			}

			if (assignment == null) {
				// TODO: balance out the attack and defense with probabilities
				MapLocation[] myTowers = rc.senseTowerLocations();
				double targetProb = rand.nextDouble();
				int SensorRadiusSquared;

				double towerLength = (double) myTowers.length;

				// Drones have to return to HQ to get resupplied.
				if (targetProb >= 0) {// towerLength / (towerLength + 1.0)) {
					assignment = myHQ;
					SensorRadiusSquared = RobotType.HQ.sensorRadiusSquared;
				} else {
					int towerIndex = (int) targetProb * (myTowers.length + 1);
					assignment = myTowers[towerIndex];
					SensorRadiusSquared = RobotType.TOWER.sensorRadiusSquared;
				}

				int friendlyMagnitude = (int) (Math.sqrt(SensorRadiusSquared)); // Floored
				int enemyMagnitude = (int) (Math.sqrt(SensorRadiusSquared) + Math.sqrt(2)) + 1; // Floored
				droneShieldLocations = new ArrayList<MapLocation>();
				droneAttackCircleLocations = new ArrayList<MapLocation>();

				Direction dir = directions[rand.nextInt(8)];
				boolean goLeft = rand.nextDouble() > 0.5;

				for (int index = 0; index < 8; index++) {
					droneShieldLocations.add(assignment.add(dir,
							friendlyMagnitude));
					droneAttackCircleLocations.add(enemyHQ.add(dir,
							enemyMagnitude));

					dir = goLeft ? dir.rotateLeft() : dir.rotateRight();

				}
			}

		}

		// Warning: If the run method ends, the robot dies!

		while (true) {
			try {
				// Avoid too much computation if initializing anything.
				if (skipFirstRound) {
					skipFirstRound = false;
					rc.yield();
				}

				// Random number from 0 to 1 for probabilistic decisions
				// TODO: calculate fate based on board terrain and tower factors
				double fate = rand.nextDouble();

				// Get the round number
				roundNum = Clock.getRoundNum();
				// Get robot type
				RobotType type = rc.getType();

				switch (type) {
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

					/**********************************************************
					 * Robot Army Coordination Signals
					 *********************************************************/

					// Start Offensive Swarm Conditions| Friendly(>=) Enemy(<=)
					// ========================================================
					// Total Robot Difference ----------------------------> 300
					// ____AND Tower Strength ----------------------------> 2
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
					int currentNumFriendlyMiners = rc
							.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL);
					int currentNumFriendlyDrones = rc
							.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL);
					MapLocation[] towers = rc.senseEnemyTowerLocations();
					if ((friendlyRobots.length - enemyRobots.length >= 200 && rc
							.readBroadcast(TOWER_STRENGTH_CHANNEL) <= 2)
							|| (roundNum > 1800 && (currentNumFriendlySoldiers > 30
									|| currentNumFriendlyBashers > 30
									|| currentNumFriendlyTanks > 15
									|| currentNumFriendlyMiners > 30 || currentNumFriendlyDrones > 15))) {

						towers = rc.senseEnemyTowerLocations();
						MapLocation location = enemyHQ; // Default to enemy HQ

						if (towers.length > 2) { // Leave at most 2 towers
							int minDistance = towers[0].distanceSquaredTo(myHQ);
							location = towers[0];
							// Bytecode conserving loop format
							for (int i = towers.length; --i > 0;) {
								int distance = towers[i]
										.distanceSquaredTo(myHQ);
								if (distance < minDistance) {
									location = towers[i];
									minDistance = distance;
								}
							}
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
					} else if (roundNum < 1800
							&& rc.readBroadcast(NUM_FRIENDLY_SOLDIERS_CHANNEL) < 5
							&& rc.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL) < 15
							&& rc.readBroadcast(NUM_FRIENDLY_TANKS_CHANNEL) < 1
							&& rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL) < 5) {
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
					int beaverCount = rc
							.readBroadcast(NUM_FRIENDLY_BEAVERS_CHANNEL);
					if (beaverCount < 3) {
						if (beaverCount == 0 || roundNum >= 300) {
							createUnit(RobotType.BEAVER, false);
						}
					}

					if (!doneAnalyzing) {
						analyzeMapMobility();
					}

					break;
				case AEROSPACELAB:
					createUnit(RobotType.LAUNCHER, false);
					break;
				case BARRACKS:
					if (roundNum >= 3000) {
						if (fate < .95) {
							createUnit(RobotType.SOLDIER, false);
						} else {
							createUnit(RobotType.BASHER, false);
						}
					}
					break;
				case BASHER:
					attackEnemyZero();
					int basherSwarm = rc.readBroadcast(BASHER_SWARM_CHANNEL);
					if (basherSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(BASHER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(BASHER_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), true,
								false, false);
					} else {
						// BASHERs attack automatically, so let's just move
						// around mostly randomly.
						moveAround();
						// flyOnBoundary();
						// mobilize();
					}
					break;
				case BEAVER:
					int beaverSwarm = rc.readBroadcast(BEAVER_SWARM_CHANNEL);
					attackEnemyZero();
					if (beaverSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(BEAVER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(BEAVER_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), false,
								false, false);
					}

					/*
					 * Drones are very useful in the beginning due to their
					 * speed and relatively high attacking ability; mobilizing
					 * them quickly will enable quick lock-down on enemies who
					 * are slow to mobilize, leading to an almost assured win on
					 * tie-breaks
					 */

					if (roundNum < 400) {
						 if (rc.readBroadcast(NUM_FRIENDLY_HELIPAD_CHANNEL) < 3) {
								createUnit(RobotType.HELIPAD, true);
						} else if (rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 1) {
							createUnit(RobotType.MINERFACTORY, true);
						}
					} else {
						/*
						 * Miner factories are crucial for the spawning of
						 * miners, which are more efficient at mining ore than
						 * beavers. Ore is needed for the further construction
						 * of other artillery like tanks and launchers for
						 * missiles
						 */

						if (roundNum < 500
								&& rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 3) {
							createUnit(RobotType.MINERFACTORY, true);

							/*
							 * Tanks are also very effective weapons because of
							 * their attacking range and power. However, they
							 * take a lot of work to create, so their
							 * development needs to be started early so as to
							 * amass a sufficient number for later in the game
							 */

						} else if (roundNum < 1000) {
							if (rc.readBroadcast(NUM_FRIENDLY_BARRACKS_CHANNEL) < 1) {
								createUnit(RobotType.BARRACKS, true);
							} else {
								createUnit(RobotType.TANKFACTORY, true);
							}

							/*
							 * Commanders are ground-attackers that are much
							 * more effective than a soldier or a basher;
							 * however, they become progressively more expensive
							 * to build, so not too many resources should be
							 * invested into their development
							 */

							// Insert commander spawning here...
							// Currently being ignored due to overhead of Tech
							// Institute and not enough return.

							/*
							 * At this later point in the game, we will have
							 * hopefully achieved the following constructions:
							 * 
							 * 1) Build several helipads --> production of many
							 * drones 2) Build a couple miner factories -->
							 * spawning of many miners 3) Build a couple of
							 * barracks --> spawning of soldiers and bashers 4)
							 * Build at least one tank factory --> spawning of
							 * some tanks 5) Build a technology institute and a
							 * training field --> the training of a commander
							 * 
							 * At this stage of the game, given that we have
							 * this level of mobilization, other factors become
							 * important:
							 * 
							 * 1) Further development of artillery
							 * 
							 * At this point, we will probably have many drones,
							 * soldiers, and bashers. Thus, we need to focus on
							 * developing other artillery such as tanks,
							 * missiles, and drones, albeit less so than befre
							 * 
							 * 2) Improvement on tie-breaker criterion
							 * 
							 * Two of the tie-breakers relate to how much
							 * material has been amassed during the game. The
							 * fourth one is the number of handwash stations,
							 * while the fifth one is the amount of ore the army
							 * has PLUS the ore costs of all surviving robots.
							 * Step 1 above helps with this because each of
							 * these has an ore cost, and artillery like tank
							 * factories are particularly expensive.
							 * 
							 * In light of these priorities, several robots will
							 * not be considered during this game such as
							 * technology institutes and training fields because
							 * having multiple these is not very beneficial. We
							 * thus assign the following probabilities or upper
							 * bounds on the creation of the remaining
							 * structures:
							 * 
							 * 1) P(SUPPLYDEPOT) = 0.14
							 * ------------------------------------------------
							 * 2) P(BARRACKS) = 0.2
							 * ------------------------------------------------
							 * 3) P(HELIPAD) = 0.15
							 * ------------------------------------------------
							 * 4) P(AEROSPACELAB) = 0.15
							 * ------------------------------------------------
							 * 5) P(HANDWASHSTATION) = 0.05
							 * ------------------------------------------------
							 * 6) P(MINERFACTORY) = Non-probabilistic
							 * ------------------------------------------------
							 * 7) P(TANKFACTORY) = 0.3
							 */

							// TODO: cleanup and redefine these probabilities

						} else if (0.05 <= fate && fate < 0.06) {
							createUnit(RobotType.HANDWASHSTATION, true);
						} else if (0.06 <= fate && fate < 0.2) {
							if (rc.readBroadcast(NUM_FRIENDLY_SUPPLYDEPOT_CHANNEL) < 10) {
								createUnit(RobotType.SUPPLYDEPOT, true);
							}
						} else if (roundNum < 1500
								&& rc.readBroadcast(NUM_FRIENDLY_MINERFACTORY_CHANNEL) < 3) {
							createUnit(RobotType.MINERFACTORY, true);
						} else if (0.2 <= fate && fate < 0.45) {
							if (rc.readBroadcast(NUM_FRIENDLY_TANKFACTORY_CHANNEL) < 3) {
								createUnit(RobotType.TANKFACTORY, true);
							}
						} else if (0.45 <= fate && fate < 0.55) {
							createUnit(RobotType.HELIPAD, true);
						} else if (0.65 <= fate && fate < 0.8) {
							createUnit(RobotType.AEROSPACELAB, true);
						} else if (1.8 <= fate
								&& rc.readBroadcast(NUM_FRIENDLY_BARRACKS_CHANNEL) < 5) {
							createUnit(RobotType.BARRACKS, true);
						}
					}

					mineAndMove();
					break;

				case COMMANDER:
					attackEnemyZero();
					int commanderSwarm = rc
							.readBroadcast(COMMANDER_SWARM_CHANNEL);
					if (commanderSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(COMMANDER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(COMMANDER_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), true,
								false, false);
					} else {
						moveAround();
						// flyOnBoundary();
						// mobilize();
					}
					break;
				case COMPUTER:
					int computerSwarm = rc
							.readBroadcast(COMPUTER_SWARM_CHANNEL);
					if (computerSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(COMPUTER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(COMPUTER_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), false,
								false, false);
					} else {
						moveAround();
						// mobilize();
					}
					break;
				case DRONE:
					// TODO: Prevent circling drones from swarming, as they
					// serve
					// as crucial lockdown on enemy and often get killed when
					// attempting to swarm
					attackEnemyZero();
//					int droneSwarm = rc.readBroadcast(DRONE_SWARM_CHANNEL);
//					if (droneSwarm == GO_TO_LOCATION) {
//						int x = rc
//								.readBroadcast(DRONE_SWARM_LOCATION_X_CHANNEL);
//						int y = rc
//								.readBroadcast(DRONE_SWARM_LOCATION_Y_CHANNEL);
//						moveTowardDestination(new MapLocation(x, y), true,
//								false, false);
//					} else if (droneDefender) {
//						// TODO: Balance attack and defense
//						// Go in for the kill when there are sufficiently many
//						// near the enemy territory
//						droneCircle(true);
//					} else {
//						droneCircle(false);
//					}
					
					if(droneDefender){
						droneCircle(true);
					}else{
//						int maxDistanceSquared = 0;
//						MapLocation targetLocation = null;
//						for(MapLocation towerLocation : rc.senseEnemyTowerLocations()){
//							int HQtowerDistanceSquared = towerLocation.distanceSquaredTo(enemyHQ);
//							
//							if(HQtowerDistanceSquared > maxDistanceSquared){
//								maxDistanceSquared = HQtowerDistanceSquared;
//								targetLocation = towerLocation;
//							}
//						}
//						
//						if(maxDistanceSquared < RobotType.HQ.attackRadiusSquared && rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL) >= ){
//							moveTowardDestination(targetLocation, true, false, false);
//						}
						droneCircle(false);
					}
					
					break;
				case HANDWASHSTATION:
					// Wash hands.
					break;
				case HELIPAD:
					if (rc.readBroadcast(NUM_FRIENDLY_DRONES_CHANNEL)
							- rc.readBroadcast(NUM_ENEMY_DRONES_CHANNEL) - 3
							* rc.readBroadcast(NUM_ENEMY_LAUNCHERS_CHANNEL) < 25) {
						createUnit(RobotType.DRONE, false);
					}
					break;
				case LAUNCHER:
					// TODO: Fix missile launching and movement
					moveAround();
					
					if(rc.getMissileCount() > 0){
						rc.launchMissile(getRandomDirection());
					}else{
						createUnit(RobotType.MISSILE, false);
					}
					break;
				case MINER:
					attackEnemyZero();
					int minerSwarm = rc.readBroadcast(MINER_SWARM_CHANNEL);
					if (minerSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(MINER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(MINER_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), true,
								false, false);
					} else {
						mineAndMove();
					}
					break;
				case MINERFACTORY:
					int minerCount = rc
							.readBroadcast(NUM_FRIENDLY_MINERS_CHANNEL);
					double miningFate = rand.nextDouble();
					if (minerCount < 10
							|| (roundNum < 1500 && miningFate <= Math.pow(
									Math.E, -minerCount * 0.07))) {
						createUnit(RobotType.MINER, false);
					}
					break;
				case MISSILE:
					int missileSwarm = rc.readBroadcast(MISSILE_SWARM_CHANNEL);
					if (missileSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(MISSILE_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(MISSILE_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), true,
								false, false);
					} else {
						if (friendEnemyRatio(null,
								RobotType.MISSILE.attackRadiusSquared, null) <= 1) {
							rc.explode();
						}
					}
					break;
				case SOLDIER:
					attackEnemyZero(); // Soldiers attack, not mine.
					int soldierSwarm = rc.readBroadcast(SOLDIER_SWARM_CHANNEL);
					if (soldierSwarm == GO_TO_LOCATION) {
						int x = rc
								.readBroadcast(SOLDIER_SWARM_LOCATION_X_CHANNEL);
						int y = rc
								.readBroadcast(SOLDIER_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), true,
								false, false);
					} else {
						/*
						 * moveAround(); POSSIBLE OPTIMIZATION: chase enemies In
						 * addition, soldiers need to attack towers eventually,
						 * so they will have to move within attacking range of
						 * the towers, which is not possible under moveAround()
						 */
						moveAround();
						// flyOnBoundary();
						// mobilize();
					}
					break;
				case SUPPLYDEPOT:
					break;
				case TANK:
					attackEnemyZero();
					int tankSwarm = rc.readBroadcast(TANK_SWARM_CHANNEL);
					if (tankSwarm == GO_TO_LOCATION) {
						int x = rc.readBroadcast(TANK_SWARM_LOCATION_X_CHANNEL);
						int y = rc.readBroadcast(TANK_SWARM_LOCATION_Y_CHANNEL);
						moveTowardDestination(new MapLocation(x, y), true,
								false, false);
					} else {
						droneCircle(true);
						// moveAround();
						// flyOnBoundary();
						// mobilize();
					}
					break;
				case TANKFACTORY:
					createUnit(RobotType.TANK, false);
					break;
				case TECHNOLOGYINSTITUTE:
					break;
				case TOWER:
					attackEnemyZero(); // basic attacking method
					break;

				case TRAININGFIELD:
					/*
					 * Limit the number of commanders that will be produced in
					 * the game (assuming any die) since their cost increases.
					 */

					int numCommanders = rc
							.readBroadcast(NUM_FRIENDLY_COMMANDERS_CHANNEL);

					if (!rc.hasCommander() && numCommanders < 3) {
						if (createUnit(RobotType.COMMANDER, false)) {
							rc.broadcast(NUM_FRIENDLY_COMMANDERS_CHANNEL, 1);
						}
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

			rc.yield(); // Robot yields its turn --> saves bytecode to avoid
						// hitting the limit.
		}
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

	private static LinkedList<MapLocation> search(MapLocation dest, boolean DFS) {
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

				LinkedList<MapLocation> nodeLocations = getChildren(currentNode
						.getLoc());

				for (MapLocation nodeLocation : nodeLocations) {
					SearchNode childNode = new SearchNode(nodeLocation,
							currentNode);

					if (reachedGoal(nodeLocation, dest)) {
						return childNode.getPath();
					} else {
						if (!visited.contains(nodeLocation)) {
							visited.add(nodeLocation);
							agenda.add(childNode);
						}
					}
				}
			}
		}

		return null;
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

	/*
	 * ==========================================================================
	 */

	/**
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
	 * 
	 * @param loc
	 * @param radiusSquared
	 * @return
	 */
	private static int measureCrowdedness(MapLocation loc, int radiusSquared) {
		// TODO: make more sophisticated
		int numBadTiles = 0;

		if (radiusSquared <= 9) {
			for (MapLocation location : MapLocation
					.getAllMapLocationsWithinRadiusSq(loc, radiusSquared)) {
				if (rc.senseTerrainTile(location) != TerrainTile.NORMAL) {
					++numBadTiles;
				}
			}
		}
		return rc.senseNearbyRobots(loc, radiusSquared, Friend).length
				+ numBadTiles;
	}

	/**
	 * 
	 * @throws GameActionException
	 */
	private static void flyOnBoundary() throws GameActionException {
		defendHQ();
		moveAround();
	}

	/**
	 * 
	 * @throws GameActionException
	 */
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
			int distanceToMyHQ = currentLocation.distanceSquaredTo(myHQ);
			int distanceToEnemyHQ = currentLocation.distanceSquaredTo(enemyHQ);
			if (distanceToMyHQ > .6 * distanceToEnemyHQ) {
				// double turnLittle = rand.nextDouble();
				// Direction currentDirection =
				// currentLocation.directionTo(myHQ);
				//
				// if(turnLittle < 0.33){
				// currentDirection = currentDirection.rotateLeft();
				// }else if(turnLittle > 0.33 & turnLittle < 0.66){
				// currentDirection = currentDirection.rotateRight();
				// }

				moveTowardDestination(myHQ, true, false, false);

			} else if (distanceToMyHQ < .2 * distanceToEnemyHQ) {
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

				moveTowardDestination(enemyHQ, true, false, false);
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

				moveTowardDestination(newLocation, true, false, false);
			}
		}
	}

	/**
	 * 
	 * @param shielding
	 * @throws GameActionException
	 */
	private static void droneCircle(boolean shielding)
			throws GameActionException { // FTW!

		// TODO: Develop a droneLine() method that can generate a "wall"
		// of drones for defensive purposes

		if (shielding) {
			boolean clear = true;
			if (rc.getSupplyLevel() > 50 && rc.getType() == RobotType.DRONE) {
				RobotInfo[] incomingEnemies = rc.senseNearbyRobots(myHQ,
						halfwayDistance, Enemy);

				for (RobotInfo robot : incomingEnemies) {
					if (robot.type == RobotType.DRONE
							|| robot.type == RobotType.LAUNCHER) {
						moveTowardDestination(robot.location, false, true, true);
						clear = false;
						break;
					}

				}
			}

//			if (rc.getSupplyLevel() <= 50) {
//				moveTowardDestination(
//						droneShieldLocations.get(currentDroneDirectionIndex),
//						false, false, false);
//			} else if (clear) {
//				moveTowardDestination(
//						droneAttackCircleLocations
//								.get(currentDroneDirectionIndex),
//						false, false, false);
//			}

			 if (clear) {
					moveTowardDestination(
							droneAttackCircleLocations
									.get(currentDroneDirectionIndex),
							false, false, false);
				}
			 
		} else {
			moveTowardDestination(
					droneAttackCircleLocations.get(currentDroneDirectionIndex),
					false, false, false);
		}

		droneCircleRound = (droneCircleRound + 1) % 150;
		if (droneCircleRound == 0) {
			currentDroneDirectionIndex = (currentDroneDirectionIndex + 1)
					% droneShieldLocations.size();
		}

	}

	/**
	 * 
	 * @throws GameActionException
	 */
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

	/**
	 * 
	 * @param roboType
	 * @param build
	 * @return
	 * @throws GameActionException
	 */
	private static boolean createUnit(RobotType roboType, boolean build)
			throws GameActionException {
		if (rc.isCoreReady() && rc.getTeamOre() > roboType.oreCost) {
			MapLocation currentLocation = rc.getLocation();
			Direction testDir = getRandomDirection();
			boolean goLeft = rand.nextDouble() > 0.5;

			for (int turnCount = 0; turnCount < 8; turnCount++) {
				MapLocation testLoc = currentLocation.add(testDir);

				if (build) {
					if (rc.canBuild(testDir, roboType)
							&& isSafe(testLoc, false, false)) {
						rc.build(testDir, roboType);
						return true;
					}
				} else { // spawning
					if (rc.canSpawn(testDir, roboType)
							&& isSafe(testLoc, false, false)) {
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
	 * 
	 * @return
	 */
	private static Direction getRandomDirection() {
		return Direction.values()[(int) rand.nextDouble() * 8];
	}

	/**
	 * 
	 * @param dest
	 * @param ignoreSafety
	 * @param onlyHQAndTowers
	 * @param attackDrones
	 *            - matters only if onlyHQAndTowers -> true
	 * @return
	 * @throws GameActionException
	 */
	private static boolean moveTowardDestination(MapLocation dest,
			boolean ignoreSafety, boolean onlyHQAndTowers, boolean attackDrones)
			throws GameActionException {
		// TODO: Should we consider including a "crowdedness" heuristic? If so,
		// how do we incorporate our current implementation?

		Direction straight = rc.getLocation().directionTo(dest);
		MapLocation currentLocation = rc.getLocation();
		Direction backupDir = null;
		boolean backup = false;
		
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
							|| isSafe(possSquare, onlyHQAndTowers, attackDrones)) {
						if(lastLocation != null){
							if(possSquare.x == lastLocation.x && possSquare.y == lastLocation.y){
								System.out.println("Called!");
								backupDir = possDirection;
								backup = true;
								continue;
							}
						}
						
						lastLocation = currentLocation;
						rc.move(possDirection);
						return true;
					}
				}
			}
			
			if(backup){
				lastLocation = currentLocation;
				rc.move(backupDir);
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * @param d
	 * @return
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
				moveTowardDestination(bestDestination, false, false, false);
			} else {
				moveAround();
			}
		}
	}

	/**
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
			boolean tileInFrontSafe = isSafe(tileInFrontLocation, false, false);
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
	 * 
	 * @param loc
	 * @param onlyHQAndTowers
	 * @param attackDrones
	 *            - matters only if onlyHQAndTowers -> True
	 * @return
	 */
	private static boolean isSafe(MapLocation loc, boolean onlyHQAndTowers,
			boolean attackDrones) {
		TerrainTile locTerrain = rc.senseTerrainTile(loc);
		RobotType roboType = rc.getType();

		if (locTerrain != TerrainTile.NORMAL) {
			if (!(locTerrain == TerrainTile.VOID && (roboType == RobotType.DRONE || roboType == RobotType.MISSILE))) {
				return false;
			}
		}

		// Check if HQ is in range
		if (Math.sqrt(enemyHQ.distanceSquaredTo(loc)) <= (int) (Math.sqrt(RobotType.HQ.attackRadiusSquared) + 
				Math.sqrt(2)) + 1) {
			return false;
		}

		// Check if towers are in range
		for (MapLocation enemyTower : rc.senseEnemyTowerLocations()) {
			if (enemyTower.distanceSquaredTo(loc) <= RobotType.TOWER.attackRadiusSquared) {
				return false;
			}
		}

		// Check if any enemies are in range
		if (!onlyHQAndTowers) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(
					roboType.sensorRadiusSquared, Enemy);
			for (RobotInfo r : enemyRobots) {
				if (r.location.distanceSquaredTo(loc) <= r.type.attackRadiusSquared) {
					return false;
				}
			}
		} else if (attackDrones) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(
					roboType.sensorRadiusSquared, Enemy);
			for (RobotInfo r : enemyRobots) {
				if (r.location.distanceSquaredTo(loc) <= r.type.attackRadiusSquared
						&& r.type != RobotType.DRONE) {
					return false;
				}
			}

		}

		return true;
	}

	/**
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
	 * 
	 * @throws GameActionException
	 */
	private static void attackEnemyZero() throws GameActionException {
		if(rc.isWeaponReady()){
			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
					rc.getType().attackRadiusSquared, rc.getTeam().opponent());
			
			if(nearbyEnemies.length > 0 && rc.canAttackLocation(nearbyEnemies[0].location)) {
				rc.attackLocation(nearbyEnemies[0].location);
			}else if(rc.getType() == RobotType.HQ && rc.senseTowerLocations().length >= 5){
				int splashRadius = 2;
				int HQradius = RobotType.HQ.attackRadiusSquared;
				int attackRadius = (int) Math.pow(Math.sqrt(HQradius) + Math.sqrt(splashRadius)
						, 2) + 1;
				
				nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), attackRadius,
						rc.getTeam().opponent());
				
				if(nearbyEnemies.length > 0){
					if(myHQ.distanceSquaredTo(nearbyEnemies[0].location) > HQradius){
						Direction toHQ = nearbyEnemies[0].location.directionTo(myHQ);
						MapLocation targetLoc = nearbyEnemies[0].location.add(toHQ);
						
						if(rc.canAttackLocation(targetLoc)){
							rc.attackLocation(targetLoc);
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @throws GameActionException
	 */
	private static void attackBestEnemy() throws GameActionException {
		// Don't do anything if the weapon is not ready.
		if (!rc.isWeaponReady()) {
			return;
		}

		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
				rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		RobotInfo target = null;
		// int currentTargetPriority;
		// int enemyPriority;
		for (RobotInfo enemy : nearbyEnemies) {
			if (Clock.getBytecodeNum() > 2000) {
				break;
			}
			if (rc.canAttackLocation(enemy.location)) {
				// TODO: Add more detail in choosing an enemy to attack.
				// Currently attacks the lowest health enemy in range.
				if (target == null || enemy.health < target.health) {
					// currentTargetPriority =
					// attackPriorityMap.get(target.type);
					// enemyPriority = attackPriorityMap.get(enemy.type);
					target = enemy;
				}

			}

		}
		if (target != null) {
			rc.attackLocation(target.location);
		}
	}

	/**
	 * 
	 * @return
	 */
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

	/**
	 * 
	 * @throws GameActionException
	 */
	private static void transferSupplies() throws GameActionException {
		// TODO: Do we want to have a global ordering on robots? So that
		// robots may decide to "sacrifice" themselves for the sake of a
		// stronger, more able robot?

		if (!rc.getType().needsSupply()) {
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
	 * 
	 * @param types
	 * @param action
	 * @param location
	 * @throws GameActionException
	 */
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

	// ************************ START OF UNIT COUNTING ************************

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

	/**
	 * 
	 * @throws GameActionException
	 */
	@SuppressWarnings("fallthrough")
	private static void updateUnitCounts() throws GameActionException {

		// Run part of the work on each round
		int roundNumMod = roundNum % 5;
		if (roundNumMod == 0 || friendlyRobots == null || enemyRobots == null) {
			// Collect all robots into separate RobotInfo arrays.
			friendlyRobots = rc.senseNearbyRobots(999999, Friend);
			enemyRobots = rc.senseNearbyRobots(999999, Enemy);
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

	// ^^^^^^^^^^^^^^^^^^^^^^^^^^ END OF UNIT COUNTING ^^^^^^^^^^^^^^^^^^^^^^^^

	// ************************** START OF MAP ANALYSIS ***********************
	/**
	 * 
	 * @throws GameActionException
	 */
	private static void analyzeTowerStrength() throws GameActionException {
		MapLocation[] myTowers = rc.senseTowerLocations(); /*
															 * because the board
															 * is symmetrical,
															 * we can use our
															 * own towers for
															 * analysis, which
															 * is cheaper
															 */
		int towerStrength = 0;

		// One or no towers -> very weak. Keep at 0.
		// Otherwise measure strength based on closeness.
		for (int i = 0; i < myTowers.length; ++i) {
			if (myTowers[i].distanceSquaredTo(myHQ) < 48) {
				towerStrength += 2; /*
									 * HQ can inflict thrice the damage but has
									 * double the delay compared to a tower
									 */
			}
			for (int j = i; j < myTowers.length; ++j) {
				if (myTowers[i].distanceSquaredTo(myTowers[j]) < 48) {
					towerStrength += 1;
				}
			}
		}

		rc.broadcast(TOWER_STRENGTH_CHANNEL, towerStrength);
	}

	/**
	 * 
	 * @throws GameActionException
	 */
	private static void getMapDimensions() throws GameActionException {
		MapLocation[] myTowers = rc.senseTowerLocations();
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();

		xmin = Integer.MAX_VALUE;
		xmax = Integer.MIN_VALUE;
		ymin = Integer.MAX_VALUE;
		ymax = Integer.MIN_VALUE;

		for (MapLocation loc : myTowers) {
			xmin = (xmin > loc.x) ? loc.x : xmin;
			xmax = (xmax > loc.x) ? xmax : loc.x;

			ymin = (ymin > loc.y) ? loc.y : xmin;
			ymax = (ymax > loc.y) ? xmax : loc.y;
		}

		for (MapLocation loc : enemyTowers) {
			xmin = (xmin > loc.x) ? loc.x : xmin;
			xmax = (xmax > loc.x) ? xmax : loc.x;

			ymin = (ymin > loc.y) ? loc.y : xmin;
			ymax = (ymax > loc.y) ? xmax : loc.y;
		}

		xmin = (xmin > myHQ.x) ? myHQ.x : xmin;
		xmin = (xmin > enemyHQ.x) ? enemyHQ.x : xmin;

		xmax = (xmax > myHQ.x) ? xmax : myHQ.x;
		xmax = (xmax > enemyHQ.x) ? xmax : enemyHQ.x;

		ymin = (ymin > myHQ.y) ? myHQ.y : ymin;
		ymin = (ymin > enemyHQ.y) ? enemyHQ.y : ymin;

		ymax = (ymax > myHQ.y) ? ymax : myHQ.y;
		ymax = (ymax > enemyHQ.y) ? ymax : enemyHQ.y;
	}

	/**
	 * 
	 * @throws GameActionException
	 */
	private static void analyzeMapMobility() throws GameActionException {
		while (ypos < ymax + 1) {
			TerrainTile terrain = rc.senseTerrainTile(new MapLocation(xpos,
					ypos));

			switch (terrain) {

			case NORMAL:
				totalNormal++;
				break;

			case VOID:
				totalVoid++;
				break;

			default:
				totalProcessed++;
			}

			xpos++;
			totalProcessed++;

			if (xpos >= xmax + 1) {
				ypos++;
				xpos = xmin;
			}

			if (Clock.getBytecodesLeft() < 100) {
				return;
			}

			normRatio = (double) totalNormal / totalProcessed;
			doneAnalyzing = true;
		}
	}

	// ^^^^^^^^^^^^^^^^^^^^^^^^^^ END OF MAP ANALYSIS ^^^^^^^^^^^^^^^^^^^^^^^^^

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
	private static final int MAP_MOBILITY_CHANNEL = 2001;

	public static void main(String[] args) {
		totalProcessed++;
	}

	// ------------------------------------------------------------------------
	// Action Constants
	// ------------------------------------------------------------------------
	private static final int NO_ACTION = 0;
	private static final int GO_TO_LOCATION = 1;

	// ^^^^^^^^^^^^^^^^^^ BROADCAST CONSTANTS END HERE ^^^^^^^^^^^^^^^^^^^^^^^^

}
