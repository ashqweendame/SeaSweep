# Blitz Ball — Ocean Waste-Sorting Edition: Complete Build Guide

A 2D arcade sorting game: pieces of trash sink from the top of the screen, down a central lane flanked by two colored bins, toward a discard zone at the bottom. The player sorts each piece left or right into the correct bin before it reaches the discard zone, using two shifting bin labels. This document is the entire specification, start to finish — project setup, every class, every mechanic, the build order, and how to verify it's done. Nothing here requires any other document. No implementation code is included anywhere — every class description is a contract for you to implement yourself.

---

## Part 0: Governing Design Rules

Read this section first — everything else follows from it.

- There are **four possible trash categories**: `PAPER`, `PLASTIC`, `TIN`, `GLASS`.
- Only **two categories are active as bin labels** at any given moment — one on the left, one on the right. The default starting pair is `PAPER` (left) and `PLASTIC` (right). `TIN` and `GLASS` are fully built and ready to use, but nothing in this spec activates them — they simply aren't drawn from until you decide to wire them in.
- Falling items are **only ever generated as one of the two currently active types** — the game never drops a piece of trash that doesn't match either visible bin. This means every single item is always correctly sortable if the player identifies it right.
- Every item falls along **exactly one fixed horizontal position** — there is no horizontal movement or randomness at any point in the game. Only two things vary from one falling item to the next: which of the two active types it is, and how fast it's currently falling (which depends on elapsed game time, not on the item itself).
- The miss line sits at a fixed vertical position, marking the boundary between the sorting bins and the discard zone below them. An item reaching that line counts as an automatic miss (life lost), exactly the same as sorting it into the wrong bin.
- Difficulty increases over time through one mechanism only: fall speed increases by a tiny fixed amount every single frame, up to a hard cap, for the entire game — it never resets, slows down, or varies by item type.
- Every 100 points, the two active bin labels swap which side they're on (left becomes right, right becomes left) — the two categories themselves don't change, just their positions on screen.
- The game ends when lives reach zero. The score from that run is compared against a stored high score, and the stored value is only overwritten if the new score is higher.

---

## Part 1: Concrete Constants (Use These Exact Values Unless Your Assignment Specifies Otherwise)

| Constant                     | Value                          | Meaning                                                                                                                                                          |
| ---------------------------- | ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Window width                 | `500` px                       | Total playable width                                                                                                                                             |
| Window height                | `700` px                       | Total playable height                                                                                                                                            |
| Sky band                     | `y = 0` to `225`               | Full width, plain sky background                                                                                                                                 |
| Sorting-bin band             | `y = 225` to `490`             | Full width; this is where the left bin, right bin, and the middle drop lane all live                                                                             |
| Left bin                     | `x = 0–170`, `y = 225–490`     | Fixed rectangle, filled with its bin color                                                                                                                       |
| Right bin                    | `x = 330–500`, `y = 225–490`   | Fixed rectangle, filled with its bin color                                                                                                                       |
| Middle drop lane             | `x = 170–330`, `y = 225–490`   | The gap between the two bins — this is where the falling item is actually visible; not a separate drawn rectangle, just the space left uncovered by the two bins |
| Discard zone                 | `y = 490` to `700`, full width | Represents unsorted trash sinking away — reaching this line is the miss condition                                                                                |
| **Miss line Y**              | `490` px                       | The Y an item's fall is measured against — reaching this line without being sorted costs a life, same as a wrong sort                                            |
| Fixed drop X                 | `250` px                       | Horizontal center of the window and of the middle drop lane — every item falls exactly here                                                                      |
| Item size                    | `70` px                        | Diameter/width used for both drawing and collision detection                                                                                                     |
| Score panel                  | `x = 250–500`, `y = 0–50`      | Fixed HUD rectangle, drawn over the sky                                                                                                                          |
| Lives panel                  | `x = 330–500`, `y = 50–100`    | Fixed HUD rectangle, drawn over the sky, directly below the right portion of the score panel                                                                     |
| Starting fall speed          | `3.5` px/frame                 | Speed at the moment each new item spawns                                                                                                                         |
| Frame acceleration           | `+0.001` px/frame, every frame | Constant, permanent speed increase                                                                                                                               |
| Max fall speed               | `9.0` px/frame                 | Hard ceiling — **confirm this exact number against your assignment sheet before finalizing; 9.0 is the value used throughout this document**                     |
| Starting lives               | `3`                            |                                                                                                                                                                  |
| Points per correct sort      | `+25`                          | Adjust freely if your assignment specifies a different value                                                                                                     |
| Score threshold for bin swap | every multiple of `100`        | Triggers once per threshold crossed, not continuously                                                                                                            |
| Timer tick interval          | `~16` ms                       | Produces roughly 60 updates/redraws per second                                                                                                                   |

