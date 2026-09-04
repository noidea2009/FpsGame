package player;

import input.InputHandler;
import map.WorldMap;
import util.CollisionDetector;

import java.awt.event.KeyEvent;

/**
 * Represents the player's position, facing direction, and movement state
 * within the world. Movement is checked against wall collision on each
 * axis independently, allowing motion to continue along an unblocked axis
 * even when the other axis is blocked. Holds no rendering logic; exposes
 * position and angle for consumption by the renderer.
 */
public class Player {

    private double x;
    private double y;
    private double directionAngle;
    private final double moveSpeed;
    private final double rotationSpeed;

    /** World units of rotation applied per pixel of horizontal mouse movement. */
    private static final double MOUSE_STRAFE_SENSITIVITY = 0.5;
    private static final double MOUSE_ROTATION_SENSITIVITY = 0.005;
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
     * Polls current key and mouse state and applies the corresponding
     * movement, rotation, or strafe for a single frame. Horizontal mouse
     * movement is consumed once per call and converted to an additional
     * strafe displacement, on top of any keyboard-driven strafing. Intended
     * to be called once per game loop iteration.
     *
     * @param map world map, passed through to movement methods for collision checks
     * @param input input handler providing current key state and accumulated mouse movement
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

        double mouseDeltaX = input.getMouseDeltaX();

        if (mouseDeltaX != 0.0) {
            rotate(mouseDeltaX * MOUSE_ROTATION_SENSITIVITY);
        }
    }

    /**
     * Advances position along the current facing direction. The x and y
     * components of the resulting displacement are applied independently,
     * so contact with a wall on one axis does not prevent motion on the other.
     *
     * @param map world map used for collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void moveForward(WorldMap map, double dt) {
        double distance = moveSpeed * dt;
        double deltaX = Math.cos(directionAngle) * distance;
        double deltaY = Math.sin(directionAngle) * distance;
        applyMovement(deltaX, deltaY, map);
    }

    /**
     * Moves position opposite to the current facing direction. The x and y
     * components of the resulting displacement are applied independently,
     * so contact with a wall on one axis does not prevent motion on the other.
     *
     * @param map world map used for collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void moveBackward(WorldMap map, double dt) {
        double distance = moveSpeed * dt;
        double deltaX = -Math.cos(directionAngle) * distance;
        double deltaY = -Math.sin(directionAngle) * distance;
        applyMovement(deltaX, deltaY, map);
    }

    /**
     * Moves position perpendicular to the facing direction, toward the left
     * side, at the configured move speed.
     *
     * @param map world map used for collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void strafeLeft(WorldMap map, double dt) {
        strafeByDistance(-moveSpeed * dt, map);
    }

    /**
     * Moves position perpendicular to the facing direction, toward the right
     * side, at the configured move speed.
     *
     * @param map world map used for collision checks
     * @param dt elapsed time since the previous frame, in seconds
     */
    public void strafeRight(WorldMap map, double dt) {
        strafeByDistance(moveSpeed * dt, map);
    }

    /**
     * Moves position perpendicular to the facing direction by a signed
     * distance. A positive distance strafes toward the right side of the
     * facing direction; a negative distance strafes toward the left. Shared
     * by keyboard-driven strafing (fixed magnitude, sign by key) and
     * mouse-driven strafing (magnitude and sign derived from cursor delta).
     * The x and y components of the resulting displacement are applied
     * independently through applyMovement, so contact with a wall on one
     * axis does not prevent motion on the other.
     *
     * @param distance signed strafe distance, in world units; positive is rightward, negative is leftward
     * @param map world map used for collision checks
     */
    private void strafeByDistance(double distance, WorldMap map) {
        double strafeAngle = directionAngle + Math.PI / 2;
        double deltaX = Math.cos(strafeAngle) * distance;
        double deltaY = Math.sin(strafeAngle) * distance;
        applyMovement(deltaX, deltaY, map);
    }

    /**
     * Applies a candidate displacement to the current position, checking the
     * x and y components against wall collision separately. Each axis commits
     * only if the resulting position, combined with the position on the other
     * axis at the time of the check, is free of wall overlap. This produces
     * sliding contact along walls rather than a full stop on diagonal contact.
     *
     * @param deltaX candidate change in x coordinate, in world units
     * @param deltaY candidate change in y coordinate, in world units
     * @param map world map used for collision checks
     */
    private void applyMovement(double deltaX, double deltaY, WorldMap map) {
        double candidateX = x + deltaX;
        double candidateY = y + deltaY;

        if (CollisionDetector.canMoveTo(candidateX, y, map)) {
            x = candidateX;
        }
        if (CollisionDetector.canMoveTo(x, candidateY, map)) {
            y = candidateY;
        }
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