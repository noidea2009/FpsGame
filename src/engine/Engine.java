        package engine;

import graphics.RaycasterRenderer;
import input.InputHandler;
import map.WorldMap;
import player.Player;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.IllegalComponentStateException;
/**
 * Main game engine.
 *
 * Handles:
 * - Game initialization
 * - Input
 * - Update loop
 * - Rendering
 * - Frame timing
 * - Program shutdown
 */
public class Engine {

    private static final double MAX_DELTA_TIME = 0.25;
    private static final long SLEEP_PRECISION_MARGIN_NANOS = 2_000_000L;

    private static final int BUFFER_COUNT = 2;
    private static final int MAX_BUFFER_RETRIES = 3;

    private static final int TEST_TILE_SIZE = 64;
    private static final int TARGET_FPS = 60;
    private int lastWidth;
    private int lastHeight;

    private final Canvas canvas;

    private BufferStrategy bufferStrategy;

    private boolean running;

    private int fps;
    private long fpsSampleStart;
    private int framesThisSecond;

    private WorldMap map;
    private Player player;
    private RaycasterRenderer renderer;
    private InputHandler input;

    private Robot mouseRobot;
    private boolean mouseCaptured;
    private Cursor invisibleCursor;

    public Engine(Canvas canvas) {
        this.canvas = canvas;
        this.running = false;
        this.fps = TARGET_FPS;

        initTestScene();

        try {
            mouseRobot = new Robot();

            BufferedImage cursorImage =
                    new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

            invisibleCursor = Toolkit.getDefaultToolkit()
                    .createCustomCursor(
                            cursorImage,
                            new Point(0, 0),
                            "invisibleCursor"
                    );

        } catch (Exception e) {
            mouseRobot = null;
            invisibleCursor = null;

            System.err.println(
                    "FPS mouse capture unavailable: " + e.getMessage()
            );
        }
    }

    /**
     * Creates the test world, player, renderer and input handler.
     */
    private void initTestScene() {

        int[][] grid = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 2, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 2, 2, 0, 0, 0, 0, 0, 1},
                {1, 0, 2, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 3, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 3, 0, 0, 1},
                {1, 0, 0, 0, 0, 3, 3, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        map = new WorldMap();
        map.loadMap(grid, TEST_TILE_SIZE);

        double startX = 5.5 * TEST_TILE_SIZE;
        double startY = 5.5 * TEST_TILE_SIZE;

        player = new Player(
                startX,
                startY,
                0.0,
                100.0,
                1.5
        );

        renderer = new RaycasterRenderer(
                800,
                600,
                Math.toRadians(60.0)
        );

        input = new InputHandler();

        /*
         * Canvas is not a Swing JComponent, so the InputHandler must
         * use its AWT KeyListener implementation.
         */
        input.bindDefaultKeys(canvas);
        input.bindMouseInput(canvas);
    }

    /**
     * Starts the game.
     */
    public void start() {

        if (running) {
            return;
        }

        canvas.setFocusable(true);
        canvas.requestFocus();

        createBufferStrategy();

        running = true;

        captureMouse();

        fpsSampleStart = System.nanoTime();
        framesThisSecond = 0;
        lastWidth = canvas.getWidth();
        lastHeight = canvas.getHeight();

        renderer.resize(lastWidth, lastHeight);

        gameLoop();

        releaseMouse();
    }

    /**
     * Stops the game.
     */
    public void stop() {
        running = false;
    }