**Suggested starting palette** (from your own layout sketch — change freely, none of this affects any mechanic):

| Element                                                | Color        | Hex                                      |
| ------------------------------------------------------ | ------------ | ---------------------------------------- |
| Sky                                                    | light cyan   | `#BFF0F7`                                |
| Sea (background behind the drop lane and discard zone) | teal blue    | `#09c3db`                                |
| Left bin                                               | blue         | `#2F5FDE` _(alternate: red `#E8383C`)_   |
| Right bin                                              | yellow       | `#F5B400` _(alternate: green `#2FB350`)_ |
| Discard zone                                           | purple       | `#6F3198`                                |
| Score panel                                            | dark navy    | `#123A52`                                |
| Lives panel                                            | magenta/pink | `#E0447A`                                |

**Two corrections to your bounds, so you don't build these in as bugs:** your sea rectangle was `575` px tall starting at `y = 225`, which reaches `y = 800` — `100` px past the actual `700` px window height. And your discard rectangle was `175` px tall starting at `y = 490`, reaching only `y = 665`, leaving an unaccounted `35` px gap before the window's actual bottom edge. Both are fixed above: sea now runs the full remaining height (`225` to `700`, i.e. `475` px tall), and discard now runs exactly to the bottom edge (`490` to `700`, i.e. `210` px tall).

---

## Part 2: Project Setup in VS Code

1. Open VS Code. Open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and run **"Java: Create Java Project"**. Choose **"No build tools"** unless your instructor specifically requires Maven or Gradle. Name the project folder and open it in VS Code.
2. This generates a `src` folder (all your `.java` files live here), a `bin` folder (compiled output — never edit anything here by hand), and a `.vscode` folder with editor settings. A placeholder entry-point file may already exist inside `src`.
3. Inside `src`, create a folder for your top-level package (e.g., `blitzball`). Inside that, create four more folders: `model`, `view`, `engine`, and `io`.
4. For every class listed in Part 3 below, create an empty `.java` file in its matching folder, named exactly after the class (case-sensitive). Give each file a package declaration matching its folder path and an empty class body — nothing else yet.
5. Keep your entry-point file (`Main.java`) directly inside the top-level package folder, not inside any of the four subfolders — it's the only class allowed to know about all the others.
6. Click Run above the entry point's main method. You want it to execute with zero errors even though nothing happens yet — this proves your JDK setup, classpath, and package structure are all correct before any real logic exists.

**Verify before continuing:** the Run panel shows a clean exit, and there are no red squiggles anywhere in the Explorer tree.

---

## Part 3: Complete Class Specifications

### Package layout

```
blitzball/
├── Main.java
├── model/
│   ├── WasteType.java
│   ├── TrashItem.java
│   ├── PaperTrash.java
│   ├── PlasticTrash.java
│   ├── TinTrash.java
│   ├── GlassTrash.java
│   └── TrashSpawner.java
├── view/
│   └── GamePanel.java
├── engine/
│   └── GameManager.java
└── io/
    └── HighScoreManager.java
```

### `WasteType` — `model` package (enum)

