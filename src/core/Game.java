package core;

import engine.Engine;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point for the Tiny Raycaster FPS application.
 *
 * Game owns the application window and rendering surface.
 * Engine owns the game lifecycle.
 */
public class Game {

    private static final int INITIAL_WIDTH = 800;
    private static final int INITIAL_HEIGHT = 600;

    private JFrame frame;
    private Canvas canvas;
    private Engine engine;

    /**
     * Initializes the game window.
     */
    private void initWindow() {

        frame = new JFrame("Tiny Raycaster FPS");

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        canvas = new Canvas();

        canvas.setPreferredSize(
                new Dimension(
                        INITIAL_WIDTH,
                        INITIAL_HEIGHT
                )
        );

        canvas.setMinimumSize(
                new Dimension(
                        640,
                        480
                )
        );

        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(true);

        frame.add(canvas);

        // Sizes the window around the Canvas.
        frame.pack();

        // The window is resizable by the user.
        frame.setResizable(true);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    /**
     * Creates and starts the game engine.
     */
    private void start() {

        engine = new Engine(canvas);

        engine.start();
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {

        Game game = new Game();

        game.initWindow();
        game.start();
    }
}