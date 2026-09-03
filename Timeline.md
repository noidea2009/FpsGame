# Timeline.md — 6-Month Development Plan
## Java Swing/AWT Tiny Raycaster FPS

**Total Availability:** ~180 hours (1 hour/day × 6 months)  
**Monthly Budget:** ~20-25 hours/month  
**Target:** Textured walls, smooth 60 FPS movement, single-type swarm enemies, tested & documented

---

## Month 1: Foundation & Core Engine (Weeks 1–4)

**Goal:** Playable movement loop with no rendering yet  
**Time Budget:** 20 hours

### Week 1–2: Project Setup + Engine Loop (8–10 hours)

- [ ] Repository structure + build setup (Maven/Gradle)
- [ ] `core.Game` class: JFrame window initialization (800×600, 60 FPS)
- [ ] `engine.Engine` class skeleton:
  - Game loop with delta-time calculation
  - FPS limiter (target 60 FPS)
  - Update/render separation
- [ ] `util.Vector2D` utility class (x, y, length, distance, normalize, rotate)
- [ ] Unit tests: Vector2D math operations
- **Deliverable:** Window opens, blank black canvas, stable 60 FPS loop confirmed

### Week 3–4: Player & Input (10–12 hours)

- [ ] `player.Player` class:
  - Position (x, y, angle in radians)
  - Movement speed + rotation speed constants
  - `moveForward()`, `moveBackward()`, `strafeLeft()`, `strafeRight()`
  - `rotate()` method
  - `update()` method that polls InputHandler
- [ ] `input.InputHandler` class:
  - Key press/release events (W, A, S, D, arrows)
  - `isKeyDown(keyCode)` query
  - No rendering yet
- [ ] Integration test: Move player in console output (print position every frame)
- **Deliverable:** Player position updates smoothly; check console output to verify movement vectors

### Testing & Documentation (2–3 hours)
- Write JUnit tests for Player movement (isolated from rendering)
- Write JavaDoc for all public methods
- Add brief README with build instructions

---

## Month 2: World Map & Collision Detection (Weeks 5–8)

**Goal:** Static map with collision, no rendering yet  
**Time Budget:** 22 hours

### Week 5–6: WorldMap & Grid System (10 hours)

- [ ] `map.WorldMap` class:
  - `int[][] grid` storing wall types (0 = empty, 1+ = wall IDs)
  - `tileSize` constant (e.g., 64 pixels per tile)
  - `getTile(x, y)` and `isWall(x, y)` methods
  - Helper: `getTileCoordinates(worldX, worldY)` to convert world coords to grid
  - Predefined test map (10×10 grid with borders + interior walls)
- [ ] Load 3 test maps (small, medium, maze-like)
- **Deliverable:** Map loads, can query wall positions

### Week 7–8: Collision Detection (12 hours)

- [ ] `player.Player` collision detection:
  - `canMoveTo(double x, double y, WorldMap map)` using circle-vs-grid collision
  - Player collision radius (e.g., 0.2 tiles)
  - Prevent movement into walls
  - Update `moveForward()`, `moveBackward()`, `strafeLeft()`, `strafeRight()` to use collision check
- [ ] Refactor: Separate collision logic into `util.CollisionDetector` (stateless utility)
- [ ] Unit tests: Player at wall boundary, diagonal movement, corner cases
- [ ] Integration test: Player walks around map without clipping
- **Deliverable:** Player bounces off walls smoothly; console output shows blocked moves

### Testing & Documentation (2–3 hours)
- JUnit tests for collision detection edge cases
- JavaDoc for WorldMap and CollisionDetector
- Document map format + test maps in README

---

## Month 3: 2D Rendering & Raycasting Foundation (Weeks 9–12)

**Goal:** See textured walls in 3D first-person view  
**Time Budget:** 23 hours

### Week 9–10: Raycasting Algorithm (12 hours)

- [ ] `graphics.RaycasterRenderer` core logic (no Swing rendering yet):
  - Screen dimensions (screenWidth, screenHeight)
  - FOV constant (e.g., 60 degrees in radians)
  - `castRay(double rayAngle, Player player, WorldMap map)` → returns RaycastHit
  - Create `util.RaycastHit` struct: distance, hitX, hitY, wallType, isVertical (for texture selection)
  - Use **DDA (Digital Differential Analyzer)** algorithm:
    - Start from player position
    - Increment along ray until hitting a wall
    - Return distance to wall + hit point
  - Test with unit tests (mocked player/map, verify distances for known configs)