- **Purpose:** a fixed, compiler-checked set of trash categories, so a category is compared as a type rather than as text — this prevents an entire class of spelling-mistake bugs in the sorting logic.
- **Required values:** `PAPER`, `PLASTIC`, `TIN`, `GLASS`. All four must exist even though only two are active at once (see Part 0).

### `TrashItem` — `model` package (abstract class)

The falling object. Abstract, not a plain class — you never want a generic, typeless piece of trash instantiated directly; every instance must be one of the four concrete subclasses below.

- **Fields, all private:**
  - `type` — a `WasteType`, set once at construction and never changed afterward.
  - `xPosition` — an `int`, always set to the fixed drop X constant and never changed after construction.
  - `yPosition` — a `double`. Must be a floating-point type, not an integer — the tiny `0.001` speed increments would round down to nothing on an integer type, and gravity would never actually change.
- **Constructor:** accepts a `WasteType` and a starting X position; always initializes `yPosition` to `0.0` regardless of what's passed in.
- **Required methods:**
  - `applyContinuousGravity(double currentSpeed)` — adds `currentSpeed` to `yPosition`. This is the only method that ever changes `yPosition`, and it lives on this class, not on the game engine — each item is responsible for its own motion.
  - `hasReachedMissLine(int missLineY)` — returns `true` once `yPosition` is greater than or equal to `missLineY`.
  - `getType()` — returns the item's `WasteType`.
  - `getX()` — returns `xPosition`.
  - `getY()` — returns `yPosition` cast down to an `int`, since screen-drawing coordinates need whole pixels even though the internal tracking is a `double`.
- **Rule:** no public setters of any kind. The only way `yPosition` ever changes is through `applyContinuousGravity`; the only way `type`/`xPosition` are set is through the constructor.

### `PaperTrash`, `PlasticTrash`, `TinTrash`, `GlassTrash` — `model` package

Four concrete classes, each extending `TrashItem`. Each one's constructor does exactly one thing: calls the parent constructor with its matching `WasteType` value and the X position it was given. None of these four classes need any fields or methods beyond what they inherit — their entire purpose is to exist as distinct, identifiable types so that other code can create and handle them by type without ever needing to check what category a generic item is.

### `TrashSpawner` — `model` package (Factory pattern)

Responsible for creating new falling items — nothing else in the program is allowed to construct a `PaperTrash`, `PlasticTrash`, `TinTrash`, or `GlassTrash` directly.

- **Field:** a single `Random` instance, created once when the spawner is constructed and reused for every call afterward. Creating a brand-new `Random` inside the generation method itself is a common mistake that can produce visibly repetitive results — avoid it.
- **Required method:** `generateNextTrashItem(int fixedDropX, WasteType currentLeftType, WasteType currentRightType)` — picks one of the two given types with equal (50/50) probability, constructs the matching concrete subclass at `fixedDropX`, and returns it typed as `TrashItem`. The two types are passed in as parameters rather than hardcoded, so this class never needs to know anything about the rest of the game's internal state.

### `GamePanel` — `view` package (extends `JPanel`)

The visible surface. This is the class that actually turns everything else in this document into something on screen — treat it as essential, not optional.

