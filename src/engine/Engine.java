package engine;
/**
 * Responsibility:
 * Controls game loop (update + render cycle)
 *
 * Fields:
 * Player player;
 * WorldMap map;
 * RaycasterRenderer renderer;
 * InputHandler input;
 * EntityManager entityManager;
 *
 * boolean running;
 * int fps;
 * long lastFrameTime;
 *
 * Methods:
 * void start()
 * void gameLoop()
 * void update(double deltaTime)
 * void render(Graphics2D g)
 * void stop()
 * Ownership:
 * Central controller
 * Owns all major subsystems
 *
 * Fixed Update Structure:
 *  while (running):
 *  deltaTime = time since last frame
 *
 *  input.poll()
 *  player.update()
 *  entityManager.update()
 *
 *  render frame (RaycasterRenderer)
 *  Timing Types:
 *  double deltaTime → smooth movement
 *  long nanoTime → FPS control
 */

public class Engine {
}
