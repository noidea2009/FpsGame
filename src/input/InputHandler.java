package input;

/**
 * Responsibility:
 * Handles keyboard input state
 *
 * Fields:
 * boolean[] keys;
 * Key Mapping:
 * W, A, S, D → movement
 * Left / Right arrows → rotation
 * Shift → sprint (optional)
 *
 * Methods:
 * void keyPressed(KeyEvent e)
 * void keyReleased(KeyEvent e)
 * boolean isKeyDown(int keyCode)
 *
 * Data Types:
 * Input state: boolean[] indexed by keyCode
 * Ownership:
 * Owned by Engine
 * Shared read-only by Player
 */
public class InputHandler {
}
