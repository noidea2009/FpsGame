package core;

import engine.Engine;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point for the Tiny Raycaster FPS application.
 * Responsible for window initialization and game engine lifecycle.
 */
public class Game {

    private JFrame frame;
    private Canvas canvas;
    private Engine engine;

    /**
     * Initializes and displays the JFrame window and starts the game engine.
     */
    private void initWindow() {

        frame = new JFrame("Tiny Raycaster FPS");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create rendering surface
        canvas = new Canvas();

        canvas.setPreferredSize(
                new Dimension(800, 600)
        );

        canvas.setMinimumSize(
                new Dimension(640, 480)
        );

        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(true);

        frame.add(canvas);

        frame.pack();

        // Allow the player to resize the window.
        frame.setResizable(true);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    /**
     * Starts the game engine and begins the game loop.
     */
    private void start() {
        engine = new Engine(canvas);
        engine.start();
    }

    /**
     * Entry point for the application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {

        Game game = new Game();

        game.initWindow();
        game.start();
    }
}