package engine;

import io.HighScoreManager;
import io.ProgressManager;
import model.BinType;
import model.ScoreEntry;
import model.Trash;
import model.TrashKind;
import model.TrashSpawner;

import java.util.ArrayList;
import java.util.List;

// The rules: modes, levels, score, lives, spawning, and what a drop on a bin means.
public class GameManager {
    private final int START_LIVES = 3;
    private final int POINTS_PER_PIECE = 10;
    // ---- unlimited mode: its own tuning, independent of the level table ----
    // Only the trash introduction order (pool, bins, "new" pieces per stage) is shared with LevelData.
    private final int PIECES_PER_STAGE = 10;                 // correct sorts before the next stage
    private final double UNLIMITED_START_SPEED = 1.2;        // float speed at the very start
    private final double UNLIMITED_SPEED_RAMP = 0.01;        // extra speed per correct sort
    private final double UNLIMITED_MAX_SPEED = 4.0;
    private final int UNLIMITED_START_ON_SCREEN = 2;         // most pieces at once on stage 1
    private final int UNLIMITED_MAX_ON_SCREEN = 4;
    private final int UNLIMITED_STAGES_PER_EXTRA_PIECE = 3;  // one more piece allowed every N stages
    private final int UNLIMITED_SPAWN_DELAY_START = 95;      // ticks between pieces on stage 1
    private final int UNLIMITED_SPAWN_DELAY_STEP = 5;        // shorter by this much per stage
    private final int UNLIMITED_SPAWN_DELAY_MIN = 35;
    private final int MESSAGE_TICKS = 150;
    private final int MAX_NAME_LENGTH = 10;
    private final int TOP_SCORE_COUNT = 3;

    private GameState state = GameState.MENU;
    private GameMode mode = GameMode.LEVELS;
    private GamePhase phase = GamePhase.RUNNING;

