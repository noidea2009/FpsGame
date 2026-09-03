package player;

import input.InputHandler;
import map.WorldMap;
import java.awt.event.KeyEvent;

/**
 * Represents the player's position, facing direction, and movement state
 * within the world. Holds no rendering logic; exposes position and angle
 * for consumption by the renderer.
 */
public class Player {

    private double x;
    private double y;
    private double directionAngle;
    private final double moveSpeed;
    private final double rotationSpeed;

    /**
     * Constructs a Player with an initial position, facing angle, and
     * movement constants.
     *
     * @param startX starting world-space x coordinate
     * @param startY starting world-space y coordinate
     * @param startAngle initial facing direction, in radians
     * @param moveSpeed linear movement speed, in world units per second
     * @param rotationSpeed rotational speed, in radians per second
     */
    public Player(double startX, double startY, double startAngle,
                  double moveSpeed, double rotationSpeed) {
        this.x = startX;
        this.y = startY;
        this.directionAngle = startAngle;
        this.moveSpeed = moveSpeed;
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Polls current key state and applies the corresponding movement or
     * rotation for a single frame. Intended to be called once per game
     * loop iteration.
     *
     * @param map world map, passed through to movement methods for future
     *            collision checks
     * @param input input handler providing current key state
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void update(WorldMap map, InputHandler input, double dt) {
        if (input.isKeyDown(KeyEvent.VK_W)) {
            moveForward(map, dt);
        }
        if (input.isKeyDown(KeyEvent.VK_S)) {
            moveBackward(map, dt);
        }
        if (input.isKeyDown(KeyEvent.VK_A)) {
            strafeLeft(map, dt);
        }
        if (input.isKeyDown(KeyEvent.VK_D)) {
            strafeRight(map, dt);
        }
        if (input.isKeyDown(KeyEvent.VK_LEFT)) {
            rotate(-rotationSpeed * dt);
        }
        if (input.isKeyDown(KeyEvent.VK_RIGHT)) {
            rotate(rotationSpeed * dt);
        }
    }

    /**
     * Advances position along the current facing direction. The map
     * parameter is unused in this milestone and reserved for wall
     * collision checks introduced in a later development phase.
     *
     * @param map world map, reserved for future collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void moveForward(WorldMap map, double dt) {
        double distance = moveSpeed * dt;
        x += Math.cos(directionAngle) * distance;
        y += Math.sin(directionAngle) * distance;
    }

    /**
     * Moves position opposite to the current facing direction. The map
     * parameter is unused in this milestone and reserved for wall
     * collision checks introduced in a later development phase.
     *
     * @param map world map, reserved for future collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void moveBackward(WorldMap map, double dt) {
        double distance = moveSpeed * dt;
        x -= Math.cos(directionAngle) * distance;
        y -= Math.sin(directionAngle) * distance;
    }

    /**
     * Moves position perpendicular to the facing direction, toward the
     * left side. The map parameter is unused in this milestone and
     * reserved for wall collision checks introduced in a later
     * development phase.
     *
     * @param map world map, reserved for future collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void strafeLeft(WorldMap map, double dt) {
        double distance = moveSpeed * dt;
        double strafeAngle = directionAngle - Math.PI / 2;
        x += Math.cos(strafeAngle) * distance;
        y += Math.sin(strafeAngle) * distance;
    }

    /**
     * Moves position perpendicular to the facing direction, toward the
     * right side. The map parameter is unused in this milestone and
     * reserved for wall collision checks introduced in a later
     * development phase.
     *
     * @param map world map, reserved for future collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void strafeRight(WorldMap map, double dt) {
        double distance = moveSpeed * dt;
        double strafeAngle = directionAngle + Math.PI / 2;
        x += Math.cos(strafeAngle) * distance;
        y += Math.sin(strafeAngle) * distance;
    }

    /**
     * Adjusts the facing direction by a given delta and normalizes the
     * result to the range [0, 2π).
     *
     * @param deltaAngle change in facing direction, in radians; positive
     *                   values rotate clockwise
     */
    public void rotate(double deltaAngle) {
        directionAngle += deltaAngle;
        double twoPi = 2 * Math.PI;
        directionAngle = ((directionAngle % twoPi) + twoPi) % twoPi;
    }

    /**
     * Returns the current world-space x coordinate.
     *
     * @return x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the current world-space y coordinate.
     *
     * @return y coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the current facing direction.
     *
     * @return facing angle, in radians, within the range [0, 2π)
     */
    public double getDirectionAngle() {
        return directionAngle;
    }
}