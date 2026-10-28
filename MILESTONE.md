# Blitz Ball — Java OOP Build Guide

A milestone-by-milestone path from an empty VS Code window to a finished game. No code is included on purpose — every section describes *what* to build and *why*, so the implementation decisions stay yours.

---

## Part 0 — Environment Setup

1. **Install the JDK.** You need a Java Development Kit (17 or 21, either is fine for Swing). Verify with `java -version` and `javac -version` in a terminal — both must resolve.
2. **Install VS Code extensions.** Add the "Extension Pack for Java" (this bundles the language server, debugger, test runner, and project manager) and "Gradle for Java" is optional — you don't need a build tool for a project this size.
3. **Create the project folder.** Skip Maven/Gradle entirely. A single source folder compiled with `javac` is simpler to reason about while you're learning, and it's what most OOP courses expect for a project like this. Structure:
   ```
   BlitzBall/
     src/
       (all your .java files go here, one class per file)
     assets/        (optional: fonts, sound files, icons)
     highscore.txt  (created automatically at runtime — don't hand-create it)
   ```
4. **Open the folder in VS Code** ("Open Folder", not "Open File"). The Java extension will detect `src/` automatically once you have at least one `.java` file in it.
5. **Compile and run** from the integrated terminal once you have code: compile every file in `src/` into a `bin/` (or `out/`) folder, then run the class containing your `main` method from that output folder. VS Code's "Run" button (above `main`) does this for you once the extension recognizes the project — use it instead of hand-typing `javac`/`java` commands once things are working, but know the manual commands as a fallback if the Run button ever misbehaves.

---

## Part 1 — Concepts You Need Before Writing a Line

These are Swing/game-specific ideas that your general Java/OOP instruction won't have covered. Understand them conceptually before Milestone 0.

**JFrame vs. JPanel.** A `JFrame` is the OS-level window — title bar, close button, resizing. A `JPanel` is a surface you draw on and place inside the frame. Your game logic and rendering live in a custom class that *extends* `JPanel`. The frame is just a container; it holds exactly one game panel and does almost nothing else.

**The paint cycle.** Swing calls a method named `paintComponent` on your panel whenever it needs to redraw — you never call it directly yourself. Inside it, you receive a `Graphics` (or `Graphics2D`) object and use it to draw shapes, text, and images. Every visible thing in your game — the ball, the labels, the score — gets drawn here, every single frame, from scratch. There's no "leave this shape where it was" — you clear and redraw the whole picture each cycle.

**The game loop.** A game isn't event-driven the way a form-based app is; it needs to update state ~60 times per second whether or not the player does anything, because the ball keeps falling on its own. The simplest way to get this in Swing is `javax.swing.Timer`: it fires an event repeatedly at a fixed interval, and each firing is one "frame" — you update your game state, then trigger a repaint. This is dramatically simpler than managing a `Thread` yourself, and for a game this scope it performs perfectly well. Treat "one Timer tick = one frame" as your mental model for the whole project.

**KeyListener vs. Key Bindings.** Swing offers two ways to capture keyboard input. `KeyListener` is the simpler, more direct one and is fine for this project — attach it to the panel, and make sure the panel is focusable and requests focus on startup, or your key presses will silently go nowhere. (This trips up almost everyone the first time — if arrow keys do nothing, focus is the first thing to check.)

**Why this matters for your architecture:** the game loop (Timer) and the render step (paintComponent) both live on the panel class, because Swing's design ties rendering to a specific component. Your job is to keep that panel "dumb" about game rules — it should orchestrate ticking and drawing, but delegate the actual decisions (is gravity capped? did the score cross 100? was the sort correct?) to other classes.

---

## Part 2 — The Class Architecture

Each class below has one job. If you find yourself wanting to put ball-falling logic inside the score class, or file-reading logic inside the ball class, that's a sign the responsibility is in the wrong place.

