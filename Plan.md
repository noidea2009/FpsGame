
# Plan.md — Java Swing/AWT Tiny Raycaster FPS Project

## 1. Project Overview

This project is a lightweight first-person raycasting engine inspired by classic FPS games (e.g., Wolfenstein 3D), implemented in **Java using Swing/AWT**.

The architecture follows a clean **Object-Oriented Programming (OOP)** design, separating responsibilities into core engine systems:

* Game loop management
* Rendering (raycasting engine)
* World/map representation
* Player state & movement
* Input handling
* Collision detection
* Optional sprite/entity system

The goal is modularity, clarity, and extensibility.

---

## 2. High-Level Package Structure

```
src/
 ├── core/
 ├── engine/
 ├── graphics/
 ├── map/
 ├── player/
 ├── input/
 ├── entity/
 ├── util/
```

---

## 3. Core Architecture (OOP Design)

### Main Flow

```
Game (entry point)
  ├── Engine (game loop)
  │     ├── InputHandler
  │     ├── Player
  │     ├── WorldMap
  │     ├── RaycasterRenderer
  │     ├── EntityManager
  │
  └── JFrame + Canvas (AWT rendering surface)
```

---

## 4. Class Design

---

# 4.1 core.Game

### Responsibility:

Entry point and window initialization.

### Fields:

```java
JFrame frame;
Engine engine;
```

### Methods:

```java
void main(String[] args)
void initWindow()
```

### Ownership:

* Owns `Engine`
* Owns Swing window lifecycle

---

# 4.2 engine.Engine

### Responsibility:

Controls game loop (update + render cycle)

### Fields:

```java
Player player;
WorldMap map;
RaycasterRenderer renderer;
InputHandler input;
EntityManager entityManager;

boolean running;
int fps;
long lastFrameTime;
```

### Methods:

```java
void start()
void gameLoop()
void update(double deltaTime)
void render(Graphics2D g)
void stop()
```

### Ownership:

* Central controller
* Owns all major subsystems

---

# 4.3 player.Player

### Responsibility:

Handles player position, rotation, movement

### Fields:

```java
double x, y;              // world position
double directionAngle;    // radians
double moveSpeed;
double rotationSpeed;
```

### Methods:

```java
void update(WorldMap map, InputHandler input, double dt)
void moveForward(WorldMap map)
void moveBackward(WorldMap map)
void strafeLeft(WorldMap map)
void strafeRight(WorldMap map)
void rotate(double deltaAngle)
```

### Data Types:

* Position: `double` (precision for smooth movement)
* Angle: `double` (radians)

### Ownership:

* Owned by `Engine`
* Independent state holder



---

# 4.4 map.WorldMap

### Responsibility:

Stores 2D grid-based world and exposes read-only spatial queries to dependent systems.

### Fields:

```java
int[][] grid;
int tileSize;
int width, height;
```

### Methods:

```java
boolean isWall(double x, double y)
int getTile(int x, int y)
int getTileSize()
int getWidth()
int getHeight()
void loadMap(int[][] data)
```

### Method Signatures & Visibility:

| Method | Visibility | Parameters | Returns | Used By |
|---|---|---|---|---|
| `isWall` | `public` | world-space `x`, `y` (doubles) | `boolean` | Player, Collision |
| `getTile` | `public` | grid-space `x`, `y` (ints) | `int` (tile/wall type) | Raycaster |
| `getTileSize` | `public` | none | `int` | Renderer, Physics |
| `getWidth` | `public` | none | `int` | Minimap, Renderer |
| `getHeight` | `public` | none | `int` | Minimap, Renderer |
| `loadMap` | `public` | `int[][] data` | `void` | Game/Engine (initialization) |

### Internal Helper (private):

```java
int[] worldToGrid(double worldX, double worldY)
```
Converts world-space coordinates into grid indices. Required because `isWall` now takes world coordinates directly (per the revised ownership table) while the underlying `grid` array is indexed by integer tile position. This conversion is not part of the public contract — no external class in the ownership table needs raw grid-index conversion, only the final boolean/tile result.

### Data Types:

* Map grid: `int[][]`
    * 0 = empty space
    * 1+ = wall types
* Spatial queries: `isWall` takes `double` (world units) since Player/Collision operate in continuous space; `getTile` takes `int` (grid indices) since Raycaster works per-cell after DDA stepping.

