package map;

/**
 * Stores a 2D grid-based world and exposes read-only spatial queries
 * for movement, collision, and rendering systems.
 */
public class WorldMap {

    private int[][] grid;
    private int tileSize;
    private int width;
    private int height;

    /**
     * Loads and validates grid data into the map.
     * Rejects null data, non-rectangular rows, negative dimensions,
     * and negative tile values before assigning the grid.
     *
     * @param data     two-dimensional array representing wall layout;
     *                 0 indicates empty space, 1 or greater indicates a wall type
     * @param tileSize size of a single grid tile in world units; must be positive
     * @throws IllegalArgumentException if data is null, empty, non-rectangular,
     *                                   contains negative tile values, or tileSize is not positive
     */
    public void loadMap(int[][] data, int tileSize) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Grid data cannot be null or empty.");
        }
        if (tileSize <= 0) {
            throw new IllegalArgumentException("Tile size must be positive.");
        }

        int expectedRowLength = data[0].length;
        if (expectedRowLength == 0) {
            throw new IllegalArgumentException("Grid rows cannot be empty.");
        }

        for (int row = 0; row < data.length; row++) {
            if (data[row] == null || data[row].length != expectedRowLength) {
                throw new IllegalArgumentException(
                        "Grid row " + row + " has inconsistent length; expected " + expectedRowLength + "."
                );
            }
            for (int col = 0; col < data[row].length; col++) {
                if (data[row][col] < 0) {
                    throw new IllegalArgumentException(
                            "Negative tile value at row " + row + ", column " + col + "."
                    );
                }
            }
        }

        this.grid = data;
        this.tileSize = tileSize;
        this.height = data.length;
        this.width = expectedRowLength;
    }

    /**
     * Converts a world-space coordinate pair into grid-space indices.
     * Used internally to translate continuous position data into
     * discrete tile lookups.
     *
     * @param worldX world-space x coordinate
     * @param worldY world-space y coordinate
     * @return a two-element array containing the grid column and row, in that order
     */
    private int[] worldToGrid(double worldX, double worldY) {
        int gridX = (int) (worldX / tileSize);
        int gridY = (int) (worldY / tileSize);
        return new int[]{gridX, gridY};
    }

    /**
     * Determines whether a world-space position falls inside a wall tile.
     * Coordinates outside the grid boundaries are treated as walls,
     * preventing movement past the edge of the map.
     *
     * @param x world-space x coordinate
     * @param y world-space y coordinate
     * @return true if the position is a wall or out of bounds, false if the tile is empty space
     */
    public boolean isWall(double x, double y) {
        int[] gridCoords = worldToGrid(x, y);
        int gridX = gridCoords[0];
        int gridY = gridCoords[1];

        return isWallAtGrid(gridX, gridY);
    }

    /**
     * Determines whether a grid-space cell contains a wall.
     * Indices outside the grid boundaries are treated as walls,
     * so callers stepping through the grid one cell at a time
     * (such as a DDA raycasting loop) can query this method every
     * iteration without a separate bounds check and without risking
     * an out-of-bounds exception.
     *
     * @param gridX grid-space column index
     * @param gridY grid-space row index
     * @return true if the cell is a wall or out of bounds, false if the tile is empty space
     */
    public boolean isWallAtGrid(int gridX, int gridY) {
        if (gridX < 0 || gridX >= width || gridY < 0 || gridY >= height) {
            return true;
        }

        return grid[gridY][gridX] >= 1;
    }

    /**
     * Retrieves the tile value at a given grid-space position.
     * Callers are expected to supply indices already validated as
     * in-bounds — for example, by confirming the cell is a wall via
     * isWallAtGrid before reading its type — since raycasting steps
     * along the grid one cell at a time.
     *
     * @param x grid-space column index
     * @param y grid-space row index
     * @return tile value at the given position; 0 for empty space, 1 or greater for wall type
     * @throws ArrayIndexOutOfBoundsException if x or y falls outside the grid
     */
    public int getTile(int x, int y) {
        return grid[y][x];
    }

    /**
     * Returns the size of a single tile in world units.
     *
     * @return tile size
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * Returns the number of columns in the grid.
     *
     * @return grid width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows in the grid.
     *
     * @return grid height
     */
    public int getHeight() {
        return height;
    }
}