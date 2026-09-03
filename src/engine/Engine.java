package engine;

import graphics.RaycasterRenderer;
import map.WorldMap;
import player.Player;

import java.awt.*;

/**
 * Core game engine managing the game loop, update/render cycle, and subsystem coordination.
 */
public class Engine {

    /** Upper bound on a single frame's simulated time, in seconds.
     *  Prevents a stalled frame (GC pause, window drag, breakpoint, etc.)
     *  from producing a single huge deltaTime that snaps player rotation
     *  or movement forward visibly in one step. */
    private static final double MAX_DELTA_TIME = 0.25;

    /** Portion of the remaining frame budget handed to Thread.sleep, in nanoseconds.
     *  Sleep is imprecise near its target, so a margin is left for a
     *  spin-wait to close the gap with sub-millisecond accuracy. */
    private static final long SLEEP_PRECISION_MARGIN_NANOS = 2_000_000L;

    private Canvas canvas;
    private boolean running;
    private int fps;

    private long fpsSampleWindowStart;
    private int framesSinceSample;

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
     * Main game loop: runs update/render cycles at a target frame rate.
     * Frame spacing is measured with nanosecond precision, deltaTime is
     * clamped to avoid large simulation jumps after a stall, and the
     * post-frame wait combines a coarse sleep with a short spin-wait to
     * hit the frame budget more precisely than Thread.sleep alone allows.
     */
    private void gameLoop() {
        long lastFrameTime = System.nanoTime();
        final long frameDuration = 1_000_000_000L / fps; // nanoseconds per frame
        fpsSampleWindowStart = lastFrameTime;
        framesSinceSample = 0;

        while (running) {
            long frameStart = System.nanoTime();

            double deltaTime = (frameStart - lastFrameTime) / 1_000_000_000.0;
            if (deltaTime > MAX_DELTA_TIME) {
                deltaTime = MAX_DELTA_TIME;
            }
            lastFrameTime = frameStart;

            update(deltaTime);
            render();

            waitForNextFrame(frameStart, frameDuration);
            sampleFps(frameStart);
        }
    }

    /**
     * Blocks until the target frame duration has elapsed since the frame
     * started. Sleeps for the bulk of the remaining time to avoid burning
     * CPU, then spin-waits the final margin to correct for the OS
     * scheduler's coarse sleep granularity.
     *
     * @param frameStart    timestamp, in nanoseconds, when the current frame began
     * @param frameDuration target duration of a single frame, in nanoseconds
     */
    private void waitForNextFrame(long frameStart, long frameDuration) {
        long targetEnd = frameStart + frameDuration;
        long remaining = targetEnd - System.nanoTime();

        if (remaining <= 0) {
            return; // frame already over budget; proceed immediately, no catch-up sleep
        }

        long sleepNanos = remaining - SLEEP_PRECISION_MARGIN_NANOS;
        if (sleepNanos > 0) {
            try {
                Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Spin-wait closes the remaining sub-millisecond gap precisely.
        while (System.nanoTime() < targetEnd) {
            Thread.onSpinWait();
        }
    }

    /**
     * Accumulates a frame count and prints the average FPS once per
     * one-second sampling window.
     *
     * @param now current timestamp, in nanoseconds
     */
    private void sampleFps(long now) {
        framesSinceSample++;
        long elapsed = now - fpsSampleWindowStart;
        if (elapsed >= 1_000_000_000L) {
            double windowSeconds = elapsed / 1_000_000_000.0;
            System.out.println("FPS: " + Math.round(framesSinceSample / windowSeconds));
            framesSinceSample = 0;
            fpsSampleWindowStart = now;
        }
    }

    /**
     * Updates game state based on delta time. The player is rotated
     * automatically at a fixed rate so the raycast projection sweeps
     * across the test map, making castRay() behavior at different
     * angles and corners visible without InputHandler wiring.
     *
     * @param deltaTime elapsed time since last frame in seconds, clamped
     *                  to {@link #MAX_DELTA_TIME}
     */
    private void update(double deltaTime) {
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