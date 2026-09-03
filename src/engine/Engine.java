package engine;

import java.awt.*;

/**
 * Core game engine managing the game loop, update/render cycle, and subsystem coordination.
 */
public class Engine {
    private Canvas canvas;
    private boolean running;
    private int fps;
    private int currentFps ;

    /**
     * Constructs the engine with a rendering canvas.
     *
     * @param canvas the AWT Canvas for rendering output
     */
    public Engine(Canvas canvas) {
        this.canvas = canvas;
        this.running = false;
        this.fps = 60;
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
     * Updates game state based on delta time.
     *
     * @param deltaTime elapsed time since last frame in seconds
     */
    private void update(double deltaTime) {
        currentFps = (int) (1.0 / deltaTime);
        // Print FPS every 60 frames (~1 second at 60 FPS)
        if (System.nanoTime() % 60 == 0) {
            System.out.println("FPS: " + currentFps);
        }
        // TODO: Update player, entities, collisions
    }

    /**
     * Renders the current frame to the canvas.
     */
    private void render() {
        Graphics2D g = (Graphics2D) canvas.getGraphics();

        if (g == null) return; // Canvas not ready

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // TODO: Render raycasted walls, entities

        g.dispose();
    }
}