- **Deliverable:** RaycastHit values correct for hand-traced test cases

### Week 11–12: Texture System & Basic Rendering (11 hours)

- [ ] `graphics.Texture` class:
  - Load from simple int[][] grid (RGB packed as 0xRRGGBB)
  - `getPixel(int x, int y)` safe access
  - Provide 3–4 pre-generated textures (checkerboard, brick, solid colors)
- [ ] `graphics.RaycasterRenderer.render()` implementation:
  - Loop over screen columns (scanlines)
  - Cast ray for each column
  - Get wall height from distance
  - Sample texture from RaycastHit
  - Draw vertical line using Graphics2D
  - Apply distance-based darkening (fog effect for visual appeal)
- [ ] Integration: Render to JFrame canvas
- [ ] Fix screen tear/stutter (may need double-buffering investigation)
- **Deliverable:** 3D textured walls visible; walk around map, walls render correctly

### Testing & Documentation (2 hours)
- Unit tests for raycasting at angles 0°, 45°, 90°
- JavaDoc for RaycasterRenderer + Texture
- Quick tuning guide in README (FOV, texture filtering, fog)

### ⚠ Risk Checkpoint
- **If rendering is laggy below 60 FPS:** Defer texture filtering optimizations to Month 5; use nearest-neighbor for now
- **If raycasting has artifacts:** Debug with vertical/horizontal line tests (axis-aligned rays first)

---

## Month 4: Entities, Enemies & Swarm AI (Weeks 13–16)

**Goal:** One enemy type with simple swarm behavior  
**Time Budget:** 22 hours

### Week 13–14: Entity System (10 hours)

- [ ] `entity.Entity` abstract base:
  - Position (x, y)
  - Active flag
  - `update(double dt)`, `render(Graphics2D g, Player player, RaycasterRenderer renderer)` (abstract)
  - `distanceTo(Player player)` helper
  - `getScreenX()` → convert world to screen position (sprite projection)
- [ ] `entity.Enemy` concrete subclass:
  - Texture (small sprite graphic, e.g., 64×64)
  - Health, speed, attack range
  - `update(double dt, Player player, WorldMap map, List<Enemy> allEnemies)` (receives swarm context)
  - `takeDamage(int dmg)`, `isAlive()`, `distanceTo(player)` helpers
- [ ] `entity.EntityManager`:
  - `List<Entity> entities`
  - `update(double dt, Player player)` → update all entities
  - `render(Graphics2D g, Player player)` → sprite rendering
  - `addEntity()`, `removeEntity()`, `getEnemiesInRange(Player, radius)`
- **Deliverable:** Can spawn enemies, they exist in world (but no movement yet)

### Week 15–16: Swarm AI & Sprite Rendering (12 hours)

- [ ] **Swarm AI Logic** in `Enemy.update()`:
  - Seek player: move towards player position (vector to player, normalize, scale by speed)
  - Separation: avoid crowding other enemies (if distance < minDistance, move away)
  - Obstacle avoidance: don't walk into walls (use `WorldMap.isWall()`)
  - Attack behavior: if distance < attackRange, deal damage (optional for scope; flag as future)
  - Wander: if player out of sight range, patrol random waypoint
  - Implementation: Priority-based (seek > separation > wander)
