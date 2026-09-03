package engine;

import graphics.RaycasterRenderer;
import map.WorldMap;
import player.Player;

import java.awt.*;

/**
 * Core game engine managing the game loop, update/render cycle, and subsystem coordination.
 */
public class Engine {
    private Canvas canvas;
    private boolean running;
    private int fps;
    private int currentFps;

    private WorldMap map;
    private Player player;
    private RaycasterRenderer renderer;

    /** Grid tile size, in world units, used by the hardcoded test map. */
    private static final int TEST_TILE_SIZE = 64;

    /** Rotation speed, in radians per second, applied automatically for visual raycast verification. */
    private static final double TEST_AUTO_ROTATE_SPEED = 0.4;

    /**
     * Constructs the engine with a rendering canvas.
     *
     * @param canvas the AWT Canvas for rendering output
     */
    public Engine(Canvas canvas) {
        this.canvas = canvas;
        this.running = false;
        this.fps = 60;

        initTestScene();
    }

    /**
     * Builds a hardcoded test map, a player placed in open space, and a
     * RaycasterRenderer sized to the canvas. Exists to give castRay() a
     * known, inspectable scene to verify against before a real level
     * loader or InputHandler wiring is in place.
     */
    private void initTestScene() {
        int[][] testGrid = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 2, 2, 0, 0, 0, 0, 0, 1},
                {1, 0, 2, 0, 0, 0, 3, 3, 0, 1},
                {1, 0, 0, 0, 0, 0, 3, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 1, 1, 0, 0, 0, 0, 1},
                {1, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        map = new WorldMap();
        map.loadMap(testGrid, TEST_TILE_SIZE);

        double startX = (5 + 0.5) * TEST_TILE_SIZE;
        double startY = (5 + 0.5) * TEST_TILE_SIZE;
        player = new Player(startX, startY, 0.0, 100.0, 1.5);

        int screenWidth = 800;
        int screenHeight = 600;
        double fov = Math.toRadians(60);
        renderer = new RaycasterRenderer(screenWidth, screenHeight, fov);
    }

    /**
     * Starts the game loop.
     */
    public void start() {
        running = true;
        gameLoop();
    }

    /**
     * Stops the game loop.
     */
    public void stop() {
        running = false;
    }

    /**
     * Main game loop: update and render cycle with delta-time calculation.
     */
    private void gameLoop() {
        long lastFrameTime = System.nanoTime();
        final long frameDuration = 1_000_000_000L / fps; // nanoseconds per frame

        while (running) {
            long currentTime = System.nanoTime();
            long deltaTimeNano = currentTime - lastFrameTime;
            double deltaTime = deltaTimeNano / 1_000_000_000.0; // convert to seconds

            update(deltaTime);
            render();

            // Frame rate limiter
            long elapsedTime = System.nanoTime() - currentTime;
            long sleepTime = frameDuration - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1_000_000); // convert to milliseconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            lastFrameTime = currentTime;
        }
    }

    /**
     * Updates game state based on delta time. The player is rotated
     * automatically at a fixed rate so the raycast projection sweeps
     * across the test map, making castRay() behavior at different
     * angles and corners visible without InputHandler wiring.
     *
     * @param deltaTime elapsed time since last frame in seconds
     */
    private void update(double deltaTime) {
        currentFps = (int) (1.0 / deltaTime);
        // Print FPS every 60 frames (~1 second at 60 FPS)
        if (System.nanoTime() % 60 == 0) {
            System.out.println("FPS: " + currentFps);
        }

        player.rotate(TEST_AUTO_ROTATE_SPEED * deltaTime);
    }

    /**
     * Renders the current frame to the canvas: clears the frame, then
     * draws the raycast projection of the test map from the player's
     * current position and facing direction.
     */
    private void render() {
        Graphics2D g = (Graphics2D) canvas.getGraphics();

        if (g == null) return; // Canvas not ready

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        renderer.render(g, player, map);

        g.dispose();
    }
}