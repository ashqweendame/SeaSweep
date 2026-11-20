package view;

import engine.BinSlot;
import engine.GameConfig;
import engine.GameManager;
import engine.GameMode;
import engine.GamePhase;
import engine.LevelData;
import engine.Playfield;
import engine.TutorialManager;
import model.BinType;
import model.ScoreEntry;
import model.Trash;
import model.TrashKind;

import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class GamePanel extends Canvas {

    private static final int WIDTH = GameConfig.WINDOW_WIDTH;
    private static final int HEIGHT = GameConfig.WINDOW_HEIGHT;

    private final GameManager gameManager;
    private final int width = WIDTH;
    private final int height = HEIGHT;
    private final int centerX = width / 2;

    // where the lives turtle is drawn (center x, center y, size of its box) - top-left sky
    private final int TURTLE_X = 125;
    private final int TURTLE_Y = 112;
    private final int TURTLE_SIZE = 200;

    // buttons: {x, y, width, height}
    private final int[] BTN_LEVELS = {centerX - 110, 290, 220, 60};
    private final int[] BTN_UNLIMITED = {centerX - 110, 370, 220, 60};
    private final int[] BTN_TUTORIAL = {centerX - 110, 450, 220, 60};
    private final int[] BTN_INTRO_START = {centerX - 70, 430, 140, 40};
    private final int[] BTN_COMPLETE_NEXT = {centerX - 165, 360, 160, 44};
    private final int[] BTN_COMPLETE_MENU = {centerX + 5, 360, 160, 44};
    private final int[] BTN_RESTART = {centerX - 138, 405, 128, 40};
    private final int[] BTN_GAMEOVER_MENU = {centerX + 10, 405, 128, 40};
    private final int[] BTN_BACK = {10, 10, 70, 32};
    private final int[] BTN_TUTORIAL_NEXT = {centerX - 75, 180, 150, 30};

    // pause: a small button in the sky while playing, and the three buttons on the pause card
    private final int[] BTN_PAUSE = {centerX - 45, 10, 90, 32};
    private final int[] BTN_PAUSE_RESUME = {centerX - 100, 275, 200, 44};
    private final int[] BTN_PAUSE_RESTART = {centerX - 100, 331, 200, 44};
    private final int[] BTN_PAUSE_MENU = {centerX - 100, 387, 200, 44};

    // level-select grid: levels 1-10 in 5 columns x 2 rows, the special level as one wide cell below
    private final int GRID_COLS = 5;
    private final int CELL_W = 130;
    private final int CELL_H = 110;
    private final int CELL_GAP = 24;
    private final int GRID_X = (WIDTH - (GRID_COLS * CELL_W + (GRID_COLS - 1) * CELL_GAP)) / 2;
    private final int GRID_Y = 190;

    private final Color COLOR_SKY = Color.web("#BFF0F7");
    private final Color COLOR_SEA = Color.web("#09c3db");
    private final Color COLOR_PLASTIC = Color.web("#2F5FDE");
    private final Color COLOR_TIN = Color.web("#E8383C");
    private final Color COLOR_PAPER = Color.web("#F5B400");
    private final Color COLOR_GLASS = Color.web("#2FB350");
    private final Color COLOR_METAL = Color.web("#8A97A6");
    private final Color COLOR_ORGANIC = Color.web("#8B5E34");
    private final Color COLOR_RECYCLABLE = Color.web("#19A7A0");
    private final Color COLOR_MISS = Color.web("#6F3198");
    private final Color COLOR_SCORE = Color.web("#123A52");
    private final Color COLOR_LIVES = Color.web("#E0447A");
    private final Color COLOR_SHADE = Color.rgb(0, 0, 0, 70 / 255.0);
    private final Color COLOR_GOOD = Color.web("#7CFC9A");
    private final Color COLOR_BAD = Color.web("#FF8A8A");
    private final Color COLOR_LOCKED = Color.web("#6B7C86");
    private final Color COLOR_LOCKED_TEXT = Color.web("#C9D3D9");

    private final Font fontBOLD = Font.font("Arial", FontWeight.BOLD, 16);

    private final Assets assets = new Assets(); // your drawn pictures (optional, per item)

    public GamePanel(GameManager gameManager) {
        super(WIDTH, HEIGHT);
        this.gameManager = gameManager;
    }

    // Called from outside every tick
    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        // Canvas keeps its state between frames, so reset what we rely on
        gc.setFont(fontBOLD);
        gc.setImageSmoothing(true);
        gc.setLineWidth(1);

        switch (gameManager.getState()) {
            case MENU:
                drawMenu(gc);
                break;
            case LEVEL_SELECT:
                drawLevelSelect(gc);
                break;
            case TUTORIAL:
                drawTutorial(gc);
                break;
            default:
                drawGame(gc);
                break;
        }
    }

    // ================= screens =================

    private void drawMenu(GraphicsContext gc) {
        drawBackground(gc);

        Font titleFont = Font.font("Arial", FontWeight.BOLD, 52);
        gc.setFill(COLOR_SHADE);
        drawCentered(gc, "SEA SWEEP", titleFont, centerX + 3, 133);
        gc.setFill(COLOR_SCORE);
        drawCentered(gc, "SEA SWEEP", titleFont, centerX, 130);
        drawCentered(gc, "Sort the trash. Save the sea.", bold(18), centerX, 165);

        // a few of the trash pieces
        TrashKind[] showcase = {TrashKind.PAPER_TRASH, TrashKind.PLASTIC_TRASH, TrashKind.WINE_BOTTLE, TrashKind.CAN};
        for (int i = 0; i < showcase.length; i++) {
            drawKindSprite(gc, showcase[i], centerX - 150 + i * 100, 215, 60);
        }

        Font btnFont = bold(26);
        drawButton(gc, BTN_LEVELS, COLOR_GLASS, "LEVELS", btnFont, Color.WHITE, true);
        drawButton(gc, BTN_UNLIMITED, COLOR_PAPER, "UNLIMITED", btnFont, COLOR_SCORE, true);
        drawButton(gc, BTN_TUTORIAL, COLOR_PLASTIC, "TUTORIAL", btnFont, Color.WHITE, true);

        gc.setFill(Color.WHITE);
        drawCentered(gc, "L: levels     U: unlimited     T: tutorial", Font.font("Arial", FontWeight.NORMAL, 14), centerX, 540);
    }

    // ---- level select: a grid of locked / open / cleared levels ----

    private void drawLevelSelect(GraphicsContext gc) {
        drawBackground(gc);

        Font titleFont = bold(40);
        gc.setFill(COLOR_SHADE);
        drawCentered(gc, "SELECT LEVEL", titleFont, centerX + 3, 103);
        gc.setFill(COLOR_SCORE);
        drawCentered(gc, "SELECT LEVEL", titleFont, centerX, 100);
        drawCentered(gc, "Clear a level to unlock the next one.", bold(16), centerX, 135);

        for (int level = 1; level <= LevelData.TOTAL_LEVELS; level++) {
            drawLevelCell(gc, level);
        }

        drawButton(gc, BTN_BACK, Color.WHITE, "BACK", bold(14), COLOR_MISS, false);
    }

    private void drawLevelCell(GraphicsContext gc, int level) {
        int[] b = levelCell(level);
        boolean unlocked = gameManager.isUnlocked(level);
        boolean cleared = gameManager.isCleared(level);
        boolean special = level == LevelData.SPECIAL_LEVEL;
        boolean next = unlocked && !cleared;   // the level you are up to

        gc.setFill(COLOR_SHADE);
        gc.fillRoundRect(b[0] + 4, b[1] + 6, b[2], b[3], 16, 16);

        Color fill;
        if (!unlocked) {
            fill = COLOR_LOCKED;
        } else if (special) {
            fill = COLOR_MISS;
        } else if (cleared) {
            fill = COLOR_GLASS;
        } else {
            fill = COLOR_PLASTIC;
        }
        gc.setFill(fill);
        gc.fillRoundRect(b[0], b[1], b[2], b[3], 16, 16);

        if (next) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(4);
            gc.strokeRoundRect(b[0], b[1], b[2], b[3], 16, 16);
            gc.setLineWidth(1);
        }

        double cx = b[0] + b[2] / 2.0;
        double cy = b[1] + b[3] / 2.0;
        gc.setFill(unlocked ? Color.WHITE : COLOR_LOCKED_TEXT);
        if (special) {
            drawCentered(gc, "SPECIAL LEVEL", bold(24), cx, cy + 6);
        } else {
            drawCentered(gc, String.valueOf(level), bold(40), cx, cy + 10);
        }

        String status = !unlocked ? "LOCKED" : (cleared ? "CLEARED" : "PLAY");
        drawCentered(gc, status, bold(12), cx, b[1] + b[3] - 12);

        double badgeX = b[0] + b[2] - 20;
        double badgeY = b[1] + 22;
        if (!unlocked) {
            drawPadlock(gc, badgeX, badgeY);
        } else if (cleared) {
            drawCheck(gc, badgeX, badgeY);
        }
    }

    private void drawCheck(GraphicsContext gc, double cx, double cy) {
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 11, cy - 11, 22, 22);
        gc.setStroke(COLOR_GLASS);
        gc.setLineWidth(3);
        gc.strokeLine(cx - 5, cy, cx - 1, cy + 4);
        gc.strokeLine(cx - 1, cy + 4, cx + 6, cy - 4);
        gc.setLineWidth(1);
    }

    private void drawPadlock(GraphicsContext gc, double cx, double cy) {
        gc.setStroke(COLOR_LOCKED_TEXT);
        gc.setLineWidth(3);
        gc.strokeArc(cx - 6, cy - 14, 12, 20, 0, 180, ArcType.OPEN);   // shackle
        gc.setLineWidth(1);
        gc.setFill(COLOR_LOCKED_TEXT);
        gc.fillRoundRect(cx - 10, cy - 4, 20, 16, 4, 4);               // body
    }

    private int[] levelCell(int level) {
        if (level == LevelData.SPECIAL_LEVEL) {
            int y = GRID_Y + 2 * (CELL_H + CELL_GAP);
            return new int[]{centerX - 150, y, 300, 90};
        }
        int index = level - 1;
        int col = index % GRID_COLS;
        int row = index / GRID_COLS;
        return new int[]{GRID_X + col * (CELL_W + CELL_GAP), GRID_Y + row * (CELL_H + CELL_GAP), CELL_W, CELL_H};
    }

    // which level's cell is at this point (0 = none)
    public int levelAt(double x, double y) {
        for (int level = 1; level <= LevelData.TOTAL_LEVELS; level++) {
            if (hit(levelCell(level), x, y)) {
                return level;
            }
        }
        return 0;
    }

    private void drawTutorial(GraphicsContext gc) {
        TutorialManager tutorial = gameManager.getTutorial();

        drawBackground(gc);
        drawPlayfield(gc, tutorial.getPlayfield(), true);

        // instruction panel over the sky
        int panelX = centerX - 230;
        gc.setFill(COLOR_SCORE);
        gc.fillRoundRect(panelX, 15, 460, 210, 20, 20);

        gc.setFill(Color.YELLOW);
        drawCentered(gc, tutorial.getTitle(), bold(22), centerX, 48);

        Font stepFont = Font.font("Arial", FontWeight.NORMAL, 12);
        String stepText = tutorial.getStepNumber() + "/" + tutorial.getTotalSteps();
        gc.setFill(Color.WHITE);
        gc.setFont(stepFont);
        gc.fillText(stepText, panelX + 445 - textWidth(stepText, stepFont), 38);

        Font bodyFont = Font.font("Arial", FontWeight.NORMAL, 15);
        String[] lines = tutorial.getBodyLines();
        for (int i = 0; i < lines.length; i++) {
            drawCentered(gc, lines[i], bodyFont, centerX, 80 + i * 22);
        }

        String feedback = tutorial.getFeedback();
        if (!feedback.isEmpty()) {
            gc.setFill(tutorial.isFeedbackGood() ? COLOR_GOOD : COLOR_BAD);
            drawCentered(gc, feedback, bold(15), centerX, 170);
        } else {
            gc.setFill(Color.YELLOW);
            drawCentered(gc, tutorial.getPrompt(), bold(15), centerX, 170);
        }

        String buttonLabel = tutorial.getButtonLabel();
        if (!buttonLabel.isEmpty()) {
            drawButton(gc, BTN_TUTORIAL_NEXT, COLOR_GLASS, buttonLabel, bold(15), Color.WHITE, false);
        }

        drawButton(gc, BTN_BACK, Color.WHITE, "MENU", bold(14), COLOR_MISS, false);
    }

    private void drawGame(GraphicsContext gc) {
        Playfield field = gameManager.getPlayfield();
        GamePhase phase = gameManager.getPhase();
        boolean over = phase == GamePhase.GAME_OVER;
        boolean paused = phase == GamePhase.PAUSED;

        drawBackground(gc);
        drawPlayfield(gc, field, !over && !paused);   // game over / pause: the bins stay, the trash is hidden

        if (over) {
            drawGameOverScreen(gc);        // dims the scene first, so the HUD below stays bright
        }

        drawTurtle(gc);
        drawHud(gc);

        if (phase == GamePhase.RUNNING) {
            drawMessage(gc);
            drawButton(gc, BTN_PAUSE, Color.WHITE, "PAUSE", bold(14), COLOR_SCORE, false);
        } else if (phase == GamePhase.INTRO) {
            drawLevelIntro(gc);
        } else if (phase == GamePhase.LEVEL_COMPLETE) {
            drawLevelComplete(gc);
        } else if (paused) {
            drawPausedCard(gc);
        }
    }

    // fresh / trashed / super-trashed turtle by lives; dead turtle on game over
    private void drawTurtle(GraphicsContext gc) {
        int lives = gameManager.isGameOver() ? 0 : gameManager.getLives();
        Image turtle = assets.getTurtle(lives);
        if (turtle != null) {
            drawSprite(gc, turtle, TURTLE_X, TURTLE_Y, TURTLE_SIZE);
        }
    }

    private void drawHud(GraphicsContext gc) {
        double hudCenter = width - 125;

        gc.setFill(COLOR_SCORE);
        drawCentered(gc, "Score: " + gameManager.getScore(), fontBOLD, hudCenter, 30);
        drawCentered(gc, gameManager.getLevelLabel(), bold(14), hudCenter, 55);

        gc.setFill(COLOR_LIVES);
        drawCentered(gc, "Lives: " + gameManager.getLives(), fontBOLD, hudCenter, 80);

        gc.setFill(COLOR_SCORE);
        drawCentered(gc, gameManager.getProgressText(), bold(13), hudCenter, 104);
    }

    // short banner above the sea: "NEW: Coffee Cup!", "Wrong bin!", "Washed away!"
    private void drawMessage(GraphicsContext gc) {
        String message = gameManager.getMessage();
        if (message.isEmpty()) return;

        Font font = bold(16);
        int w = textWidth(message, font);
        gc.setFill(Color.rgb(18, 58, 82, 0.9));
        gc.fillRoundRect(centerX - w / 2.0 - 14, 192, w + 28, 34, 16, 16);
        gc.setFill(gameManager.isMessageGood() ? COLOR_GOOD : COLOR_BAD);
        drawCentered(gc, message, font, centerX, 215);
    }

    // ================= the sea: bins and floating trash =================

    private void drawPlayfield(GraphicsContext gc, Playfield field, boolean showTrash) {
        BinSlot hovered = field.getHoveredSlot();
        for (BinSlot slot : field.getBinSlots()) {
            drawBin(gc, slot, slot == hovered);
        }

        if (showTrash) {
            for (Trash trash : field.getItems()) {   // the piece being dragged is last, so it is on top
                drawTrash(gc, trash, trash == field.getDragged());
            }
        }
    }

    private void drawBin(GraphicsContext gc, BinSlot slot, boolean hovered) {
        int x = slot.getX();
        int y = slot.getY();
        int w = slot.getWidth();
        int h = slot.getHeight();

        Image art = assets.getBin(slot.getType());
        if (art != null) {
            gc.drawImage(art, x, y, w, h);
        } else {
            gc.setFill(COLOR_SHADE);
            gc.fillRoundRect(x + 3, y + 6, w, h, 18, 18);
            gc.setFill(getBinColor(slot.getType()));
            gc.fillRoundRect(x, y, w, h, 18, 18);
        }

        if (hovered) {
            gc.setFill(Color.rgb(255, 255, 255, 0.35));
            gc.fillRoundRect(x - 4, y - 30, w + 8, h + 34, 20, 20);
        }

        // name plate above the bin
        String label = slot.getType().getLabel();
        Font font = bold(12);
        int plateW = textWidth(label, font) + 16;
        gc.setFill(COLOR_SCORE);
        gc.fillRoundRect(x + w / 2.0 - plateW / 2.0, y - 24, plateW, 20, 10, 10);
        gc.setFill(Color.WHITE);
        drawCentered(gc, label, font, x + w / 2.0, y - 9);
    }

    private void drawTrash(GraphicsContext gc, Trash trash, boolean dragged) {
        double size = trash.getSize() * (dragged ? 1.15 : 1.0);

        if (dragged) {
            gc.setFill(COLOR_SHADE);
            gc.fillOval(trash.getX() - size / 2, trash.getY() + size / 2 - 4, size, 14);
        }

        drawKindSprite(gc, trash.getKind(), trash.getX(), trash.getY(), size);

        if (trash.isComposite()) {
            // purple badge = made of several pieces (double-click to split)
            double bx = trash.getX() + size / 2 - 8;
            double by = trash.getY() - size / 2 + 8;
            gc.setFill(COLOR_MISS);
            gc.fillOval(bx - 11, by - 11, 22, 22);
            gc.setFill(Color.WHITE);
            drawCentered(gc, String.valueOf(trash.getKind().getParts().size()), bold(13), bx, by + 5);
        }
    }

    // your picture if it exists, otherwise a colored circle with the name
    private void drawKindSprite(GraphicsContext gc, TrashKind kind, double cx, double cy, double size) {
        Image sprite = assets.getTrash(kind);
        if (sprite != null) {
            drawSprite(gc, sprite, cx, cy, size);
            return;
        }

        gc.setFill(kind.isComposite() ? COLOR_MISS : getBinColor(kind.getBin()));
        gc.fillOval(cx - size / 2, cy - size / 2, size, size);
        gc.setFill(Color.WHITE);
        drawCentered(gc, kind.getDisplayName(), bold(11), cx, cy + 4);
    }

    // ================= background =================

    private void drawBackground(GraphicsContext gc) {
        Image background = assets.getBackground();
        if (background != null) {
            gc.drawImage(background, 0, 0, width, height);
            return;
        }

        gc.setFill(COLOR_SKY);
        gc.fillRect(0, 0, width, 225);

        gc.setFill(COLOR_SEA);
        gc.fillRect(0, 225, width, height - 225);
    }

    // ================= cards over the game =================

    private void drawLevelIntro(GraphicsContext gc) {
        gc.setFill(COLOR_SCORE);
        gc.fillRoundRect(centerX - 220, 150, 440, 340, 24, 24);

        gc.setFill(Color.YELLOW);
        drawCentered(gc, gameManager.getLevelData().getTitle(), bold(34), centerX, 198);

        gc.setFill(Color.WHITE);
        drawCentered(gc, "NEW: " + gameManager.getLevelData().getNewKindsText(), bold(22), centerX, 235);

        List<TrashKind> featured = gameManager.getLevelData().getNewKinds();
        for (int i = 0; i < featured.size(); i++) {
            double cx = centerX + (i - (featured.size() - 1) / 2.0) * 140;
            drawKindSprite(gc, featured.get(i), cx, 315, 110);
        }

        Font hintFont = Font.font("Arial", FontWeight.NORMAL, 15);
        String[] hints = gameManager.getLevelData().getHintLines();
        for (int i = 0; i < hints.length; i++) {
            drawCentered(gc, hints[i], hintFont, centerX, 393 + i * 22);
        }

        drawButton(gc, BTN_INTRO_START, COLOR_GLASS, "START", bold(20), Color.WHITE, false);
    }

    private void drawLevelComplete(GraphicsContext gc) {
        gc.setFill(COLOR_SCORE);
        gc.fillRoundRect(centerX - 200, 220, 400, 230, 24, 24);

        gc.setFill(Color.YELLOW);
        drawCentered(gc, gameManager.getLevelData().getTitle() + " COMPLETE!", bold(28), centerX, 275);

        gc.setFill(Color.WHITE);
        drawCentered(gc, "Score: " + gameManager.getScore(), Font.font("Arial", FontWeight.NORMAL, 20), centerX, 315);

        if (gameManager.getLevel() == (LevelData.SPECIAL_LEVEL - 1)) {
            drawCentered(gc, "A special level is next!", Font.font("Arial", FontWeight.NORMAL, 14), centerX, 345);
        }

        drawButton(gc, BTN_COMPLETE_NEXT, COLOR_GLASS, "NEXT LEVEL", bold(18), Color.WHITE, false);
        drawButton(gc, BTN_COMPLETE_MENU, Color.WHITE, "LEVELS", bold(18), COLOR_PLASTIC, false);
    }

    // the card shown by ESC / the PAUSE button
    private void drawPausedCard(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 110 / 255.0));
        gc.fillRect(0, 0, width, height);

        gc.setFill(COLOR_SCORE);
        gc.fillRoundRect(centerX - 160, 205, 320, 255, 24, 24);

        gc.setFill(Color.YELLOW);
        drawCentered(gc, "PAUSED", bold(34), centerX, 252);

        drawButton(gc, BTN_PAUSE_RESUME, COLOR_GLASS, "RESUME", bold(20), Color.WHITE, false);
        drawButton(gc, BTN_PAUSE_RESTART, COLOR_PAPER, "RESTART", bold(20), COLOR_SCORE, false);
        drawButton(gc, BTN_PAUSE_MENU, Color.WHITE, "MENU", bold(20), COLOR_PLASTIC, false);

        gc.setFill(Color.WHITE);
        drawCentered(gc, "ESC: resume     R: restart     M: menu",
                Font.font("Arial", FontWeight.NORMAL, 13), centerX, 450);
    }

    // ================= game over =================

    private void drawGameOverScreen(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 100 / 255.0));
        gc.fillRect(0, 0, width, height);

        gc.setFill(COLOR_MISS);
        gc.fillRoundRect(centerX - 150, 200, 300, 490 - 225, 30, 30);

        if (gameManager.isVictory()) {
            gc.setFill(COLOR_GOOD);
            drawCentered(gc, "YOU WIN!", bold(36), centerX, 245);
        } else {
            gc.setFill(Color.RED);
            drawCentered(gc, "GAME OVER", bold(36), centerX, 245);
        }

        String where = gameManager.getMode() == GameMode.LEVELS ? "Level " : "Stage ";
        gc.setFill(Color.WHITE);
        int whereNumber = gameManager.getMode() == GameMode.LEVELS ? gameManager.getLevel() : gameManager.getStage();
        drawCentered(gc, "Score: " + gameManager.getScore() + "   " + where + whereNumber,
                Font.font("Arial", FontWeight.NORMAL, 20), centerX, 280);

        if (gameManager.isEnteringName()) {
            drawNameEntry(gc);
        } else {
            drawLeaderboard(gc);
        }
    }

    private void drawNameEntry(GraphicsContext gc) {
        gc.setFill(Color.WHITE);
        drawCentered(gc, "Enter your name:", Font.font("Arial", FontWeight.NORMAL, 20), centerX, 325);

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(centerX - 110, 340, 220, 36, 10, 10);

        boolean caretOn = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = gameManager.getNameInput() + (caretOn ? "_" : "");
        gc.setFill(COLOR_SCORE);
        gc.setFont(bold(20));
        gc.fillText(shown, centerX - 100, 365);

        gc.setFill(Color.WHITE);
        drawCentered(gc, "Press ENTER to confirm", Font.font("Arial", FontWeight.NORMAL, 14), centerX, 405);
    }

    private void drawLeaderboard(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        String heading = gameManager.getMode() == GameMode.LEVELS ? "TOP 3 - LEVELS" : "TOP 3 - UNLIMITED";
        drawCentered(gc, heading, bold(20), centerX, 312);

        Font plain18 = Font.font("Arial", FontWeight.NORMAL, 18);
        gc.setFill(Color.WHITE);
        gc.setFont(plain18);
        List<ScoreEntry> top = gameManager.getTopScores();
        for (int i = 0; i < top.size(); i++) {
            ScoreEntry entry = top.get(i);
            int rowY = 340 + i * 26;
            gc.fillText((i + 1) + ". " + entry.getName(), centerX - 120, rowY);

            String scoreText = String.valueOf(entry.getScore());
            gc.fillText(scoreText, centerX + 120 - textWidth(scoreText, plain18), rowY);
        }

        drawButton(gc, BTN_RESTART, Color.WHITE, "RESTART", bold(20), COLOR_PAPER, false);
        drawButton(gc, BTN_GAMEOVER_MENU, Color.WHITE, "MENU", bold(20), COLOR_PLASTIC, false);
    }

    // ================= buttons and hit-testing =================

    private void drawButton(GraphicsContext gc, int[] b, Color fill, String text,
                            Font font, Color textColor, boolean shadow) {
        double arc = b[3] > 50 ? 16 : 10;

        if (shadow) {
            gc.setFill(COLOR_SHADE);
            gc.fillRoundRect(b[0] + 4, b[1] + 6, b[2], b[3], arc, arc);
        }

        gc.setFill(fill);
        gc.fillRoundRect(b[0], b[1], b[2], b[3], arc, arc);

        gc.setFill(textColor);
        drawCentered(gc, text, font, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0 + font.getSize() * 0.36);
    }

    private boolean hit(int[] b, double x, double y) {
        return x >= b[0] && x <= b[0] + b[2] && y >= b[1] && y <= b[1] + b[3];
    }

    public boolean isLevelsButtonHit(double x, double y) {return hit(BTN_LEVELS, x, y);}
    public boolean isUnlimitedButtonHit(double x, double y) {return hit(BTN_UNLIMITED, x, y);}
    public boolean isTutorialButtonHit(double x, double y) {return hit(BTN_TUTORIAL, x, y);}
    public boolean isIntroStartHit(double x, double y) {return hit(BTN_INTRO_START, x, y);}
    public boolean isCompleteNextHit(double x, double y) {return hit(BTN_COMPLETE_NEXT, x, y);}
    public boolean isCompleteMenuHit(double x, double y) {return hit(BTN_COMPLETE_MENU, x, y);}
    public boolean isRestartButtonHit(double x, double y) {return hit(BTN_RESTART, x, y);}
    public boolean isGameOverMenuButtonHit(double x, double y) {return hit(BTN_GAMEOVER_MENU, x, y);}
    public boolean isBackButtonHit(double x, double y) {return hit(BTN_BACK, x, y);}
    public boolean isPauseHit(double x, double y) {return hit(BTN_PAUSE, x, y);}
    public boolean isPauseResumeHit(double x, double y) {return hit(BTN_PAUSE_RESUME, x, y);}
    public boolean isPauseRestartHit(double x, double y) {return hit(BTN_PAUSE_RESTART, x, y);}
    public boolean isPauseMenuHit(double x, double y) {return hit(BTN_PAUSE_MENU, x, y);}

    // the tutorial's NEXT / PLAY LEVELS button only exists on some steps
    public boolean isTutorialNextHit(double x, double y) {
        return !gameManager.getTutorial().getButtonLabel().isEmpty() && hit(BTN_TUTORIAL_NEXT, x, y);
    }

    // ================= helpers =================

    // draws a picture centered on (cx, cy), fitted inside a size x size box (keeps its proportions)
    private void drawSprite(GraphicsContext gc, Image image, double cx, double cy, double size) {
        double scale = size / Math.max(image.getWidth(), image.getHeight());
        double w = image.getWidth() * scale;
        double h = image.getHeight() * scale;
        gc.drawImage(image, cx - w / 2, cy - h / 2, w, h);
    }

    private Font bold(double size) {
        return Font.font("Arial", FontWeight.BOLD, size);
    }

    private void drawCentered(GraphicsContext gc, String s, Font font, double centerX, double baselineY) {
        gc.setFont(font);
        gc.fillText(s, centerX - textWidth(s, font) / 2.0, baselineY);
    }

    // replaces Swing's FontMetrics.stringWidth
    private int textWidth(String s, Font font) {
        Text t = new Text(s);
        t.setFont(font);
        return (int) Math.round(t.getLayoutBounds().getWidth());
    }

    private Color getBinColor(BinType type) {
        switch (type) {
            case PAPER: return COLOR_PAPER;
            case PLASTIC: return COLOR_PLASTIC;
            case GLASS: return COLOR_GLASS;
            case METAL: return COLOR_METAL;
            case ORGANIC: return COLOR_ORGANIC;
            case RECYCLABLE: return COLOR_RECYCLABLE;
            default: return COLOR_TIN;   // HAZARD
        }
    }
}