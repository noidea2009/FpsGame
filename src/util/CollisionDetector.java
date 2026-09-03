package util;

import map.WorldMap;

/**
 * Stateless utility performing circle-vs-grid collision checks between a
 * player-sized circle and wall tiles in a WorldMap. Movement methods call
 * this class once per axis so that contact along one axis does not block
 * movement along the other, producing a sliding effect against walls.
 */
public class CollisionDetector {

    /** Default player collision radius, expressed as a fraction of one tile. */
    public static final double DEFAULT_PLAYER_RADIUS_TILES = 0.2;

    /**
     * Private constructor prevents instantiation of this stateless utility class.
     */
    private CollisionDetector() {
    }

    /**
     * Determines whether a circle of the given radius, centered at the given
     * world-space position, is free of wall overlap. Four points offset from
     * center along both axes by the radius are checked against the grid,
     * approximating the circle boundary without requiring exact circle-tile
     * intersection math.
     *
     * @param x world-space x coordinate of the circle center
     * @param y world-space y coordinate of the circle center
     * @param radiusTiles collision radius, expressed as a fraction of one tile
     * @param map world map providing wall queries and tile size
     * @return true if the position is free of wall overlap, false if any sampled point falls on a wall
     */
    public static boolean canMoveTo(double x, double y, double radiusTiles, WorldMap map) {
        double radius = radiusTiles * map.getTileSize();

        boolean blockedRight = map.isWall(x + radius, y);
        boolean blockedLeft = map.isWall(x - radius, y);
        boolean blockedDown = map.isWall(x, y + radius);
        boolean blockedUp = map.isWall(x, y - radius);

        return !(blockedRight || blockedLeft || blockedDown || blockedUp);
    }

    /**
     * Determines whether a circle of the default player radius, centered at
     * the given world-space position, is free of wall overlap.
     *
     * @param x world-space x coordinate of the circle center
     * @param y world-space y coordinate of the circle center
     * @param map world map providing wall queries and tile size
     * @return true if the position is free of wall overlap, false if any sampled point falls on a wall
     */
    public static boolean canMoveTo(double x, double y, WorldMap map) {
        return canMoveTo(x, y, DEFAULT_PLAYER_RADIUS_TILES, map);
    }
}