    /**
     * Creates the rendering buffer.
     */
    private void createBufferStrategy() {

        int attempts = 0;

        while (bufferStrategy == null && attempts < MAX_BUFFER_RETRIES) {

            try {
                canvas.createBufferStrategy(BUFFER_COUNT);
                bufferStrategy = canvas.getBufferStrategy();

            } catch (IllegalStateException e) {
                attempts++;

                try {
                    Thread.sleep(10);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (bufferStrategy == null) {
            throw new IllegalStateException(
                    "Unable to create BufferStrategy."
            );
        }
    }

    /**
     * Main game loop.
     */
    private void gameLoop() {

        long previousTime = System.nanoTime();

        final long targetFrameTime =
                1_000_000_000L / TARGET_FPS;

        while (running) {

            long currentTime = System.nanoTime();

            double deltaTime =
                    (currentTime - previousTime) / 1_000_000_000.0;

            previousTime = currentTime;

            /*
             * Prevent a long pause from causing the player to
             * teleport when the game resumes.
             */
            deltaTime = Math.min(deltaTime, MAX_DELTA_TIME);

            update(deltaTime);
            render();

            if (mouseCaptured) {
                recenterMouse();
            }

            updateFPS();
            /*
             * Cap the game at approximately 60 FPS.
             */
            long frameTime =
                    System.nanoTime() - currentTime;

            long remainingNanos =
                    targetFrameTime - frameTime;

            if (remainingNanos > SLEEP_PRECISION_MARGIN_NANOS) {

                try {
                    long sleepMillis =
                            (remainingNanos - SLEEP_PRECISION_MARGIN_NANOS)
                                    / 1_000_000L;

                    if (sleepMillis > 0) {
                        Thread.sleep(sleepMillis);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop();
                }
            }

            /*
             * ESCAPE is handled here rather than relying on
             * Swing's ActionMap. Canvas uses AWT input.
             * need to add resume function
             */
            if (input.isKeyDown(KeyEvent.VK_ESCAPE)) {
                stop();
            }
        }
    }

    /**
     * Updates all game objects.
     */
    private void update(double deltaTime) {

        player.update(
                map,
                input,
                deltaTime
        );
    }

    /**
     * Renders the current frame.
     */
    private void render() {

        if (bufferStrategy == null) {
            return;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (width > 0 && height > 0 &&
                (width != lastWidth || height != lastHeight)) {

            renderer.resize(width, height);

            lastWidth = width;
            lastHeight = height;
        }

        Graphics2D graphics =
                (Graphics2D) bufferStrategy.getDrawGraphics();

        try {

            graphics.setColor(Color.BLACK);

            graphics.fillRect(
                    0,
                    0,
                    width,
                    height
            );

            renderer.render(
                    graphics,
                    player,
                    map
            );

        } finally {
            graphics.dispose();
        }

        bufferStrategy.show();
    }

    /**
     * Captures the mouse and hides the cursor.
     */
    private void captureMouse() {

        if (mouseRobot == null || mouseCaptured) {
            return;
        }

        try {
            canvas.setCursor(invisibleCursor);

            recenterMouse();

            mouseCaptured = true;

        } catch (Exception e) {
            System.err.println(
                    "Unable to capture mouse: " + e.getMessage()
            );
        }
    }

    /**
     * Releases the mouse and restores the normal cursor.
     */
    private void releaseMouse() {

        mouseCaptured = false;

        canvas.setCursor(Cursor.getDefaultCursor());

        if (input != null) {
            input.resetMousePosition();
        }
    }

    /**
     * Moves the OS cursor back to the center of the game window.
     *
     * The generated mouse event is deliberately ignored by
     * InputHandler so it does not rotate the camera.
     */
    private void recenterMouse() {

        if (mouseRobot == null || !canvas.isShowing()) {
            return;
        }

        try {

            Point location = canvas.getLocationOnScreen();

            int centerX =
                    location.x + canvas.getWidth() / 2;

            int centerY =
                    location.y + canvas.getHeight() / 2;

            input.ignoreNextMouseMovement();

            mouseRobot.mouseMove(centerX, centerY);

        } catch (IllegalComponentStateException e) {
            // Canvas is not currently attached to a visible window.
        }
    }
    /**
     * Calculates FPS once per second.
     */
    private void updateFPS() {

        framesThisSecond++;

        long currentTime = System.nanoTime();

        if (currentTime - fpsSampleStart >= 1_000_000_000L) {

            fps = framesThisSecond;

            framesThisSecond = 0;
            fpsSampleStart = currentTime;
        }
    }

    public int getFPS() {
        return fps;
    }
}

