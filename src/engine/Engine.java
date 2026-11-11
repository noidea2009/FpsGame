        package engine;

import graphics.RaycasterRenderer;
import input.InputHandler;
import map.WorldMap;
import player.Player;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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
    private int lastWidth = -1;
    private int lastHeight = -1;

    private final Canvas canvas;

    private BufferStrategy bufferStrategy;

    private boolean running;
    private boolean paused;

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

    /** Regex matching one or more whitespace characters (spaces, tabs) separating tile values. */
    private static final Pattern TOKEN_DELIMITER = Pattern.compile("\\s+");

    /** Prefix marking a map file line as a comment to be skipped. */
    private static final String COMMENT_PREFIX = "#";

    /** Byte order mark that some editors prepend to UTF-8 files. */
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    /** Filesystem path of the map file loaded at startup. */
    private static final String MAP_FILE_PATH = "maps/level1.txt";

    /** Grid of tile values parsed from the map file; 0 is empty space, 1 or greater is a wall type. */
    private int[][] mapGrid;

    public Engine(Canvas canvas) {
        this.canvas = canvas;
        this.running = false;
        this.paused=false;
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
    private void togglePause() {

        paused = !paused;

        if (paused) {
            releaseMouse();
        } else {
            canvas.requestFocus();
            captureMouse();
        }
    }

    /**
     * Reads a map file and stores the parsed tile values in {@link #mapGrid}.
     * Each non-blank, non-comment line becomes one grid row. Row-length
     * consistency and negative tile values are validated later by
     * {@link WorldMap#loadMap(int[][], int)}.
     *
     * @param path location of the map text file
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if a token is not an integer or the file contains no rows
     */
    private void loadMapGrid(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<int[]> rows = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).replace(BYTE_ORDER_MARK, "").strip();
             // Blank lines and comment lines carry no tile data.
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                continue;
            }
            rows.add(parseRow(line, i + 1));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Map file contains no grid rows: " + path);
        }
        mapGrid = rows.toArray(new int[0][]);
    }
    /**
     * Splits a single map file line on whitespace and converts each token
     * to an integer tile value.
     *
     * @param line       trimmed, non-empty line of the map file
     * @param lineNumber one-based line number in the file, used in error messages
     * @return the tile values of the row, in column order
     * @throws IllegalArgumentException if any token is not a valid integer
     */
    private int[] parseRow(String line, int lineNumber) {
        String[] tokens = TOKEN_DELIMITER.split(line);
        int[] row = new int[tokens.length];

        for (int col = 0; col < tokens.length; col++) {
            try {
                row[col] = Integer.parseInt(tokens[col]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid tile value '" + tokens[col] + "' at line " + lineNumber
                                        + ", column " + (col + 1) + ".", e);
                    }
                }
        return row;
    }
    /**
     * Creates the test world, player, renderer and input handler.
     */
    private void initTestScene() {
        try {
            loadMapGrid(Path.of(MAP_FILE_PATH));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read map file: " + MAP_FILE_PATH, e);
        }

        map = new WorldMap();
        map.loadMap(mapGrid, TEST_TILE_SIZE);


        double startX = 5.5 * TEST_TILE_SIZE;
        double startY = 5.5 * TEST_TILE_SIZE;

        player = new Player(
                startX,
                startY,
                0.0,
                200.0,
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
        paused = false;

        captureMouse();

        fpsSampleStart = System.nanoTime();
        framesThisSecond = 0;

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
                    (currentTime - previousTime)
                            / 1_000_000_000.0;

            previousTime = currentTime;

            deltaTime =
                    Math.min(
                            deltaTime,
                            MAX_DELTA_TIME
                    );

            handleInput();

            update(deltaTime);

            render();

            if (mouseCaptured && !paused) {
                recenterMouse();
            }

            updateFPS();

            long frameTime =
                    System.nanoTime() - currentTime;

            long remainingNanos =
                    targetFrameTime - frameTime;

            if (remainingNanos >
                    SLEEP_PRECISION_MARGIN_NANOS) {

                try {

                    long sleepMillis =
                            (remainingNanos -
                                    SLEEP_PRECISION_MARGIN_NANOS)
                                    / 1_000_000L;

                    if (sleepMillis > 0) {
                        Thread.sleep(sleepMillis);
                    }

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    stop();
                }
            }
        }
    }

            private void handleInput() {

        if (input.wasKeyPressed(KeyEvent.VK_ESCAPE)) {
            togglePause();
        }
    }

    /**
     * Updates all game objects.
     */
    private void update(double deltaTime) {

        if (paused) {
            return;
        }

        player.update(
                map,
                input,
                deltaTime
        );
    }

    private void renderPauseOverlay(Graphics2D graphics) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        graphics.setColor(
                new Color(0, 0, 0, 140)
        );

        graphics.fillRect(
                0,
                0,
                width,
                height
        );

        graphics.setColor(Color.WHITE);

        graphics.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        Math.max(24, width / 25)
                )
        );

        String text = "PAUSED";

        int textWidth =
                graphics.getFontMetrics()
                        .stringWidth(text);

        int textHeight =
                graphics.getFontMetrics()
                        .getAscent();

        graphics.drawString(
                text,
                (width - textWidth) / 2,
                (height + textHeight) / 2
        );
    }

    /**
     * Renders the current frame.
     */
    private void render() {

        if (bufferStrategy == null) {
            return;
        }

        updateRendererSize();

        Graphics2D graphics =
                (Graphics2D) bufferStrategy.getDrawGraphics();

        try {

            graphics.setColor(Color.BLACK);

            graphics.fillRect(
                    0,
                    0,
                    canvas.getWidth(),
                    canvas.getHeight()
            );

            renderer.render(
                    graphics,
                    player,
                    map
            );

            if (paused) {
                renderPauseOverlay(graphics);
            }

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

    private void updateRendererSize() {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (width == lastWidth && height == lastHeight) {
            return;
        }

        renderer.resize(width, height);

        lastWidth = width;
        lastHeight = height;
    }

    public int getFPS() {
        return fps;
    }
}

