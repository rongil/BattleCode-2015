// Run on trenches.xml

package searchBot;

import battlecode.common.*;
import java.util.LinkedList;

public class RobotPlayer {

	private static RobotController rc;
	private static RobotType thisRobotType;
	private static MapLocation friendlyHQ;
	private static MapLocation enemyHQ;
	private static MapLocation currentLocation;
	private static LinkedList<SearchNode> agenda;
	
	private static int min_HQ_x;
	private static int max_HQ_x;
	private static int min_HQ_y;
	private static int max_HQ_y;
	
	private static final int CURRENT_LOCATION_X_CHANNEL = 0;
	private static final int CURRENT_LOCATION_Y_CHANNEL = 1;
	
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		thisRobotType = rc.getType();
		friendlyHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
	
		min_HQ_x = Math.min(friendlyHQ.x, enemyHQ.x);
		max_HQ_x = Math.max(friendlyHQ.x, enemyHQ.x);
		min_HQ_y = Math.min(friendlyHQ.y, enemyHQ.y);
		max_HQ_y = Math.max(friendlyHQ.y, enemyHQ.y);
		
		if (thisRobotType == RobotType.HQ) {
			currentLocation = friendlyHQ;
			agenda = new LinkedList<SearchNode>();
			
			rc.broadcast(CURRENT_LOCATION_X_CHANNEL, currentLocation.x);
			rc.broadcast(CURRENT_LOCATION_Y_CHANNEL, currentLocation.y);
		}
		
		while (true) {
			try {
				if(thisRobotType == RobotType.HQ) {
					currentLocation = new MapLocation(rc.readBroadcast(CURRENT_LOCATION_X_CHANNEL),
							rc.readBroadcast(CURRENT_LOCATION_Y_CHANNEL));
					
					search(new MapLocation(-12919, 13146), true);
				}
				
			} catch (GameActionException e) {
				e.printStackTrace();
			}

			rc.yield();
		}

	}
	
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
		
		if (reachedGoal(currentLocation, dest)) {
			return true;
		}
		
		agenda.add(new SearchNode(currentLocation, null));
		rc.broadcast(channelHashFunc(currentLocation, 10000), 1);
		
		SearchNode currentNode;

		while (agenda.size() != 0) {
			if (DFS) {
				currentNode = agenda.removeLast();
			} else {
				currentNode = agenda.removeFirst();
			}
			
			System.out.println("Bytecode Remaining: " + Clock.getBytecodesLeft());

			if (Clock.getBytecodesLeft() < 700) {
				rc.broadcast(CURRENT_LOCATION_X_CHANNEL, currentNode.getLoc().x);
				rc.broadcast(CURRENT_LOCATION_Y_CHANNEL, currentNode.getLoc().y);
				return false;
			}
			
			System.out.println("Expanding Location (" + currentNode.getLoc().x + ", " + currentNode.getLoc().y + ")...");
			MapLocation currentLoc = currentNode.getLoc();
			SearchNode parentNode = currentNode.getParent();
				
			if(parentNode != null) {
				MapLocation parentLoc = parentNode.getLoc();
				Direction parentToCurrent = parentLoc.directionTo(currentLoc);
				rc.broadcast(channelHashFunc(parentLoc, 30000),
						directionToInt(parentToCurrent));
			}

			LinkedList<MapLocation> nodeLocations = getChildren(currentNode.getLoc());

			for (MapLocation nodeLocation : nodeLocations) {
				SearchNode childNode = new SearchNode(nodeLocation, currentNode);

				if (reachedGoal(nodeLocation, dest)) {
					Direction currentToChild = currentLoc.directionTo(nodeLocation);
					rc.broadcast(channelHashFunc(currentLoc, 30000), directionToInt(currentToChild));
					
					System.out.println(childNode.getPath());
					currentLocation = friendlyHQ;
					
					rc.resign();
					return true;

				} else {
					int nodeChannel = channelHashFunc(nodeLocation, 10000);
					int visited = rc.readBroadcast(nodeChannel);
					
					if (visited == 0) {
						rc.broadcast(nodeChannel, 1);
						agenda.add(childNode);
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

			if ((possSquare.x < min_HQ_x && max_HQ_x - possSquare.x > 30) ||
					(possSquare.x > max_HQ_x && possSquare.x - min_HQ_x > 30) ||
					(possSquare.y < min_HQ_y && max_HQ_y - possSquare.y > 30) ||
					(possSquare.y > max_HQ_y && possSquare.y - min_HQ_y > 30) ||
					(possSquareTerrain != TerrainTile.NORMAL)){
				continue;
				
			} else {
				possibleChildren.add(possSquare);
			}
		}

		return possibleChildren;
	}

	private static int channelHashFunc(MapLocation loc, int offset) {
		int maxWidth = GameConstants.MAP_MAX_WIDTH;
		int maxHeight = GameConstants.MAP_MAX_HEIGHT;
		return offset + loc.x % maxWidth + maxWidth * loc.y % maxHeight;
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
