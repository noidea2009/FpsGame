package graphics;

import map.WorldMap;
import player.Player;
import util.RaycastHit;
import util.Vector2D;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Performs DDA-based raycasting against a WorldMap and draws the
 * resulting first-person projection. Texture sampling is not yet
 * implemented; walls are drawn as flat, distance-shaded color strips
 * so raycasting correctness can be verified visually before texture
 * mapping is added.
 */
public class RaycasterRenderer {

    private final int screenWidth;
    private final int screenHeight;
    private final double fov;

    /** Distance, in world units, beyond which walls are drawn at minimum shade. */
    private static final double MAX_VIEW_DISTANCE = 800.0;

    /** Minimum shade multiplier applied regardless of distance, so far walls remain visible. */
    private static final double MIN_SHADE = 0.3;

    /** Shade multiplier applied to horizontal-grid-line hits, to distinguish them from vertical hits. */
    private static final double HORIZONTAL_FACE_SHADE = 0.75;

    /**
     * Constructs a renderer bound to a fixed screen resolution and field of view.
     *
     * @param screenWidth  screen width in pixels; also the number of columns raycast per frame
     * @param screenHeight screen height in pixels
     * @param fov          horizontal field of view, in radians
     */
    public RaycasterRenderer(int screenWidth, int screenHeight, double fov) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.fov = fov;
    }

    /**
     * Casts a single ray from the player's position at the given angle and
     * finds the nearest wall intersection using a DDA (Digital Differential
     * Analyzer) grid traversal.
     *
     * The traversal walks grid-space cells one boundary at a time. Distance
     * is first computed along the ray itself (Euclidean, since the ray
     * direction is a unit vector), then corrected to the perpendicular
     * distance relative to the player's facing direction to avoid fisheye
     * distortion in the projected wall height.
     *
     * @param player   the player supplying ray origin and facing direction
     * @param map      the world map queried for wall boundaries
     * @param rayAngle absolute angle of this ray, in radians
     * @return a RaycastHit describing the nearest wall intersection along the ray
     */
    public RaycastHit castRay(Player player, WorldMap map, double rayAngle) {
        int tileSize = map.getTileSize();

        double originGridX = player.getX() / tileSize;
        double originGridY = player.getY() / tileSize;

        Vector2D rayDir = Vector2D.fromAngle(rayAngle);
        double rayDirX = rayDir.getX();
        double rayDirY = rayDir.getY();

        int mapX = (int) Math.floor(originGridX);
        int mapY = (int) Math.floor(originGridY);

        double deltaDistX = (rayDirX == 0) ? Double.MAX_VALUE : Math.abs(1.0 / rayDirX);
        double deltaDistY = (rayDirY == 0) ? Double.MAX_VALUE : Math.abs(1.0 / rayDirY);

        int stepX;
        int stepY;
        double sideDistX;
        double sideDistY;

        if (rayDirX < 0) {
            stepX = -1;
            sideDistX = (originGridX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1 - originGridX) * deltaDistX;
        }

        if (rayDirY < 0) {
            stepY = -1;
            sideDistY = (originGridY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1 - originGridY) * deltaDistY;
        }

        // Upper bound on steps: a ray can cross at most width + height cells
        // before it necessarily exits the grid, at which point isWallAtGrid
        // reports a boundary hit and the loop terminates regardless.
        int maxSteps = map.getWidth() + map.getHeight() + 2;
        boolean hitVerticalLine = false;

        for (int step = 0; step < maxSteps; step++) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
                hitVerticalLine = true;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
                hitVerticalLine = false;
            }

            if (map.isWallAtGrid(mapX, mapY)) {
                break;
            }
        }

        double rawGridDistance = hitVerticalLine
                ? (mapX - originGridX + (1 - stepX) / 2.0) / rayDirX
                : (mapY - originGridY + (1 - stepY) / 2.0) / rayDirY;

        double rawWorldDistance = rawGridDistance * tileSize;

        Vector2D facingDir = Vector2D.fromAngle(player.getDirectionAngle());
        double perpendicularDistance = rawWorldDistance * rayDir.dot(facingDir);

        Vector2D origin = new Vector2D(player.getX(), player.getY());
        Vector2D hitPoint = origin.add(rayDir.scale(rawWorldDistance));

        int wallType = map.isWallAtGrid(mapX, mapY) ? map.getTile(mapX, mapY) : 0;

        return new RaycastHit(perpendicularDistance, hitPoint.getX(), hitPoint.getY(), wallType, hitVerticalLine);
    }

    /**
     * Renders one first-person frame by casting one ray per screen column
     * and drawing each result as a flat-shaded vertical strip. Serves as a
     * visual correctness check for castRay() ahead of texture sampling
     * being implemented.
     *
     * @param g      target graphics context to draw into
     * @param player the player supplying ray origin and facing direction
     * @param map    the world map raycast against
     */
    public void render(Graphics2D g, Player player, WorldMap map) {
        for (int column = 0; column < screenWidth; column++) {
            double cameraOffset = ((double) column / screenWidth) - 0.5;
            double rayAngle = player.getDirectionAngle() + cameraOffset * fov;

            RaycastHit hit = castRay(player, map, rayAngle);

            int lineHeight = computeWallLineHeight(hit.getDistance(), map.getTileSize());
            int drawStart = Math.max(0, (screenHeight - lineHeight) / 2);
            int drawEnd = Math.min(screenHeight - 1, (screenHeight + lineHeight) / 2);

            g.setColor(shadeColor(baseColorForWallType(hit.getWallType()), hit.getDistance(), hit.isVertical()));
            g.drawLine(column, drawStart, column, drawEnd);
        }
    }

    /**
     * Converts a perpendicular wall distance into a projected screen-space
     * wall height, scaled so that a wall exactly one tile away spans the
     * full screen height.
     *
     * @param perpendicularDistance corrected perpendicular distance to the wall, in world units
     * @param tileSize              size of a single grid tile in world units
     * @return projected wall height in pixels
     */
    private int computeWallLineHeight(double perpendicularDistance, int tileSize) {
        if (perpendicularDistance <= 0.0001) {
            return screenHeight;
        }
        double distanceInTiles = perpendicularDistance / tileSize;
        return (int) (screenHeight / distanceInTiles);
    }

    /**
     * Maps a wall type identifier to a placeholder base color. Serves as a
     * stand-in for texture sampling so different wall types remain visually
     * distinguishable before Texture is implemented.
     *
     * @param wallType tile value returned by WorldMap.getTile()
     * @return base color associated with the wall type
     */
    private Color baseColorForWallType(int wallType) {
        switch (wallType) {
            case 1:
                return new Color(180, 60, 60);
            case 2:
                return new Color(60, 120, 180);
            case 3:
                return new Color(90, 160, 90);
            default:
                return new Color(150, 150, 150);
        }
    }

    /**
     * Darkens a base wall color based on distance and grid-line orientation,
     * approximating depth cueing and simple directional lighting.
     *
     * @param base       unshaded base color for the wall type
     * @param distance   perpendicular distance to the wall, in world units
     * @param isVertical true if the hit occurred on a vertical grid line
     * @return shaded color for this column
     */
    private Color shadeColor(Color base, double distance, boolean isVertical) {
        double distanceShade = Math.max(MIN_SHADE, 1.0 - (distance / MAX_VIEW_DISTANCE));
        double faceShade = isVertical ? 1.0 : HORIZONTAL_FACE_SHADE;
        double shade = distanceShade * faceShade;

        int r = clampColorComponent((int) (base.getRed() * shade));
        int g2 = clampColorComponent((int) (base.getGreen() * shade));
        int b = clampColorComponent((int) (base.getBlue() * shade));

        return new Color(r, g2, b);
    }

    /**
     * Clamps a color component to the valid 0-255 range.
     *
     * @param value candidate color component value
     * @return value clamped to [0, 255]
     */
    private int clampColorComponent(int value) {
        return Math.max(0, Math.min(255, value));
    }
}