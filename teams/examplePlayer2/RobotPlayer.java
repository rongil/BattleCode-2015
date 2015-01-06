// Example robot from the YouTube channel

package examplePlayer2;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {

	static Direction facing;
	static Random rand; /* this will help to distinguish each robot
						   otherwise, each robot will behave exactly
						   the same */
	static RobotController rc;
	
	public static void run(RobotController myRC){
		rc = myRC;
		rand = new Random(rc.getID());
		facing = getRandomDirection(); // randomize starting direction
		
		// if the run method ends, the robot dies
		while(true){
			try{
				if(rc.getType() == RobotType.HQ){
					/* each action requires several conditions that need to be satisfied;
					   otherwise, exceptions will be thrown, each of which incurs a 500
					   bytecode penalty */
					
					attackEnemyZero(); /* we can this method first before spawning beavers 
					 					  because we probably will want to spawn more efficient
					 					  creatures such as miners; in addition, spawning introduces
					 					  attacking delay */
					spawnUnit(RobotType.BEAVER);
					
				}else if(rc.getType() == RobotType.BEAVER){
					attackEnemyZero();
					
					if(Clock.getRoundNum() < 700){
						buildUnit(RobotType.MINERFACTORY);
					}else{
						buildUnit(RobotType.BARRACKS);
					}
					
					mineAndMove();
					
				}else if(rc.getType() == RobotType.MINER){
					attackEnemyZero();
					mineAndMove();
				}else if(rc.getType() == RobotType.MINERFACTORY){
					spawnUnit(RobotType.MINER);
				}else if(rc.getType() == RobotType.BARRACKS){
						spawnUnit(RobotType.SOLDIER); // can also spawn bashers
				}else if(rc.getType() == RobotType.TOWER){
					attackEnemyZero(); // basic attacking method
				}else if(rc.getType() == RobotType.SOLDIER){
					attackEnemyZero(); // soldiers attack, not mine 
					moveAround(); /* POSSIBLE OPTIMIZATION: chase enemies
									 In addition, soldiers need to attack towers eventually,
									 so they will have to move within attacking range of the
									 towers, which is not possible under moveAround() */
				}
				
				/* If robots go low on supplies, they will become less effective in attacking;
				   HQ supplies goods at a constant rate + any additional units from having supply
				   depots built; these units are to be passed from HQ among the robots in a way
				   such that all robots are sufficiently supplied 
				   
				   NOTE: robots that are low on supplies will have a white square around them */
				
				transferSupplies();
				
			}catch(GameActionException e){ /* spawn method contains GameActionException
										      need to catch these exceptions; otherwise,
										      the function returns, and robot explodes */
				e.printStackTrace();
			}
			
			rc.yield(); // robot yields its turn --> saves bytecode to avoid hitting limit
		}
	}

	private static void spawnUnit(RobotType roboType) throws GameActionException{
		Direction randomDir = getRandomDirection();
		if(rc.isCoreReady() && rc.canSpawn(randomDir, roboType)){
			rc.spawn(randomDir, roboType);
		}
	}

	private static Direction getRandomDirection() {
		return Direction.values()[(int)rand.nextDouble() * 8];
	}

	/* need to put "throws GameActionException" because the method is called in run(),
	   which catches this kind of exception, so we need this method to be able to throw
	   that kind of exception so that it can able handled in run() */
	
	private static void moveAround() throws GameActionException{
		if(rand.nextDouble() < 0.05){
			if(rand.nextDouble() < 0.5){
				facing = facing.rotateLeft(); // 45 degree turn
			}else{
				facing = facing.rotateRight();
			}
		}
		
		MapLocation tileInFront = rc.getLocation().add(facing);
	
		/* check that the direction in front is not a tile that can be attacked by
		   the enemy towers (does not include HQ) */
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		boolean tileInFrontSafe = true;
		
		for(MapLocation m : enemyTowers){
			if(m.distanceSquaredTo(tileInFront) <= RobotType.TOWER.attackRadiusSquared){
				tileInFrontSafe = false;
				break; // one tower is enough to make it unsafe
			}
		}
		
		// check that are we are not facing off the edge of the map
		// this conditional makes sense for ground units but not drone units, which
		// can fly and are hence not hindered as much
		//
		// we also check that the tile in front is not within range of a tower
		
		if(rc.senseTerrainTile(tileInFront) != TerrainTile.NORMAL || !tileInFrontSafe){
			facing = facing.rotateLeft();
		}else{ // try to move in the facing direction since the tile in front is safe
			if(rc.isCoreReady() && rc.canMove(facing)){
				rc.move(facing);
			}
		}
	}
	
	private static void mineAndMove() throws GameActionException{
		if(rc.senseOre(rc.getLocation()) > 1){ // if there is ore, try to mine
			if(rc.isCoreReady() && rc.canMine()){
				rc.mine();
			}
		}else{ // otherwise, look for ore
			moveAround();
		}
	}
	
	private static void attackEnemyZero() throws GameActionException{
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		if(nearbyEnemies.length > 0){ /* there are enemies nearby, and we'll
										 try to shoot a nearbyEnemies[0] */
			if(rc.isWeaponReady() && rc.canAttackLocation(nearbyEnemies[0].location)){
				rc.attackLocation(nearbyEnemies[0].location);
			}
		}
	}
	
	private static void transferSupplies() throws GameActionException{
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;
		
		MapLocation suppliesToThisLocation = null;
		
		for(RobotInfo ri : nearbyAllies){
			if(ri.supplyLevel < lowestSupply){
				lowestSupply = ri.supplyLevel;
				transferAmount = (rc.getSupplyLevel() - ri.supplyLevel) / 2;
				suppliesToThisLocation = ri.location;
			}
		}
		
		if(suppliesToThisLocation != null){
			rc.transferSupplies((int)transferAmount, suppliesToThisLocation);
		}
	}

	private static void buildUnit(RobotType roboType) throws GameActionException{
		if(rc.getTeamOre() > roboType.oreCost){
			Direction buildDir = getRandomDirection();
			
			if(rc.isCoreReady() && rc.canBuild(buildDir, roboType)){
				rc.build(buildDir, roboType);
			}
		}
	}
}