| Class | Responsibility | Notes |
|---|---|---|
| `Main` | Program entry point only | Creates the window and starts it. Nothing else. |
| `GameWindow` | Wraps the `JFrame` | Sets title, size, close behavior, and adds the `GamePanel`. |
| `GamePanel` | The game loop + rendering hub | Owns the `Timer`, owns the current `Ball`, delegates to the manager classes, calls their update/draw methods each tick. |
| `Ball` | One falling ball's state | x/y position, vertical velocity, radius, and which category it belongs to (e.g. SOCCER or TENNIS). Knows how to update its own position and how to draw itself — nothing more. |
| `LabelManager` | The two on-screen category labels and their left/right arrangement | Knows which category is currently on the left vs. right, and knows how to swap them. |
| `ScoreManager` | Score and lives | Tracks the running score, remaining lives, and detects when a shuffle threshold has been crossed. |
| `InputHandler` | Keyboard input | Implements the key-listening interface and reports "left was pressed" / "right was pressed" — nothing about what that *means* for the game. |
| `GameState` | Which screen/mode the game is in | An enum: `MENU`, `PLAYING`, `GAME_OVER` (add `MENU` even though the spec doesn't strictly require it — it gives you a clean place to show the high score and a "press any key to start" prompt). |
| `HighScoreManager` | Reading and writing `highscore.txt` | The only class that touches the filesystem. |
| `Constants` (or `GameConfig`) | Shared tunable numbers | Screen width/height, gravity increment, gravity cap, starting lives, floor y-position, ball radius. Centralizing these means you change a number in one place instead of hunting through five files. |

### Design principles at play

- **Single Responsibility Principle.** Every row in that table is a one-sentence job description. If a class's job needs "and" in the description, split it.
- **Encapsulation.** `Ball`'s position and velocity should be private fields with controlled access — nothing outside the class should be able to yank the ball to an arbitrary position. `GamePanel` should ask the ball to update itself, not reach in and manipulate its internals.
- **The State pattern (lightweight version).** You don't need a full State design pattern with interfaces per state — for this project scope, a `GameState` enum checked inside your update/render methods (essentially a switch) is the appropriately-sized tool. Don't over-engineer this part; it's a common trap to build an elaborate state machine framework for a game with three states.
- **Composition, not inheritance.** `GamePanel` *has a* `Ball`, *has a* `ScoreManager`, etc. There's no meaningful "is-a" relationship anywhere in this game — resist any urge to make `Ball` extend some generic `GameObject` superclass. With one object type in the whole game, that hierarchy adds ceremony without paying for itself.

### The one architectural decision you need to make deliberately: where does gravity live?

Re-read the spec closely: *"Gravity increases smoothly by a tiny decimal every frame up to a strict cap."* This is not saying "each ball accelerates as it falls" — every falling object does that trivially by definition of velocity increasing. It's saying the **strength of gravity itself** ramps up over the course of the game, permanently, until it hits its ceiling. That means:

- If gravity lived as a field on `Ball`, it would reset to its starting value every time a new ball spawns — the game would never actually get harder, because every ball would restart the ramp from zero.
- Gravity needs to live somewhere that persists across ball spawns — most naturally as a field on `GamePanel` (or a small `Physics` helper it owns), incremented once per frame regardless of which ball currently exists, and handed to whichever ball is active so it can apply that shared value to its own fall.

This is a good example of a question your OOP class will actually care about when grading: *where does state that outlives an individual object belong?* Get this one right and the difficulty curve works exactly as specified — the whole session gets faster, not just each individual drop.

---

## Part 3 — Milestone Roadmap

Build in this exact order. Each milestone should be independently runnable and visibly checkable before you move to the next — don't stack three milestones' worth of untested logic on top of each other.

### Milestone 0 — A window that opens

**Goal:** An empty window appears on screen and closes cleanly.

**Classes involved:** `Main`, `GameWindow`.

**Logic breakdown:** `Main`'s only job is to construct a `GameWindow` and make it visible. `GameWindow` sets a fixed size (pick something like 480×640 — tall and narrow suits a "ball falls from sky" game), a title, and configures the close operation so the whole program exits when the window closes.

**Done when:** Running the program shows a blank window you can close normally.

---

### Milestone 1 — A static ball on screen

**Goal:** A circle appears at a fixed position. No movement yet.

**Classes involved:** `GamePanel`, `Ball` (minimal version — just x, y, radius, and a draw method).

**Logic breakdown:** `GamePanel` extends `JPanel`, gets added to the `GameWindow`, and overrides `paintComponent`. Inside it, ask the `Ball` to draw itself using the `Graphics` object passed in. `Ball`'s draw method fills an oval (a circle is just an oval with equal width and height) at its stored x/y.

**Done when:** A filled circle renders in a fixed spot when you run the program.

---

### Milestone 2 — A ticking game loop

**Goal:** The panel updates on a fixed interval, proven by something visibly changing (even something trivial like a counter printed to console).

**Classes involved:** `GamePanel`.

**Logic breakdown:** Add a `Timer` to `GamePanel`, firing roughly every 16 milliseconds (≈60 fps). Each tick should call an `update` method (where game state changes) followed by a `repaint()` call (which schedules `paintComponent` to run again). Keep `update` and `paintComponent` as two clearly separate methods from the very start — mixing "change state" and "draw state" into one blob is the single most common structural mistake in beginner game code, and it's much harder to untangle later than to keep separate now.

**Done when:** You can prove ticks are firing at a steady rate (console output is fine for this milestone — visual proof comes next).

---

### Milestone 3 — The ball falls (constant speed)

**Goal:** The ball moves downward every frame at a fixed rate — gravity not yet implemented, just straight-line motion.

**Classes involved:** `Ball`.

**Logic breakdown:** Give `Ball` a vertical velocity field. Each `update` call, add that velocity to the ball's y-position. Call `ball.update()` from `GamePanel`'s update method, followed by `repaint()`.

**Done when:** You watch a circle fall down the window and off the bottom edge.

---

### Milestone 4 — Real gravity: smooth ramp with a cap

**Goal:** The fall accelerates smoothly over time, following the exact spec: +0.001 pixels/frame every frame, capped at 9.0.

**Classes involved:** `GamePanel` (owns the persistent gravity value — see Part 2's design note above), `Ball` (receives gravity, applies it to its own velocity).

**Logic breakdown:**
1. `GamePanel` holds a decimal gravity value starting at some small base (e.g. 0.1 — the spec doesn't state a starting value, so pick one that feels right and treat it as a tunable constant).
2. Every frame, before updating the ball: `gravity = minimum(gravity + 0.001, 9.0)`. This is a classic "clamp" pattern — increment, then never let the result exceed the ceiling.
3. Pass the current gravity value into the ball's update call. Inside `Ball`, add gravity to the vertical velocity, then add velocity to position — this two-step (accelerate, then move) is what makes it feel like real falling rather than constant speed.

**Done when:** The ball visibly starts slow and gets noticeably faster the longer the program runs, and does not exceed a fixed maximum fall speed no matter how long you let it run.

---

### Milestone 5 — Ground collision, life loss, and respawn

**Goal:** When the ball reaches a floor line, it's destroyed, a life is lost, and a new ball spawns at the top — with exactly one ball existing at any time.

**Classes involved:** `GamePanel`, `Ball`, `ScoreManager` (minimal — just a lives counter for now).

**Logic breakdown:** Define a floor y-coordinate as a constant. Every frame, after moving the ball, check whether its y-position (plus radius) has reached or passed that line. If it has: decrement lives, discard the current ball reference, and create a brand-new `Ball` at the top of the screen with position reset and starting velocity reset to zero (gravity, per your Milestone 4 decision, is *not* reset — it lives on the panel and keeps ramping).

Enforcing "only one ball at a time" is almost free if you store the current ball as a single field (not a list/array) on `GamePanel` — there's structurally nowhere to put a second one. Resist the temptation to use a collection here; a single reference is the correct data structure for "exactly one of something."

**Done when:** Balls repeatedly fall, hit the floor, disappear, and a new one starts falling from the top — indefinitely, with a lives counter (printed to console for now) ticking down each time.

---

### Milestone 6 — Life system and Game Over

**Goal:** When lives reach zero, the game stops running and enters a distinct Game Over state.

**Classes involved:** `GameState`, `GamePanel`, `ScoreManager`.

**Logic breakdown:** Introduce the `GameState` enum and a field on `GamePanel` tracking the current state, starting at `PLAYING`. Your `update` method should check the state first — if it's not `PLAYING`, skip all the gameplay updates entirely (no ball movement, no spawning). When lives hit zero, flip the state to `GAME_OVER`. Your `paintComponent` should branch on state too, so Game Over can show different content (you'll wire up the actual "Game Over" screen and text rendering here, even if it's just placeholder text for now).

**Done when:** Losing all your lives freezes the game and visibly shows a different screen; nothing about the falling ball continues to update afterward.

---

### Milestone 7 — The two category labels

**Goal:** Two text labels are permanently visible on screen, one on the left half, one on the right half.

**Classes involved:** `LabelManager`.

**Logic breakdown:** `LabelManager` holds two fixed category names (e.g. `"SOCCER"` and `"TENNIS"`) and a flag for which one is currently on the left. It has a draw method that renders each label's text at a fixed x-position depending on the current arrangement — left label drawn in the left half of the screen, right label in the right half, both near the top or bottom (your call, but keep them out of the ball's typical falling path so they're never visually confused with the ball itself).

**Done when:** Both category names are visible on screen, correctly positioned left and right, and stay static while the ball falls.

---

### Milestone 8 — The ball gets a category

**Goal:** Every newly spawned ball is randomly assigned one of the two categories, and that category is visible somehow (during development, printing it to console is enough — you don't need to render it on the ball itself, since the whole point of the game is that the player has to remember which side it belongs to based on the labels).

**Classes involved:** `Ball`, `LabelManager` (as the source of truth for what categories exist).

**Logic breakdown:** When `GamePanel` spawns a new ball (Milestone 5's respawn logic), randomly pick one of the two category names and assign it to the new ball's category field. Keep this random choice equally weighted between the two — don't let it drift toward one category more than the other.

**Done when:** Console output confirms each new ball is randomly one category or the other, roughly evenly over many spawns.

---

### Milestone 9 — Keyboard input is captured

**Goal:** Pressing the left or right arrow key is detected and logged, without affecting gameplay yet.

**Classes involved:** `InputHandler`.

**Logic breakdown:** `InputHandler` implements the key-listening interface and is attached to `GamePanel`. Make sure the panel is focusable and explicitly requests focus once it's visible — this is the step that trips people up (see Part 1). On a left-arrow or right-arrow key press, have `InputHandler` do nothing more than report which direction was pressed (console output for this milestone).

**Done when:** Pressing left/right arrows reliably logs the correct direction every time, with no missed or duplicated presses.

---

### Milestone 10 — Sorting resolution

**Goal:** Pressing left or right resolves the current ball instantly: check whether the ball's category matches the label currently on that side, apply the correct consequence, destroy the ball, and spawn a new one — regardless of where the ball currently is on screen.

**Classes involved:** `GamePanel` (coordinates the check), `Ball`, `LabelManager`, `ScoreManager`.

**Logic breakdown:** This is the core gameplay loop, so get the sequence exactly right:
1. Player presses left or right.
2. Ask `LabelManager`: "what category is currently on the side that was pressed?"
3. Compare that against the current ball's category.
4. If they match: correct sort. If they don't: incorrect sort.
5. Either way, the current ball is immediately discarded and a new one spawns — the resolution doesn't wait for the ball to reach any particular position. This is an important simplification: there's no collision detection between the ball and a "zone" at all. The sort is purely a comparison between two category values, triggered by a keypress, independent of the ball's x/y coordinates. If you find yourself calculating whether the ball's x-position overlaps a "left zone" rectangle, you've overcomplicated it — delete that logic.

**Done when:** Pressing the correct arrow for the ball's category and the label's current position produces one outcome; pressing the wrong one produces another; either way, a new ball spawns immediately and falling resumes.

---

### Milestone 11 — Scoring correct/incorrect

**Goal:** Correct sorts add points; incorrect sorts cost a life (reusing the life-loss path from Milestone 6).

**Classes involved:** `ScoreManager`.

**Logic breakdown:** Extend the correct/incorrect branch from Milestone 10: on correct, add a fixed point value to the running score. On incorrect, decrement lives through the same method Milestone 5/6 already use for ground misses — a wrong sort and a missed ball are the same underlying event ("a life was lost"), so route them through one shared method rather than duplicating the decrement-and-check-game-over logic in two places.

**Done when:** The on-screen (or console, if you haven't rendered UI text yet) score climbs on correct answers, and lives fall on both missed balls and wrong sorts, correctly triggering Game Over at zero.

---

### Milestone 12 — Score shuffling at multiples of 100

**Goal:** Every time the score crosses a multiple of 100, the two labels swap sides.

**Classes involved:** `ScoreManager` (detects the crossing), `LabelManager` (performs the swap).

**Logic breakdown:** Here's the subtlety worth thinking through carefully: if your point value per correct answer is anything other than exactly 1, checking `score % 100 == 0` can miss the boundary entirely — a score that jumps from 95 to 105 in one hit never equals a multiple of 100 exactly. The robust way to detect "just crossed a hundred" is to compare `oldScore / 100` against `newScore / 100` using integer division — if those two values differ, you've crossed at least one hundred-boundary, regardless of how big the point jump was. Compute this in `ScoreManager` right after adding points, and if a crossing happened, tell `LabelManager` to flip its left/right arrangement.

**Done when:** Watch the score approach and pass 100 — the labels visibly swap sides exactly once at the crossing, no matter what your per-answer point value is.

---

### Milestone 13 — High score file I/O

**Goal:** The current high score is read from `highscore.txt` when the game starts (or when the menu/game-over screen needs to display it), and written back out if the player beats it, when the game ends.

**Classes involved:** `HighScoreManager`.

**Logic breakdown:**
- **On startup:** attempt to read a single integer from `highscore.txt`. If the file doesn't exist yet (first run ever), treat the high score as zero rather than crashing — this is a real edge case you must handle explicitly, not an afterthought.
- **On Game Over:** compare the just-finished run's score against the stored high score. If the new score is higher, overwrite the file with the new value. If not, leave the file untouched.
- Keep all of this — the file path, the reading, the writing, the missing-file fallback — inside `HighScoreManager` exclusively. No other class should ever open a file directly; they should ask `HighScoreManager` for the current high score or hand it a new score to consider.

**Done when:** Close and reopen the program after a high run — the previous high score persists. Beat it, and the file updates; fall short, and it doesn't.

---

### Milestone 14 — Polish pass

**Goal:** Tie everything together into a presentable, submittable build.

**Suggested (non-exhaustive) checklist:**
- Render actual score, lives, and (on the menu/game-over screen) high score as text, not just console output.
- Add a simple menu screen (`GameState.MENU`) with a "press any key to start" prompt, and route the game-over screen back to it or offer a restart.
- Visually distinguish the two category labels from each other and from any other on-screen text.
- Double check the ball's spawn position never overlaps the labels or floor line at spawn time.
- Playtest the difficulty ramp — does gravity actually feel like it's ramping toward the 9.0 cap over a realistic play session, or does it hit the cap too fast/slow? This is a one-line constant change (the +0.001 increment or the starting value) if it needs tuning, and tuning it is a legitimate design decision worth a sentence in your project write-up.

---

## Part 4 — Common Pitfalls Coming From C

A few things that catch people making this exact jump:

- **No manual memory management, but references still matter.** When you assign one `Ball` variable to another, you're copying a reference, not the object — both variables point to the same ball. This matters when you're deciding whether "destroy the ball" means "set the reference to a new object" (correct approach here) versus trying to manually clear its fields (unnecessary — Java's garbage collector reclaims the old object once nothing references it anymore).
- **Access modifiers are your friend, use them deliberately.** Coming from C, where everything in a file is just accessible, Java's `private`/`public` on fields and methods is doing real design work — it's how you enforce the encapsulation described in Part 2. Default to `private` fields with intentional public methods, not the other way around.
- **Static vs. instance.** `Constants` should be `static` fields (one shared copy, no need to create a `Constants` object). `Ball`'s position should absolutely not be `static` — every ball needs its own copy of its own position, and if you make it static you'll find all your balls mysteriously sharing one position.
- **Integer division truncates, same as C.** The Milestone 12 crossing-detection trick relies on this behavior being intentional, not a bug — `205 / 100` giving `2`, discarding the remainder, is exactly what makes the comparison work.
- **`null` is not the same as C's `NULL` pointer risk, but it can still crash you.** Calling a method on a ball reference that hasn't been assigned yet throws a `NullPointerException` at runtime rather than silently corrupting memory — which is safer than C's undefined behavior, but still means you should be deliberate about exactly when `GamePanel`'s ball field is guaranteed to hold a real object (hint: it should never legitimately be null once Milestone 0 setup finishes running).

---

## Part 5 — Suggested File List

One file per class, all in `src/`:

```
Main.java
GameWindow.java
GamePanel.java
Ball.java
LabelManager.java
ScoreManager.java
InputHandler.java
GameState.java
HighScoreManager.java
Constants.java
```

Ten files is a completely normal size for a project at this scope — don't feel pressure to consolidate them, and don't feel pressure to split further, either. Each one earns its place by owning exactly one job from the Part 2 table.