- **Field:** a single reference to the game's `GameManager` instance, used strictly for reading state — `GamePanel` must never call any method that changes game state, only getters.
- **Constructor requirement:** set a fixed preferred size of `500` by `700` pixels (matching Part 1's constants) so the window opens at a predictable size.
- **`paintComponent(Graphics g)` — must execute in exactly this order:**
  1. Call `super.paintComponent(g)` first. Skipping this causes old frames to never get cleared, leaving visible trails behind every moving object.
  2. Fill `y = 0` to `225` (full width) with the sky color.
  3. Fill `y = 225` to `700` (full width) with the sea color — this is the base water layer everything else in the lower two bands sits on top of.
  4. Fill the discard zone, `y = 490` to `700` (full width), with the discard color, layered on top of the sea fill from the previous step.
  5. Fill the left bin rectangle (`x = 0–170`, `y = 225–490`) with the left bin color, and the right bin rectangle (`x = 330–500`, `y = 225–490`) with the right bin color. The gap between them (`x = 170–330`) is deliberately left showing the plain sea color underneath — that gap is the visible drop lane.
  6. Draw the currently active trash item as a filled `70`x`70` shape centered at `(getX(), getY())`, in a color distinct per waste type (e.g., paper = off-white/beige, plastic = bright turquoise, tin = grey/silver, glass = green or amber) — distinct silhouettes matter more than exact colors for at-a-glance sorting.
  7. Draw the left bin's label text centered inside the left bin rectangle, and the right bin's label text centered inside the right bin rectangle — pulled fresh from `GameManager` every single frame; never cache or store label text inside `GamePanel` itself, since it must update the instant the bins swap.
  8. Draw the score panel rectangle (`x = 250–500`, `y = 0–50`) over the sky, with the current score as centered text on top of it.
  9. Draw the lives panel rectangle (`x = 330–500`, `y = 50–100`) over the sky, with the current lives count as centered text on top of it.
  10. If the game is over, draw a semi-transparent rectangle covering the entire panel, with "GAME OVER" and the final score as text on top of it, and skip drawing the falling item and bin labels underneath — the overlay should read as a clear stop, not as clutter on top of a still-visible game.
- **Rule:** `GamePanel` never calls `repaint()` on itself and never contains a `Timer` or any other trigger for its own redraws — a class outside `GamePanel` is responsible for calling `repaint()` on it repeatedly.

### `GameManager` — `engine` package

The central coordinator. Owns all game state and all rules; every other class either feeds it data or reads from it.

- **Fields, all private:**
  - `internalScore` — a `double`, the running raw score total.
  - `displayScore` — an `int`, the score value actually shown on screen and compared against the 100-point swap threshold.
  - `currentFallingSpeed` — a `double`, starts at `3.5`.
  - `MAX_FALL_SPEED` — a constant `double`, `9.0`.
  - `FRAME_ACCELERATION` — a constant `double`, `0.001`.
  - `playerLives` — an `int`, starts at `3`.
  - `leftBinType`, `rightBinType` — both `WasteType`, starting as `PAPER` and `PLASTIC` respectively.
  - `gameIsOver` — a `boolean`, starts `false`.
  - `spawner` — a `TrashSpawner` instance, constructed once.
  - `activeTrashItem` — a `TrashItem` reference; this is the boolean-lock mechanism in practice — a new item is only ever spawned when this reference has just been cleared.
  - `highScoreManager` — a `HighScoreManager` instance, constructed once.
  - `WINDOW_WIDTH` — constant `500`.
  - `FIXED_DROP_X` — constant `250`.
  - `MISS_LINE_Y` — constant `490`.
- **Constructor:** immediately calls the spawner to populate `activeTrashItem` with the first item, so the game has something falling from the very first frame.
- **Required methods:**
  - `processFrameUpdate(double deltaTime)` — called once per timer tick. In order: does nothing if `gameIsOver` is `true`; otherwise increases `internalScore` based on elapsed time, recalculates `displayScore` from it, checks whether `displayScore` just crossed a new multiple of `100` and if so triggers the bin-label swap; increases `currentFallingSpeed` by `FRAME_ACCELERATION` unless it's already at `MAX_FALL_SPEED`; calls `applyContinuousGravity` on `activeTrashItem`; checks `hasReachedMissLine` and if true, treats it as an automatic miss (see below).
  - `executePlayerAction(String direction)` — called once per arrow-key press. Does nothing if `gameIsOver` is `true` or if `activeTrashItem` is currently `null`. Otherwise compares `activeTrashItem.getType()` against `leftBinType` (if `direction` is left) or `rightBinType` (if `direction` is right); awards points on a match, otherwise treats it as a miss; either way, clears `activeTrashItem` and requests a new one from the spawner afterward.
  - A private miss-handling method — decrements `playerLives`; if `playerLives` reaches `0`, sets `gameIsOver` to `true` and calls the high-score manager to save the final score.
  - A private bin-swap method — exchanges the values of `leftBinType` and `rightBinType` with each other. This only ever swaps which side each category is displayed on; it never changes which two categories are active.
  - A private next-item method — sets `activeTrashItem` to `null`, then (only if the game isn't over) immediately calls the spawner for a replacement, passing in `FIXED_DROP_X`, `leftBinType`, and `rightBinType`.
  - Getters, all public: `getActiveTrashItem()`, `getScore()`, `getLives()`, `isGameOver()`, `getLeftBinLabel()` (returns `leftBinType` as displayable text), `getRightBinLabel()` (same, for the right side).

### `HighScoreManager` — `io` package

The only class in the entire program permitted to open a file handle on `highscore.txt`.

- **Required methods:**
  - `loadHighScore()` — checks whether `highscore.txt` exists in the project's working directory; if it doesn't, returns `0` without error. If it does, reads and returns the integer stored inside it.
  - `saveIfRecord(int finalScore)` — calls `loadHighScore()` internally to get the currently stored value; only writes `finalScore` to `highscore.txt`, overwriting whatever was there, if `finalScore` is strictly greater than the stored value. If it isn't, the method does nothing.
- **Rule:** both methods must handle a missing or unreadable file gracefully — the game must never crash because `highscore.txt` doesn't exist yet on a first run.

### `Main.java` — top level of the package

The entry point and the only place all the other classes get wired together.

- **Required sequence inside `main`:**
  1. Construct one `GameManager`.
  2. Construct one `GamePanel`, giving it the `GameManager` instance.
  3. Construct a `JFrame`: set a title, make it non-resizable (the fixed-pixel coordinate math throughout this spec assumes a fixed window size), add the `GamePanel` to it, call `pack()` so the frame sizes itself to the panel's preferred size, center it on screen, set the default close operation to exit the application, then make it visible.
  4. Register **Key Bindings** (not a raw `KeyListener`) on the panel's input/action maps for the left and right arrow keys, each triggering a call to `GameManager.executePlayerAction(...)` with the matching direction.
  5. Create a `javax.swing.Timer` with roughly a `16` millisecond delay. Its callback must call `GameManager.processFrameUpdate(...)` first, then `GamePanel.repaint()` second, every single tick — update always happens before render. Start the timer only after the frame has already been made visible.

**Verify before continuing:** launching the program opens a `500`x`700` window showing sky, two colored side bins flanking an open drop lane, a discard band at the bottom, one piece of trash sinking down the middle, both bin labels, a live score and lives readout, and a game-over overlay once lives run out.

---

## Part 4: Build Order (Follow In Sequence, Verify Each Step Before Moving On)

1. **Blank window.** Just a `JFrame` containing a `GamePanel` filled with one solid color. _Confirms:_ your setup and window pipeline work at all.
2. **One placeholder shape falling at a constant speed, no real game logic.** A single hardcoded shape whose Y position increases by a fixed constant every timer tick, with no acceleration and no `GameManager` involved yet. _Confirms:_ your timer loop and `repaint()` cycle actually work.
3. **Real gravity and miss-line detection.** Wire in the actual acceleration math and the `490`-pixel boundary check; print to the console on a hit instead of touching lives yet. _Confirms:_ the item visibly speeds up as it falls, and the console fires right as it crosses into the discard zone.
4. **Respawn logic.** The item destroys itself on reaching the miss line and a new one appears via the spawner. _Confirms:_ items appear one after another indefinitely, never two at once.
5. **Static bin labels and raw keyboard capture.** Hardcoded label text on screen; arrow presses print to the console with no scoring yet. _Confirms:_ labels render, and every keypress is detected.
6. **Wire input into real scoring.** Arrow presses now call the actual sorting logic, and score/lives visibly update. _Confirms:_ a full sort-and-score loop works end to end.
7. **100-point bin swap.** _Confirms:_ the two bin labels visibly trade sides the instant score crosses a threshold.
8. **Lives depletion and game over.** _Confirms:_ the overlay appears and input stops the moment lives hit zero.
9. **Real distinct trash types.** Replace the placeholder shape with the four concrete classes (only two active per Part 0), each drawn with a distinct look. _Confirms:_ two visually different trash types alternate randomly, always along the same vertical line.
10. **Visual polish.** Ocean gradient, horizon styling, refined bin art. _Confirms:_ the game reads as an ocean cleanup scene, not placeholder shapes on a blank background.
11. **High-score file I/O.** _Confirms:_ `highscore.txt` appears after your first game over and only updates on runs that beat the stored value.
12. **Optional, only if you choose to pursue it:** wiring `TIN`/`GLASS` into active rotation per Part 0.

---

## Part 5: Requirements Checklist

- [ ] Exactly one `TrashItem` is active at any given time.
- [ ] Every item falls at the same fixed X position — no horizontal variation, ever.
- [ ] Fall speed increases by exactly `+0.001` every frame, capped at `9.0`, for the entire game.
- [ ] The miss line sits at `Y = 490` — reaching it without sorting costs a life.
- [ ] A correct sort adds points; an incorrect sort or a miss-line hit costs exactly one life.
- [ ] Bin labels swap sides exactly on multiples of `100`, once per threshold crossed.
- [ ] The spawner never produces an item that doesn't match one of the two currently active bin types.
- [ ] `highscore.txt` persists across separate runs of the program and is only overwritten when a run's score exceeds the previously stored value.
- [ ] Reaching zero lives ends the game exactly once and triggers exactly one high-score save attempt.

---

## Part 6: OOP Pillars — Where Each One Lives

| Pillar            | Class(es)                                                         | What demonstrates it                                                                                                                                                                   |
| ----------------- | ----------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Encapsulation** | `TrashItem`, `GameManager`                                        | Every field is private; state only ever changes through defined methods, never through public setters or direct field access from other classes                                        |
| **Inheritance**   | `TrashItem` → `PaperTrash`/`PlasticTrash`/`TinTrash`/`GlassTrash` | The physics (`applyContinuousGravity`, `hasReachedMissLine`) is written once, in the parent, and used by all four subclasses without duplication                                       |
| **Polymorphism**  | `TrashSpawner`'s return type, `GameManager.activeTrashItem`       | Both are declared and used as type `TrashItem`, never as any specific concrete subclass — the correct subclass's behavior runs automatically at runtime without any type-checking code |
| **Abstraction**   | `HighScoreManager`, `GameManager`'s public methods                | Every other class calls simple, purpose-named methods (`saveIfRecord`, `processFrameUpdate`) without ever seeing file handles, raw math, or internal bookkeeping                       |

---

## Part 7: Final Verification — Run This Checklist Start to Finish on a Clean Launch

1. Launch the program — a `500`x`700` window opens showing sky at the top, two colored bins with an open lane between them, and a discard band at the bottom.
2. A piece of trash falls from the top, visibly speeding up, always along the same vertical line.
3. Sort several items correctly — score increases each time, and a new (possibly different-looking) item appears immediately after each sort.
4. Sort one item incorrectly — a life is removed.
5. Let an item reach the miss line without sorting it — a life is removed, identically to a wrong sort.
6. Push the score past a multiple of `100` — the two bin labels visibly swap which side they're on.
7. Let lives reach zero — a game-over overlay appears, and the arrow keys stop doing anything.
8. Look in the project folder — a `highscore.txt` file exists, containing your final score.
9. Close and relaunch the program — score and lives reset to their starting values, but the value inside `highscore.txt` carries over, and only changes if a new run beats it.

If every step above holds true, the implementation satisfies every requirement in this document in full.
