package input;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Made by Junjit Chang
 *
 * Manages FPS-style keyboard and mouse input.
 *
 * Keyboard:
 * - Keys are tracked as "held" states.
 * - W/A/S/D are intended for movement.
 * - Arrow keys can be used for rotation.
 * - SPACE and ESCAPE are also tracked.
 *
 * Mouse:
 * - Mouse movement is treated as relative movement.
 * - mouseDeltaX represents horizontal LOOK movement.
 * - mouseDeltaY represents vertical LOOK movement.
 * - Mouse movement does NOT directly move or strafe the player.
 * - Mouse buttons are tracked as held states.
 *
 * The InputHandler only records input. Player is responsible for
 * deciding how that input affects movement and camera rotation.
 */
public class InputHandler {

    /**
     * Keys that are currently being held down.
     *
     * HashSet provides simple O(1) add/remove/contains operations
     * while preventing duplicate key entries.
     */
    private final Set<Integer> keyStates = new HashSet<>();

    /**
     * Mouse buttons that are currently being held down.
     *
     * This allows the game to poll mouse buttons in the same way
     * that it polls keyboard keys.
     */
    private final Set<Integer> mouseButtonStates = new HashSet<>();
    private boolean ignoreNextMouseMovement = false;
    /**
     * Accumulated horizontal mouse movement since the previous poll.
     *
     * Positive = mouse moved right.
     * Negative = mouse moved left.
     *
     * This is a LOOK delta, not a movement/strafe value.
     */
    private double mouseDeltaX = 0.0;

    /**
     * Accumulated vertical mouse movement since the previous poll.
     *
     * Positive = mouse moved down.
     * Negative = mouse moved up.
     *
     * This can later be used for looking up/down.
     */
    private double mouseDeltaY = 0.0;

    /**
     * Previous mouse X position.
     *
     * Used to calculate relative mouse movement.
     */
    private Integer lastMouseX = null;

    /**
     * Previous mouse Y position.
     *
     * Used to calculate relative mouse movement.
     */
    private Integer lastMouseY = null;


    /**
     * Binds the default FPS keyboard controls to a Swing component.
     *
     * Swing Key Bindings are used when the component is a JComponent.
     *
     * @param component Swing component receiving keyboard input.
     */
    public void bindDefaultKeys(JComponent component) {

        // Movement keys
        bindKey(component, "W", KeyEvent.VK_W);
        bindKey(component, "A", KeyEvent.VK_A);
        bindKey(component, "S", KeyEvent.VK_S);
        bindKey(component, "D", KeyEvent.VK_D);

        // Optional keyboard look controls
        bindKey(component, "LEFT", KeyEvent.VK_LEFT);
        bindKey(component, "RIGHT", KeyEvent.VK_RIGHT);
        bindKey(component, "UP", KeyEvent.VK_UP);
        bindKey(component, "DOWN", KeyEvent.VK_DOWN);

        // Other common FPS/game controls
        bindKey(component, "SPACE", KeyEvent.VK_SPACE);
        bindKey(component, "ESCAPE", KeyEvent.VK_ESCAPE);
    }


