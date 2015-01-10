// unittest player from the YouTube channel

package examplePlayer4;

import battlecode.common.*;

public class RobotPlayer{
	
	public static void run(RobotController rc){
		
		while(true){
			try {
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),rc.getType().sensorRadiusSquared,rc.getTeam().opponent());
				if(rc.getType()==RobotType.DRONE){
					if(nearbyEnemies.length>0){
						int distToEnemy =rc.getLocation().distanceSquaredTo(nearbyEnemies[0].location);
						boolean inEnemyRange = distToEnemy<=nearbyEnemies[0].type.attackRadiusSquared;
						boolean enemyInRange = rc.canAttackLocation(nearbyEnemies[0].location);
						if(inEnemyRange){//run away!
							if(rc.isCoreReady()&&rc.canMove(Direction.WEST)){
								rc.move(Direction.WEST);
							}
						}else{//try to attack
							if(rc.isWeaponReady()&&enemyInRange){
								rc.attackLocation(nearbyEnemies[0].location);
							}
						}
					}
				}else if(rc.getType()==RobotType.SOLDIER){
					if(nearbyEnemies.length>0){
						RobotInfo anEnemy = nearbyEnemies[nearbyEnemies.length-1];
						int distToEnemy =rc.getLocation().distanceSquaredTo(anEnemy.location);
						boolean inEnemyRange = distToEnemy<=anEnemy.type.attackRadiusSquared;
						boolean enemyInRange = rc.canAttackLocation(anEnemy.location);
						if(!enemyInRange){//enemy out of range; advance to attack
							if(rc.isCoreReady()&&rc.canMove(Direction.WEST)){
								rc.move(Direction.WEST);
							}
						}else{//enemy in range; try to attack
							if(rc.isWeaponReady()){
								rc.attackLocation(anEnemy.location);
							}
						}
					}else{//no enemies in sight; advance to attack
						if(rc.isCoreReady()&&rc.canMove(Direction.WEST)){
							rc.move(Direction.WEST);
						}
					}
				}else if(rc.getType()==RobotType.HQ){
					transferSupplies(rc);
				}

			} catch (GameActionException e) {
				
				e.printStackTrace();
			}
			
			rc.yield();
		}
		
	}
	
	private static void transferSupplies(RobotController rc) throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;
		MapLocation suppliesToThisLocation = null;
		for(RobotInfo ri:nearbyAllies){
			if(ri.supplyLevel<lowestSupply){
				lowestSupply = ri.supplyLevel;
				transferAmount = (rc.getSupplyLevel()-ri.supplyLevel)/2;
				suppliesToThisLocation = ri.location;
			}
		}
		if(suppliesToThisLocation!=null){
			rc.transferSupplies((int)transferAmount, suppliesToThisLocation);
		}
	}
	
}