    private final Playfield playfield = new Playfield(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
    private final TrashSpawner spawner = new TrashSpawner();
    private final TutorialManager tutorial = new TutorialManager();
    private final HighScoreManager levelScores = new HighScoreManager("highscore-levels.txt");
    private final HighScoreManager unlimitedScores = new HighScoreManager("highscore-unlimited.txt");
    private final ProgressManager progress = new ProgressManager("progress.txt", LevelData.TOTAL_LEVELS);

    private int cleared;                   // how many levels have been cleared (level N is open when N <= cleared + 1)
    private int level = 1;                 // levels mode only: the level being played
    private int stage = 1;                 // unlimited mode only: the current stage
    private LevelData levelData = LevelData.get(1);
    private int score = 0;
    private int lives = START_LIVES;
    private int correctCount = 0;
    private int spawnedThisLevel = 0;
    private int spawnTimer = 0;

    private RunStats stats = new RunStats();   // what happened this level (or this whole unlimited run)
    private int recapTicks = 0;                // how far the recap animation has got
    private boolean recapEndsRun = false;      // true: the game-over screen follows the recap
    private String recapLesson = "";           // the text in the "did you know?" box

    private boolean victory = false;
    private boolean enteringName = false;
    private final StringBuilder nameInput = new StringBuilder();
    private List<ScoreEntry> topScores = new ArrayList<>();

    private String message = "";
    private boolean messageGood = true;
    private int messageTicks = 0;

    public GameManager(){
        cleared = progress.loadCleared();
    }

    // ================= screens =================

    // the grid of levels (from the menu, the tutorial, or the level-complete card)
    public void openLevelSelect(){
        if(state == GameState.PLAYING && enteringName) return;
        state = GameState.LEVEL_SELECT;
    }

    // starts one specific level from the grid; locked levels are refused here, not just greyed out on screen
    public void startLevel(int number){
        if(!isUnlocked(number)) return;

        mode = GameMode.LEVELS;
        score = 0;
        level = number;
        state = GameState.PLAYING;
        beginLevel();
    }

    public void startUnlimited(){
        mode = GameMode.UNLIMITED;
        score = 0;
        stage = 1;
        correctCount = 0;
        state = GameState.PLAYING;

        resetRound();
        phase = GamePhase.RUNNING;
        announceNewTrash();
    }

    public void startTutorial(){
        tutorial.start();
        state = GameState.TUTORIAL;
    }

    // from the tutorial, the grid, or a run (unless the name prompt is open)
    public void returnToMenu(){
        if(state == GameState.TUTORIAL || state == GameState.LEVEL_SELECT
                || (state == GameState.PLAYING && !enteringName)){
            state = GameState.MENU;
        }
    }

    // leaving a run quietly (ESC on a card): back to the grid in levels mode, the menu in unlimited
    public void exitRun(){
        if(state != GameState.PLAYING || enteringName) return;
        state = (mode == GameMode.LEVELS) ? GameState.LEVEL_SELECT : GameState.MENU;
    }

    public void tutorialConfirm(){
        if(state != GameState.TUTORIAL) return;

        if(tutorial.isFinished()){
            openLevelSelect();
        }else{
            tutorial.confirm();
        }
    }

    // ================= progress =================

    public boolean isUnlocked(int number){
        return number >= 1 && number <= Math.min(LevelData.TOTAL_LEVELS, cleared + 1);
    }

    public boolean isCleared(int number){
        return number >= 1 && number <= cleared;
    }

    public int getClearedCount() {return cleared;}

    // only ever goes up, so replaying an old level can't lock anything again
    private void recordCleared(int number){
        if(number > cleared){
            cleared = Math.min(LevelData.TOTAL_LEVELS, number);
            progress.saveCleared(cleared);
        }
    }

    // ================= levels =================

    private void beginLevel(){
        levelData = LevelData.get(level);
        resetRound();
        phase = GamePhase.INTRO;
    }

    private void resetRound(){
        lives = START_LIVES;
        spawnedThisLevel = 0;
        spawnTimer = 30;
        victory = false;
        enteringName = false;
        nameInput.setLength(0);
        topScores = new ArrayList<>();
        clearMessage();
        stats = new RunStats();

        playfield.clear();
        playfield.setBins(currentBins());
    }

    public void confirmIntro(){
        if(state == GameState.PLAYING && phase == GamePhase.INTRO){
            phase = GamePhase.RUNNING;
        }
    }

    public void nextLevel(){
        if(state == GameState.PLAYING && phase == GamePhase.LEVEL_COMPLETE){
            level++;
            beginLevel();
        }
    }

    // ================= pause =================

    public void pause(){
        if(state == GameState.PLAYING && phase == GamePhase.RUNNING){
            playfield.cancelDrag();   // don't leave a piece glued to the mouse
            phase = GamePhase.PAUSED;
        }
    }

    public void resume(){
        if(state == GameState.PLAYING && phase == GamePhase.PAUSED){
            phase = GamePhase.RUNNING;
        }
    }

    public boolean isPaused() {return phase == GamePhase.PAUSED;}

    // ================= one tick =================

    public void processFrameUpdate(double deltaTime){
        if(state == GameState.TUTORIAL){
            tutorial.update();
            return;
        }
        if(state == GameState.PLAYING && phase == GamePhase.RECAP){
            recapTicks++;
            return;
        }
        // INTRO, PAUSED, LEVEL_COMPLETE and GAME_OVER all stop here, so a pause freezes everything
        if(state != GameState.PLAYING || phase != GamePhase.RUNNING) return;

        if(messageTicks > 0){
            messageTicks--;
        }

        int washedAway = playfield.update();
        for(Trash missed : playfield.getWashedAway()){
            stats.recordMissed(missed.getKind());
        }
        for(int i = 0; i < washedAway && phase == GamePhase.RUNNING; i++){
            loseLife("Washed away! You lost a life.");
        }
        if(phase != GamePhase.RUNNING) return;

        spawnStep();
        checkLevelComplete();
    }

    private void spawnStep(){
        if(mode == GameMode.LEVELS && spawnedThisLevel >= levelData.getQuota()) return;

        if(spawnTimer > 0){
            spawnTimer--;
            if(playfield.isEmpty() && spawnTimer > 15){
                spawnTimer = 15;   // don't make the player stare at an empty sea
            }
            return;
        }
        if(playfield.size() >= maxOnScreen()) return;

        TrashKind kind = spawner.pickKind(currentPool(), currentFeatured());
        Trash trash = spawner.createTrash(kind, GameConfig.WINDOW_WIDTH, currentSpeed(),
                GameConfig.BAND_TOP, GameConfig.BAND_BOTTOM);

        if(playfield.hasRoomFor(trash)){
            playfield.add(trash);
            spawnedThisLevel++;
            spawnTimer = spawnDelay();
        }
    }

    // unlimited: starts at UNLIMITED_START_SPEED and ramps per correct sort, whatever the stage
    private double currentSpeed(){
        if(mode == GameMode.UNLIMITED){
            return Math.min(UNLIMITED_MAX_SPEED, UNLIMITED_START_SPEED + UNLIMITED_SPEED_RAMP * correctCount);
        }
        return levelData.getSpeed();
    }

    // what can appear right now: levels read the level table, unlimited reads its own stages
    private List<TrashKind> currentPool(){
        return mode == GameMode.UNLIMITED ? UnlimitedData.getPool(stage) : levelData.getPool();
    }

    private List<TrashKind> currentFeatured(){
        return mode == GameMode.UNLIMITED ? UnlimitedData.getNewKinds(stage) : levelData.getNewKinds();
    }

    private List<BinType> currentBins(){
        return mode == GameMode.UNLIMITED ? UnlimitedData.getBins(stage) : levelData.getBins();
    }

    private String currentNewKindsText(){
        return mode == GameMode.UNLIMITED ? UnlimitedData.getNewKindsText(stage) : levelData.getNewKindsText();
    }

    private int maxOnScreen(){
        if(mode == GameMode.UNLIMITED){
            return Math.min(UNLIMITED_MAX_ON_SCREEN,
                    UNLIMITED_START_ON_SCREEN + (stage - 1) / UNLIMITED_STAGES_PER_EXTRA_PIECE);
        }
        return levelData.getMaxOnScreen();
    }

    private int spawnDelay(){
        if(mode == GameMode.UNLIMITED){
            return Math.max(UNLIMITED_SPAWN_DELAY_MIN,
                    UNLIMITED_SPAWN_DELAY_START - UNLIMITED_SPAWN_DELAY_STEP * stage);
        }
        return Math.max(35, 95 - 5 * level);
    }

    private void checkLevelComplete(){
        if(mode != GameMode.LEVELS) return;

        if(spawnedThisLevel >= levelData.getQuota() && playfield.isEmpty()){
            recordCleared(level);   // unlocks the next level (and is saved right away)
            if(level >= LevelData.TOTAL_LEVELS){
                endRun(true);
            }else{
                beginRecap(false);
            }
        }
    }

    // ================= mouse =================

    public void mousePressed(double x, double y, int clickCount){
        if(state == GameState.TUTORIAL){
            tutorial.mousePressed(x, y, clickCount);
        }else if(state == GameState.PLAYING && phase == GamePhase.RUNNING){
            playfield.press(x, y, clickCount);
        }
    }

    public void mouseDragged(double x, double y){
        if(state == GameState.TUTORIAL){
            tutorial.mouseDragged(x, y);
        }else if(state == GameState.PLAYING && phase == GamePhase.RUNNING){
            playfield.dragTo(x, y);
        }
    }

    public void mouseReleased(double x, double y){
        if(state == GameState.TUTORIAL){
            tutorial.mouseReleased();
        }else if(state == GameState.PLAYING && phase == GamePhase.RUNNING){
            handleDrop(playfield.release());
        }
    }

    private void handleDrop(Playfield.DropResult result){
        Trash dropped = playfield.getLastDropped();
        BinType droppedBin = playfield.getLastDropBin();

        switch(result){
            case CORRECT:
                if(dropped != null){
                    stats.recordSorted(dropped.getKind(), droppedBin);
                }
                score += POINTS_PER_PIECE;
                correctCount++;
                checkStageUp();
                break;
            case WRONG:
                if(dropped != null){
                    stats.recordWrong(dropped.getKind(), droppedBin);
                }
                loseLife("Wrong bin! You lost a life.");
                break;
            case UNSPLIT:
                if(dropped != null){
                    stats.recordNotSplit(dropped.getKind());
                }
                loseLife("Split it first! You lost a life.");
                break;
            case NEEDS_SPLIT:
                setMessage("Double-click it first to split it into pieces!", false);
                break;
            default:
                break;
        }
    }

    // unlimited mode: every PIECES_PER_STAGE correct sorts the next stage's trash and bin arrive
    private void checkStageUp(){
        if(mode != GameMode.UNLIMITED) return;

        int newStage = Math.min(UnlimitedData.LAST_STAGE, 1 + correctCount / PIECES_PER_STAGE);
        if(newStage > stage){
            stage = newStage;
            playfield.setBins(currentBins());
            announceNewTrash();
        }
    }

    private void announceNewTrash(){
        setMessage("NEW: " + currentNewKindsText() + "!", true);
    }

    private void loseLife(String reason){
        lives--;
        setMessage(reason, false);
        if(lives <= 0){
            endRun(false);
        }
    }

    private void endRun(boolean won){
        victory = won;
        beginRecap(true);
    }

    // ================= recap =================

    // after a level or a game over: an animation of what was sorted, what went wrong, and a lesson
    private void beginRecap(boolean endsRun){
        phase = GamePhase.RECAP;
        recapTicks = 0;
        recapEndsRun = endsRun;
        enteringName = false;   // the name prompt only opens once the recap is dismissed
        recapLesson = stats.getMistakes().isEmpty()
                ? EcoFacts.praiseFor(stats)
                : EcoFacts.lessonFor(stats.getTopMistake());
    }

    public boolean isRecapDone(){
        return phase == GamePhase.RECAP && recapTicks >= stats.getTotalTicks();
    }

    // first click / ENTER: jump to the end of the animation
    public void skipRecap(){
        if(state == GameState.PLAYING && phase == GamePhase.RECAP
                && recapTicks >= RunStats.INPUT_DELAY && !isRecapDone()){
            recapTicks = stats.getTotalTicks();
        }
    }

    // once the animation is finished: on to the level-complete card or the game-over screen
    public void continueRecap(){
        if(state == GameState.PLAYING && isRecapDone() && recapTicks >= RunStats.INPUT_DELAY){
            if(recapEndsRun){
                phase = GamePhase.GAME_OVER;
                enteringName = true;
                nameInput.setLength(0);
            }else{
                phase = GamePhase.LEVEL_COMPLETE;
            }
        }
    }

    // ENTER / ESC: skip the animation, or continue if it has already finished
    public void advanceRecap(){
        if(isRecapDone()){
            continueRecap();
        }else{
            skipRecap();
        }
    }

    // ================= name entry and leaderboard =================

    public void typeNameCharacter(char c){
        if(!enteringName || nameInput.length() >= MAX_NAME_LENGTH) return;

        if(Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-'){
            nameInput.append(Character.toUpperCase(c));
        }
    }

    public void deleteNameCharacter(){
        if(enteringName && nameInput.length() > 0){
            nameInput.deleteCharAt(nameInput.length() - 1);
        }
    }

    public void submitName(){
        if(!enteringName) return;

        String name = nameInput.toString().trim();
        if(name.isEmpty()){
            name = "PLAYER";
        }

        HighScoreManager scores = (mode == GameMode.LEVELS) ? levelScores : unlimitedScores;
        scores.saveScore(name, score);
        topScores = scores.getTopScores(TOP_SCORE_COUNT);
        enteringName = false;
    }

    // restart is offered on the game-over screen (after the name is entered) and on the pause card
    public boolean canRestart(){
        if(state != GameState.PLAYING) return false;
        return phase == GamePhase.PAUSED || (phase == GamePhase.GAME_OVER && !enteringName);
    }

    // starts the same level (levels mode) or a fresh unlimited run from the beginning
    public void restart(){
        if(!canRestart()) return;

        if(mode == GameMode.LEVELS){
            startLevel(level);
        }else{
            startUnlimited();
        }
    }

    // ================= messages =================

    private void setMessage(String text, boolean good){
        message = text;
        messageGood = good;
        messageTicks = MESSAGE_TICKS;
    }

    private void clearMessage(){
        message = "";
        messageTicks = 0;
    }

    // ================= what the screen reads =================

    public GameState getState() {return state;}
    public GameMode getMode() {return mode;}
    public GamePhase getPhase() {return phase;}
    public Playfield getPlayfield() {return playfield;}
    public TutorialManager getTutorial() {return tutorial;}
    public LevelData getLevelData() {return levelData;}
    public int getLevel() {return level;}
    public int getStage() {return stage;}
    public int getScore() {return score;}
    public int getLives() {return lives;}
    public boolean isGameOver() {return phase == GamePhase.GAME_OVER;}
    public boolean isVictory() {return victory;}
    public boolean isEnteringName() {return enteringName;}
    public String getNameInput() {return nameInput.toString();}
    public List<ScoreEntry> getTopScores() {return new ArrayList<>(topScores);}
    public String getMessage() {return messageTicks > 0 ? message : "";}
    public boolean isMessageGood() {return messageGood;}

    // the recap screen
    public RunStats getStats() {return stats;}
    public int getRecapTicks() {return recapTicks;}
    public String getRecapLesson() {return recapLesson;}
    public List<BinType> getRecapBins() {return currentBins();}

    public String getRecapTitle(){
        if(recapEndsRun){
            return victory ? "YOU WIN!" : "GAME OVER";
        }
        return levelData.getTitle() + " COMPLETE!";
    }

    public String getLevelLabel(){
        return mode == GameMode.LEVELS ? levelData.getTitle() : "UNLIMITED - STAGE " + stage;
    }

    public String getProgressText(){
        if(mode == GameMode.LEVELS){
            return "Pieces: " + Math.min(spawnedThisLevel, levelData.getQuota()) + "/" + levelData.getQuota();
        }
        return "Sorted: " + correctCount;
    }
}