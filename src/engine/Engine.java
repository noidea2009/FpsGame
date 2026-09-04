package engine;

import graphics.RaycasterRenderer;
import input.InputHandler;
import map.WorldMap;
import player.Player;

import java.awt.*;
import java.awt.image.BufferStrategy;

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

    /** Number of buffers requested for the canvas's BufferStrategy.
     *  Two buffers (double buffering) is standard for eliminating tearing
     *  without the added input latency of triple buffering. */
    private static final int BUFFER_COUNT = 2;

    /** Maximum redraw attempts per frame if the back buffer repeatedly
     *  reports lost contents. Bounds render() so a persistently lost
     *  buffer (e.g. minimized window) degrades a frame instead of
     *  hanging the game loop indefinitely. */
    private static final int MAX_BUFFER_RETRIES = 3;

    private Canvas canvas;
    private BufferStrategy bufferStrategy;
    private boolean running;
    private int fps;

    private long fpsSampleWindowStart;
    private int framesSinceSample;

    private WorldMap map;
    private Player player;
    private RaycasterRenderer renderer;
    private InputHandler input;

    /** Grid tile size, in world units, used by the hardcoded test map. */
    private static final int TEST_TILE_SIZE = 64;

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
     * Builds a hardcoded test map, a player placed in open space, a
     * RaycasterRenderer sized to the canvas, and an InputHandler bound to
     * the canvas for mouse-driven strafing and click logging. Exists to
     * give castRay() a known, inspectable scene to verify against before a
     * real level loader is in place.
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

        input = new InputHandler();
        input.bindDefaultKeys(canvas);
        input.bindMouseInput(canvas);
    }

    /**
     * Requests a BufferStrategy on the canvas for double-buffered rendering.
     * Must run after the canvas is added to a visible, displayable window;
     * creation is retried in a short loop since the underlying native
     * peer resources are not always available on the first attempt.
     */
    private void initBufferStrategy() {
        canvas.createBufferStrategy(BUFFER_COUNT);
        bufferStrategy = canvas.getBufferStrategy();

        // BufferStrategy creation can silently fail to attach on the first
        // call immediately after the peer becomes displayable; retrying
        // guards against a null strategy reaching the render loop.
        int attempts = 0;
        while (bufferStrategy == null && attempts < 10) {
            canvas.createBufferStrategy(BUFFER_COUNT);
            bufferStrategy = canvas.getBufferStrategy();
            attempts++;
        }

        if (bufferStrategy == null) {
            throw new IllegalStateException(
                    "Failed to create BufferStrategy; canvas may not be displayable yet."
            );
        }
    }

    /**
     * Starts the game loop.
     */
    public void start() {
        initBufferStrategy();

        canvas.setFocusable(true);
        canvas.requestFocusInWindow();

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
     * Updates game state for the frame by delegating to the player, which
     * polls the InputHandler for key state and accumulated mouse movement
     * and applies the corresponding movement, rotation, and strafe.
     *
     * @param deltaTime elapsed time since last frame in seconds, clamped
     *                  to {@link #MAX_DELTA_TIME}
     */
    private void update(double deltaTime) {
        player.update(map, input, deltaTime);
    }

    /**
     * Renders the current frame using the canvas's BufferStrategy: draws
     * the cleared frame and raycast projection to an off-screen back
     * buffer, then presents it in a single show() call. Presenting a
     * fully-drawn buffer at once, instead of drawing incrementally to
     * the on-screen graphics context, eliminates the tearing and flicker
     * that direct Canvas.getGraphics() rendering is prone to.
     *
     * Draw and show are retried together, bounded by MAX_BUFFER_RETRIES,
     * since a buffer reported as lost must be redrawn before it can be
     * shown again; retrying show() alone on undrawn contents can loop
     * indefinitely if the buffer keeps reporting as lost.
     */
    private void render() {
        int attempts = 0;

        do {
            Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();

            try {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                renderer.render(g, player, map);
            } finally {
                g.dispose();
            }

            bufferStrategy.show();
            attempts++;
        } while (bufferStrategy.contentsLost() && attempts < MAX_BUFFER_RETRIES);

        Toolkit.getDefaultToolkit().sync();
    }
}