    /**
     * Binds the default keyboard controls to an AWT Component.
     *
     * Canvas is not a JComponent, so Swing Key Bindings cannot be
     * installed on it. In that case we use a KeyListener instead.
     *
     * The Canvas must be focusable and should request focus when
     * the game starts so that it can receive keyboard input.
     *
     * @param component AWT or Swing component receiving input.
     */
    public void bindDefaultKeys(Component component) {

        if (component instanceof JComponent) {
            bindDefaultKeys((JComponent) component);
            return;
        }

        component.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                keyStates.add(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keyStates.remove(e.getKeyCode());
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // Not used.
                //
                // FPS controls care about physical key presses,
                // not typed characters.
            }
        });

        // Canvas must be focusable to receive keyboard events.
        component.setFocusable(true);
    }


    /**
     * Binds one keyboard key using Swing's InputMap/ActionMap system.
     *
     * Key presses add the key to keyStates.
     * Key releases remove the key from keyStates.
     *
     * @param component Swing component receiving the binding.
     * @param key String representation used by KeyStroke.
     * @param keyCode KeyEvent constant representing the key.
     */
    private void bindKey(
            JComponent component,
            String key,
            int keyCode) {

        InputMap inputMap =
                component.getInputMap(
                        JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionMap actionMap = component.getActionMap();

        String pressedAction = key + "_pressed";
        String releasedAction = key + "_released";

        // Register key press.
        inputMap.put(
                KeyStroke.getKeyStroke("pressed " + key),
                pressedAction);

        actionMap.put(
                pressedAction,
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        keyStates.add(keyCode);
                    }
                });

        // Register key release.
        inputMap.put(
                KeyStroke.getKeyStroke("released " + key),
                releasedAction);

        actionMap.put(
                releasedAction,
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        keyStates.remove(keyCode);
                    }
                });
    }


    /**
     * Checks whether a keyboard key is currently held.
     *
     * This is intended to be polled by Player every game update.
     *
     * Example:
     *
     * if (input.isKeyDown(KeyEvent.VK_W)) {
     *     // Move forward
     * }
     *
     * @param keyCode KeyEvent constant.
     * @return true if the key is currently held.
     */
    public boolean isKeyDown(int keyCode) {
        return keyStates.contains(keyCode);
    }


    /**
     * Binds FPS-style mouse input.
     *
     * Mouse movement is converted into relative X/Y deltas.
     *
     * IMPORTANT:
     * Mouse movement is ONLY input for looking.
     * It does not directly strafe or move the player.
     *
     * @param component Component receiving mouse events.
     */
    public void bindMouseInput(Component component) {

        component.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                recordMouseMovement(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                recordMouseMovement(e.getX(), e.getY());
            }
        });


        component.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                mouseButtonStates.add(e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseButtonStates.remove(e.getButton());
            }
        });
    }


    /**
     * Calculates relative mouse movement.
     *
     * Unlike an ordinary desktop application, an FPS is interested
     * in HOW FAR the mouse moved rather than WHERE the cursor is.
     *
     * Example:
     *
     * Mouse moves:
     *
     *     400 -> 405 -> 410
     *
     * The player receives:
     *
     *     +5, +5
     *
     * rather than an absolute cursor position of 410.
     *
     * @param currentX current cursor X position.
     * @param currentY current cursor Y position.
     */
    private void recordMouseMovement(int currentX, int currentY) {

        if (ignoreNextMouseMovement) {
            lastMouseX = currentX;
            lastMouseY = currentY;
            ignoreNextMouseMovement = false;
            return;
        }

        if (lastMouseX != null && lastMouseY != null) {
            mouseDeltaX += currentX - lastMouseX;
            mouseDeltaY += currentY - lastMouseY;
        }

        lastMouseX = currentX;
        lastMouseY = currentY;
    }
    public void ignoreNextMouseMovement() {
        ignoreNextMouseMovement = true;
    }

    /**
     * Returns accumulated horizontal mouse movement.
     *
     * The value is consumed when read and reset to zero.
     *
     * Player should use this value to rotate the player horizontally.
     *
     * @return horizontal mouse-look delta in pixels.
     */
    public synchronized double getMouseDeltaX() {

        double delta = mouseDeltaX;

        mouseDeltaX = 0.0;

        return delta;
    }


    /**
     * Returns accumulated vertical mouse movement.
     *
     * The value is consumed when read and reset to zero.
     *
     * Player can use this value for looking up/down once
     * vertical camera rotation is implemented.
     *
     * @return vertical mouse-look delta in pixels.
     */
    public synchronized double getMouseDeltaY() {

        double delta = mouseDeltaY;

        mouseDeltaY = 0.0;

        return delta;
    }


    /**
     * Checks whether a mouse button is currently held.
     *
     * Example:
     *
     * if (input.isMouseButtonDown(MouseEvent.BUTTON1)) {
     *     // Fire weapon
     * }
     *
     * @param button MouseEvent button constant.
     * @return true if the button is currently held.
     */
    public boolean isMouseButtonDown(int button) {
        return mouseButtonStates.contains(button);
    }


    /**
     * Resets the mouse position baseline.
     *
     * This is useful when the mouse is captured/recentered by the
     * engine. The next mouse event establishes a new baseline rather
     * than causing a large unwanted camera rotation.
     */
    public synchronized void resetMousePosition() {
        lastMouseX = null;
        lastMouseY = null;
    }
}