- [ ] **Sprite Rendering** in `RaycasterRenderer`:
  - Collect all entities from EntityManager
  - For each entity: project world position (x, y) to screen space
  - Calculate sprite scale based on distance (farther = smaller)
  - Draw sprite texture at screen position (painter's algorithm: farthest first, then player, then nearest)
  - Test with 2–3 enemies spawned at known positions
- [ ] Unit tests: Swarm separation (two enemies move apart), wall avoidance
- [ ] Integration test: 5 enemies swarm and chase player, no clipping through walls
- **Deliverable:** Enemies visible, move toward player, avoid walls + each other

### Testing & Documentation (2–3 hours)
- Unit tests for AI movement (separation, obstacle avoidance)
- JavaDoc for Entity + Enemy + swarm AI methods
- Behavior tuning guide (speed, separation distance, attack range) in README

### ⚠ Risk Checkpoint
- **If sprite rendering overlaps walls incorrectly:** Implement sorted rendering list (by distance)
- **If enemies get stuck:** Add randomized pathing or increase wall buffer distance

---

## Month 5: Integration, Optimization & Polish (Weeks 17–20)

**Goal:** Stable 60 FPS, visual polish, minor features  
**Time Budget:** 21 hours

### Week 17: Performance Optimization (7 hours)

- [ ] Profile frame rate:
  - Use JProfiler or simple timer logic to identify bottlenecks
  - Check rendering, raycasting, collision detection, AI update time
- [ ] Optimize raycasting:
  - Cache ray directions (pre-compute per column)
  - Consider early termination (ray hits wall before max distance)
  - Possibly implement grid-based ray traversal (faster wall detection)
- [ ] Optimize sprite rendering:
  - Frustum culling (don't render sprites far outside FOV)
  - Sort entities once per frame instead of per-draw
- [ ] Double-buffering check (ensure no screen tear)
- **Target:** Maintain 60 FPS with 10+ enemies on-screen
- **Deliverable:** Frame profiling report; target met or documented why

### Week 18: Visual Polish & Optional Features (8 hours)

- [ ] Ceiling/floor rendering (flat colors or simple dithering)
- [ ] Weapon viewmodel (simple hand/gun sprite in corner, optional; can skip if short on time)
- [ ] HUD overlay: FPS counter, health, minimap (simple), enemy count
- [ ] Screen flash on hit (simple color overlay when player takes damage)
- [ ] Simple particle effects or muzzle flash (optional, defer if short on time)
- [ ] Refactor texture loading into `util.TextureLoader` (prepare for external assets)
- **Deliverable:** Game feels polished; visual feedback for player actions

### Week 19: Bug Fixes & Stability (6 hours)

- [ ] Test with 20+ enemies (stress test)
- [ ] Walk through entire map end-to-end; log edge cases
- [ ] Fix floating-point precision issues (if any)
- [ ] Handle edge cases: player at map edge, ray casts outside grid, enemy death cleanup
- [ ] Ensure clean shutdown (no memory leaks)
- **Deliverable:** No crashes; stable gameplay loop

### Testing & Documentation (2–3 hours)
- Integration tests for full gameplay scenarios
- JavaDoc pass: ensure all public methods + classes have descriptions
- Performance tuning guide in README

---

## Month 6: Testing, Refactoring & Documentation (Weeks 21–24)

**Goal:** Polished, tested, well-documented codebase  
**Time Budget:** 20 hours

### Week 21: Full Testing Suite (7 hours)

- [ ] Unit tests for all core systems:
  - Player movement + collision (10 test cases)
  - Raycasting accuracy (5 test cases: axis-aligned, diagonal, corner)
  - Swarm AI behavior (5 test cases: separation, cohesion, obstacle avoidance)
  - WorldMap queries (5 test cases)
  - Entity lifecycle (5 test cases)
- [ ] Integration tests:
  - Full game loop (spawn enemies, move player, render frame) ×5 scenarios
  - Map with 3 different layouts
  - Stress test: 50 enemies on screen
- [ ] Regression tests: Confirm nothing broke from Month 5 optimizations
- **Target:** >80% code coverage on core logic (engine, player, raycasting)
- **Deliverable:** Test report + coverage metrics

### Week 22: Refactoring & Code Cleanup (6 hours)

- [ ] Extract magic numbers → constants (e.g., PLAYER_RADIUS, WALL_GRID_SIZE)
- [ ] Rename variables for clarity (e.g., `da` → `deltaAngle`)
- [ ] Remove dead code
- [ ] Consolidate duplicate logic (e.g., if raycasting + collision both iterate walls)
- [ ] Organize imports; enforce consistent formatting
- [ ] Break large methods into smaller helpers (if any method >50 lines)
- **Deliverable:** Code review checklist passed (no code smells)

### Week 23: Documentation (4 hours)

- [ ] **Architecture document** (2 pages):
  - Data flow diagram (Game → Engine → subsystems)
  - Package overview + key classes
  - How to extend (add new enemy type, new map)
- [ ] **README.md** (comprehensive):
  - Setup + build instructions
  - Controls (W/A/S/D, arrows, etc.)
  - How to edit maps
  - Known limitations + future work
  - Performance tips (FPS tuning, enemy count)
- [ ] **Javadoc HTML generation** (verify clean output)
- [ ] Inline code comments for tricky algorithms (raycasting DDA, swarm AI priority)
- **Deliverable:** docs/ folder with generated Javadoc + Architecture.md + README

### Week 24: Final Polish & Release (3 hours)

- [ ] Smoke test: Play through game 3 times, no crashes
- [ ] Final FPS check at target resolution
- [ ] Create demo (small map with enemy showcases)
- [ ] Tag release (v1.0)
- [ ] Update GitHub with final README + build instructions
- [ ] Celebrate! 🎉
- **Deliverable:** Playable, documented game ready for sharing

---

## Overall Risk Mitigation & Decision Tree

### High Risk: Raycasting Math Bugs
- **Mitigation:** Month 2 ends with collision tests (debug math early)
- **Decision:** If Month 3 Week 9 raycasting is incorrect, pivot to simple grid-based approach (no interpolation, just tile-by-tile)

### Medium Risk: Performance Below 60 FPS
- **Mitigation:** Profile at Month 5 Week 17
- **Decision:** 
  - Reduce max enemy count on-screen
  - Simplify texture filtering
  - Lower screen resolution to 640×480
  - Defer ceiling/floor rendering

### Low Risk: Scope Creep
- **Mitigation:** Scope lock after Month 1; defer features to v1.1 (sound, doors, advanced AI)
- **Decision:** Cut from each month in order: weapon viewmodel, ceiling/floor, minimap, particle effects

### Medium Risk: Swing Rendering Issues (Screen Tear)
- **Mitigation:** Investigate double-buffering in Month 1 Week 2
- **Decision:** Use BufferedImage rendering if Canvas is unstable

---

## Success Criteria (End of Month 6)

✅ **Functional Requirements:**
- [x] Smooth 60 FPS movement + rotation (no frame drops)
- [x] Textured walls rendered correctly from any angle
- [x] Single enemy type with swarm behavior (seeks, separates, avoids walls)
- [x] Can walk entire map without clipping
- [x] No crashes after 30-minute gameplay session

✅ **Code Quality:**
- [x] >80% test coverage on core modules
- [x] All public methods documented (JavaDoc)
- [x] No code duplication (DRY principle)
- [x] Clear separation of concerns (OOP)

✅ **Documentation:**
- [x] Architecture guide (how the engine works)
- [x] Setup + build instructions
- [x] How to extend (new maps, new enemy types)
- [x] Known limitations + future work

---

## Monthly Time Summary

| Month | Phase                                  | Hours | Status     |
|-------|----------------------------------------|-------|------------|
| 1     | Foundation + Core Engine Loop          | 20    | Setup      |
| 2     | World Map + Collision Detection        | 22    | Backend    |
| 3     | 2D Rendering + Raycasting              | 23    | Rendering  |
| 4     | Entities + Swarm AI                    | 22    | Gameplay   |
| 5     | Integration, Optimization, Polish      | 21    | Performance|
| 6     | Testing, Refactoring, Documentation   | 20    | Release    |
| **Total** |                                   | **128** | ✅ Done    |

**Remaining Budget:** ~52 hours across 6 months for:
- Debugging unexpected issues
- Refactoring between phases
- Learning/research on raycasting theory (if needed)
- Extended polish/iteration

---

## Suggested Modifications to Plan.md

Based on this timeline and tinyraycaster adaptation, recommend these additions/clarifications:

### A. Add `graphics.RaycastHit` Struct
```
Stores result of single ray cast:
- distance: double (world units to wall)
- hitX, hitY: double (world coordinates of wall hit)
- wallType: int (0–N, references wall texture)
- isVertical: boolean (hit a vertical grid line or horizontal?)
```

### B. Refine `graphics.RaycasterRenderer`
```
Algorithm: DDA (Digital Differential Analyzer)
- Per-column raycasting loop
- Return RaycastHit for each column
- Scale wall height: wallHeight = screenHeight / distance
- Sample texture: textureY = (hitY % tileSize) / tileSize
- Apply distance darkening for depth cue
```

### C. Add Sprite Rendering to `entity.EntityManager`
```
Render: collect all entities, sort by distance (painters algorithm)
- Project world (x,y) to screen (x_screen, scale)
- Scale sprite based on distance
- Draw texture at screen position
- Frustum cull: only render if within FOV ±90°
```

### D. Specify Enemy AI Behavior (Priority-based)
```
1. Seek: move toward player (if in sight range, e.g., 10 tiles)
2. Separate: avoid crowding other enemies (if distance < 1 tile)
3. Wander: patrol random point (if player out of range)
4. Obstacle Avoidance: don't walk into walls
```

### E. Add Collision System Detail
```
Circle-vs-Grid collision:
- Player radius: 0.2 tiles (configurable)
- Check 4 corner points around player center
- Prevent movement if any corner overlaps a wall
```

---

**Next Step:** Review this timeline. Any adjustments to scope, monthly focus, or risk tolerance? Once approved, I can create the modified Plan.md.
