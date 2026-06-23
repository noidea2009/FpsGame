package player;

/**
 * Responsibility:
 * Handles player position, rotation, movement
 *
 * Fields:
 * double x, y;              // world position
 * double directionAngle;    // radians
 * double moveSpeed;
 * double rotationSpeed;
 *
 * Methods:
 * void update(WorldMap map, InputHandler input, double dt)
 * void moveForward(WorldMap map)
 * void moveBackward(WorldMap map)
 * void strafeLeft(WorldMap map)
 * void strafeRight(WorldMap map)
 * void rotate(double deltaAngle)
 * Data Types:
 * Position: double (precision for smooth movement)
 * Angle: double (radians)
 *
 * Ownership:
 * Owned by Engine
 * Independent state holder
 */
public class Player {
}
