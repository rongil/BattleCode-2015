// Run on trenches.xml

package searchBot;

import battlecode.common.*;
import java.util.LinkedList;

public class RobotPlayer {

	private static RobotController rc;
	private static RobotType thisRobotType;
	private static MapLocation friendlyHQ;
	private static MapLocation currentLocation;
	
	private static final int CURRENT_LOCATION_X_CHANNEL = 0;
	private static final int CURRENT_LOCATION_Y_CHANNEL = 1;
	
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		thisRobotType = rc.getType();
		friendlyHQ = rc.senseHQLocation();
		
		if (thisRobotType == RobotType.HQ) {
			currentLocation = friendlyHQ;
			
			rc.broadcast(CURRENT_LOCATION_X_CHANNEL, currentLocation.x);
			rc.broadcast(CURRENT_LOCATION_Y_CHANNEL, currentLocation.y);
		}
		
		while (true) {
			try {
				currentLocation = new MapLocation(rc.readBroadcast(CURRENT_LOCATION_X_CHANNEL),
						rc.readBroadcast(CURRENT_LOCATION_Y_CHANNEL));
				
				search(new MapLocation(-12903, 13138), false);

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

			if (Clock.getBytecodesLeft() < 200) {
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
				rc.broadcast(channelHashFunc(parentLoc),
						directionToInt(parentToCurrent));
			}

			LinkedList<MapLocation> nodeLocations = getChildren(currentNode.getLoc());

			for (MapLocation nodeLocation : nodeLocations) {
				SearchNode childNode = new SearchNode(nodeLocation, currentNode);

				if (reachedGoal(nodeLocation, dest)) {
					Direction currentToChild = currentLoc.directionTo(nodeLocation);
					rc.broadcast(channelHashFunc(currentLoc), directionToInt(currentToChild));
					
					System.out.println(childNode.getPath());
					currentLocation = friendlyHQ;
					
					rc.resign();
					return true;

				} else {
					if (!visited.contains(nodeLocation)) {
						visited.add(nodeLocation);
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

			possibleChildren.add(possSquare);
			
//			if (possSquareTerrain == TerrainTile.NORMAL) {
//				possibleChildren.add(possSquare);
//			}
		}

		return possibleChildren;
	}

	private static int channelHashFunc(MapLocation loc) {
		int maxWidth = GameConstants.MAP_MAX_WIDTH;
		int maxHeight = GameConstants.MAP_MAX_HEIGHT;
		return 10000 + loc.x % maxWidth + maxWidth * loc.y % maxHeight;
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
