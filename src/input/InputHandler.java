package input;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Made by Junjit Chang
 * Manages keyboard and mouse input state.
 * Keyboard state uses Java's Key Bindings API, tracking which keys are
 * currently held down and avoiding issues common with standard KeyListeners
 * like ghosting or stuck keys. Mouse state tracks horizontal cursor movement
 * for strafing and logs button clicks to the console.
 */
public class InputHandler {

    /** Tracks the set of currently depressed keys. */
    // HashSet chosen for O(1) add/remove/contains on key codes without duplicate entries.
    private Set<Integer> keyStates = new HashSet<>();

    /** Accumulated horizontal cursor movement, in pixels, since the last poll. */
    private double mouseDeltaX = 0.0;

    /** X coordinate of the most recently observed cursor position; null until the first mouse event arrives. */
    private Integer lastMouseX = null;

    /**
     * Binds common game keys to the provided component.
     * @param component The JComponent (usually a JPanel) to bind keys to.
     */
    public void bindDefaultKeys(JComponent component) {
        bindKey(component, "LEFT", KeyEvent.VK_LEFT);
        bindKey(component, "RIGHT", KeyEvent.VK_RIGHT);
        bindKey(component, "UP", KeyEvent.VK_UP);
        bindKey(component, "DOWN", KeyEvent.VK_DOWN);
        bindKey(component, "W", KeyEvent.VK_W);
        bindKey(component, "S", KeyEvent.VK_S);
        bindKey(component, "A", KeyEvent.VK_A);
        bindKey(component, "D", KeyEvent.VK_D);
        bindKey(component, "SPACE", KeyEvent.VK_SPACE);
        bindKey(component, "ESCAPE", KeyEvent.VK_ESCAPE);

    }

    /**
     * Maps a specific key code to pressed/released actions within the component's ActionMap.
     * @param component The target component.
     * @param key The string representation of the key.
     * @param keyCode The KeyEvent constant (e.g., KeyEvent.VK_LEFT).
     */
    private void bindKey(JComponent component, String key, int keyCode) {
        //This is important, else its a makeshift keylogger, and weird stuff happens to the memory
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        // Register the press action
        inputMap.put(KeyStroke.getKeyStroke("pressed " + key), key + "_pressed");
        actionMap.put(key + "_pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyStates.add(keyCode);
            }
        });

        // Register the release action to remove the key from the set
        inputMap.put(KeyStroke.getKeyStroke("released " + key), key + "_released");
        actionMap.put(key + "_released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyStates.remove(keyCode);
            }
        });
    }

    /**
     * Checks if a key is currently being held down.
     * @param keyCode The KeyEvent constant to check.
     * @return true if the key is in the set of pressed keys, false otherwise.
     */
    public boolean isKeyDown(int keyCode) {
        return keyStates.contains(keyCode);
    }

    /**
     * Binds mouse movement and button-click listeners to the provided component.
     * Horizontal cursor movement is accumulated for later retrieval via
     * getMouseDeltaX(). Left and right button clicks are logged to the console
     * to distinguish which button triggered the click.
     *
     * @param component the AWT or Swing component to receive mouse events
     */
    public void bindMouseInput(Component component) {
        component.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                recordMouseMovement(e.getX());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                recordMouseMovement(e.getX());
            }
        });

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    System.out.println("left click");
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    System.out.println("right click");
                }
            }
        });
    }

    /**
     * Updates the accumulated horizontal mouse delta from a newly observed
     * cursor x position. The first observed position after binding (or after
     * any gap with no prior position recorded) establishes a baseline rather
     * than contributing to the delta, avoiding a spurious jump on the initial
     * event.
     *
     * @param currentX x coordinate of the cursor, relative to the bound component
     */
    private void recordMouseMovement(int currentX) {
        if (lastMouseX != null) {
            mouseDeltaX += (currentX - lastMouseX);
        }
        lastMouseX = currentX;
    }

    /**
     * Returns the horizontal mouse movement accumulated since the previous
     * call to this method, then resets the accumulator to zero. Intended to
     * be polled once per frame so each frame consumes only the movement that
     * occurred during that frame.
     *
     * @return accumulated horizontal movement, in pixels, since the previous call; positive values indicate rightward movement
     */
    public double getMouseDeltaX() {
        double delta = mouseDeltaX;
        mouseDeltaX = 0.0;
        return delta;
    }
}