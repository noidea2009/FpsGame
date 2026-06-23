package map;

/**
 * Responsibility:
 * Stores 2D grid-based world
 *
 * Fields:
 * int[][] grid;
 * int tileSize;
 * int width, height;
 * Methods:
 * boolean isWall(int x, int y)
 * int getTile(int x, int y)
 * void loadMap(int[][] data)
 * Data Types:
 * Map grid: int[][]
 *
 * 0 = empty space
 * 1+ = wall types
 * Ownership:
 *
 *  Shared read-only resource for:
 *
 *      Player
 *      Raycaster
 *      Collision system
 */
public class WorldMap {
}
