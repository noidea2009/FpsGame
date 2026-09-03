package util;

public class RaycastHit {
    /**
     * Immutable result of a single ray cast performed by RaycasterRenderer.
     * Stores the distance to the nearest wall intersection, the exact
     * world-space hit coordinates, the type of wall struck, and whether
     * the hit occurred on a vertical or horizontal grid line.
     */
    private final double distance;
    private final double hitX;
    private final double hitY;
    private final int wallType;
    private final boolean isVertical;

    /**
     * Constructs a completed raycast result.
     *
     * @param distance perpendicular distance from the ray origin to the wall hit point, in world units
     * @param hitX world-space x coordinate of the intersection point
     * @param hitY world-space y coordinate of the intersection point
     * @param wallType tile value at the hit position, matching WorldMap.getTile(); selects texture
     * @param isVertical true if the hit occurred on a vertical grid line, false if on a horizontal grid line
     */

    public RaycastHit(double distance, double hitX, double hitY, int wallType, boolean isVertical) {
        this.distance = distance;
        this.hitX = hitX;
        this.hitY = hitY;
        this.wallType = wallType;
        this.isVertical = isVertical;
    }

    /**
     * Returns the perpendicular distance from the ray origin to the wall hit point.
     *
     * @return distance in world units
     */

    public double getDistance(){
        return distance;
    }

    /**
     * Returns the world-space x coordinate of the intersection point.
     *
     * @return hit x coordinate
     */

    public double getHitX()
    {
        return hitX;
    }

    /**
     * Returns the world-space y coordinate of the intersection point.
     *
     * @return hit y coordinate
     */

    public double getHitY()
    {
        return hitY;
    }

    /**
     * Returns the tile value struck by the ray.
     *
     * @return wall type, matching WorldMap.getTile()
     */

    public int getWallType()
    {
        return wallType;
    }

    /**
     * Indicates which grid line orientation the ray struck.
     *
     * @return true if the hit is on a vertical grid line, false if on a horizontal grid line
     */

    public boolean isVertical()
    {
        return isVertical;
    }

}
