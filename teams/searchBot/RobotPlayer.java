package searchBot;

import battlecode.common.*;
import java.util.LinkedList;

public class RobotPlayer {

	private static RobotController rc;
	private static RobotType thisRobotType;

	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;
		thisRobotType = rc.getType();
		
		while (true) {
			try {
				switch (thisRobotType) {

				case HQ:
					break;
					
				default:
					break;

				}

			} catch (Exception e) {
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