### Ownership:

* Owned by `Engine`
* Shared read-only resource — public query surface now explicitly split by coordinate space (world vs. grid) to match how each consumer operates.

---


# 4.5 graphics.RaycasterRenderer

### Responsibility:

Performs raycasting and draws 3D projection

### Fields:

```java
int screenWidth;
int screenHeight;
double fov;

Texture[] textures;
```

### Methods:

```java
void render(Graphics2D g, Player player, WorldMap map)
void castRay(Player player, WorldMap map, double rayAngle)
double calculateDistance(double x1, double y1, double x2, double y2)
```

### Data Types:

* Rays: computed per column (loop-based, no persistent object required unless optimized)
* Distances: `double`

### Ownership:

* Owned by `Engine`
* Stateless renderer (preferred)

---

# 4.6 input.InputHandler

### Responsibility:

Handles keyboard input state

### Fields:

```java
boolean[] keys;
```

### Key Mapping:

```java
W, A, S, D → movement
Left / Right arrows → rotation
Shift → sprint (optional)
```

### Methods:

```java
void keyPressed(KeyEvent e)
void keyReleased(KeyEvent e)
boolean isKeyDown(int keyCode)
```

### Data Types:

* Input state: `boolean[]` indexed by keyCode

### Ownership:

* Owned by `Engine`
* Shared read-only by Player

---

# 4.7 entity.EntityManager

### Responsibility:

Manages enemies, pickups, and sprites

### Fields:

```java
List<Entity> entities;
```

### Methods:

```java
void update(double dt)
void render(Graphics2D g, Player player)
void addEntity(Entity e)
void removeEntity(Entity e)
```

### Ownership:

* Owned by `Engine`
* Owns multiple `Entity`

---

# 4.8 entity.Entity (Abstract)

### Responsibility:

Base class for all world objects

### Fields:

```java
double x, y;
boolean active;
```

### Methods:

```java
void update(double dt)
void render(Graphics2D g)
double distanceTo(Player player)
```

### Subclasses:

* `Enemy`
* `Pickup`
* `DecorationSprite`

---

# 4.9 graphics.Texture

### Responsibility:

Stores wall/sprite textures

### Fields:

```java
int width, height;
int[] pixels; // RGB packed ints
```

### Methods:

```java
int getPixel(int x, int y)
```

### Data Types:

* Pixel data: `int[]` (fast rendering)

### Ownership:

* Loaded once by engine or resource loader
* Shared immutable asset

---

# 4.10 util.Vector2D (Optional)

### Responsibility:

Reusable math utility

### Fields:

```java
double x, y;
```

### Methods:

```java
double length()
double distance(Vector2D other)
Vector2D normalize()
```

---

## 5. Game Loop Model

### Fixed Update Structure:

```
while (running):
    deltaTime = time since last frame

    input.poll()
    player.update()
    entityManager.update()
    
    render frame (RaycasterRenderer)
```

### Timing Types:

* `double deltaTime` → smooth movement
* `long nanoTime` → FPS control

---

## 6. Data Ownership Model

| Component     | Owns             | Uses              |
| ------------- | ---------------- | ----------------- |
| Game          | Engine           | JFrame            |
| Engine        | All systems      | Shared subsystems |
| Player        | Position + state |WorldMap (isWall), InputHandler     |
| WorldMap      | Grid data        |Renderer (getTile, getTileSize), Player/Collision (isWall), Raycaster (getTile), Physics (getTileSize), Minimap (getWidth, getHeight)  |
| Renderer      | None (stateless) | Player, Map       |
| EntityManager | Entities list    | Player            |
| InputHandler  | Key state        | Player            |
| Minimap  | None (stateless)	        | WorldMap (getWidth, getHeight)            |

---

## 7. Design Principles

### ✔ OOP Principles Used

* Encapsulation (each system owns its state)
* Single Responsibility Principle (each class has one job)
* Loose coupling (Renderer does not mutate world state)
* High cohesion (Player handles only movement logic)

---

## 8. Suggested Future Extensions

* Sprite-based enemies with AI
* Door systems (animated walls)
* Lighting & shading gradients
* Minimap overlay (Swing HUD)
* Sound system (Java Clip API)
* Level loader (JSON or TXT maps)

