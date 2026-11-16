package engine;

import io.HighScoreManager;
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
    private final int PIECES_PER_STAGE = 10;          // unlimited: correct sorts before the next stage
    private final double UNLIMITED_SPEED_RAMP = 0.01; // unlimited: extra speed per correct sort
    private final double UNLIMITED_MAX_SPEED = 4.0;
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

    private int level = 1;                 // levels mode: the level; unlimited: the current stage
    private LevelData levelData = LevelData.get(1);
    private int score = 0;
    private int lives = START_LIVES;
    private int correctCount = 0;
    private int spawnedThisLevel = 0;
    private int spawnTimer = 0;

    private boolean victory = false;
    private boolean enteringName = false;
    private final StringBuilder nameInput = new StringBuilder();
    private List<ScoreEntry> topScores = new ArrayList<>();

    private String message = "";
    private boolean messageGood = true;
    private int messageTicks = 0;

    // ================= screens =================

    public void startLevels(){
        mode = GameMode.LEVELS;
        score = 0;
        level = 1;
        state = GameState.PLAYING;
        beginLevel();
    }

    public void startUnlimited(){
        mode = GameMode.UNLIMITED;
        score = 0;
        level = 1;
        correctCount = 0;
        state = GameState.PLAYING;

        levelData = LevelData.get(level);
        resetRound();
        phase = GamePhase.RUNNING;
        announceNewTrash();
    }

    public void startTutorial(){
        tutorial.start();
        state = GameState.TUTORIAL;
    }

    // from the tutorial any time; from a run unless the name prompt is open
    public void returnToMenu(){
        if(state == GameState.TUTORIAL || (state == GameState.PLAYING && !enteringName)){
            state = GameState.MENU;
        }
    }

    public void tutorialConfirm(){
        if(state != GameState.TUTORIAL) return;

        if(tutorial.isFinished()){
            startLevels();
        }else{
            tutorial.confirm();
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

        playfield.clear();
        playfield.setBins(levelData.getBins());
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

    // ================= one tick =================

    public void processFrameUpdate(double deltaTime){
        if(state == GameState.TUTORIAL){
            tutorial.update();
            return;
        }
        if(state != GameState.PLAYING || phase != GamePhase.RUNNING) return;

        if(messageTicks > 0){
            messageTicks--;
        }

        int washedAway = playfield.update();
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
        if(playfield.size() >= levelData.getMaxOnScreen()) return;

        TrashKind kind = spawner.pickKind(levelData.getPool(), levelData.getNewKinds());
        Trash trash = spawner.createTrash(kind, GameConfig.WINDOW_WIDTH, currentSpeed(),
                GameConfig.BAND_TOP, GameConfig.BAND_BOTTOM);

        if(playfield.hasRoomFor(trash)){
            playfield.add(trash);
            spawnedThisLevel++;
            spawnTimer = Math.max(35, 95 - 5 * level);
        }
    }

    private double currentSpeed(){
        if(mode == GameMode.UNLIMITED){
            return Math.min(UNLIMITED_MAX_SPEED, levelData.getSpeed() + UNLIMITED_SPEED_RAMP * correctCount);
        }
        return levelData.getSpeed();
    }

    private void checkLevelComplete(){
        if(mode != GameMode.LEVELS) return;

        if(spawnedThisLevel >= levelData.getQuota() && playfield.isEmpty()){
            if(level >= LevelData.TOTAL_LEVELS){
                endRun(true);
            }else{
                phase = GamePhase.LEVEL_COMPLETE;
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
        switch(result){
            case CORRECT:
                score += POINTS_PER_PIECE;
                correctCount++;
                checkStageUp();
                break;
            case WRONG:
                loseLife("Wrong bin! You lost a life.");
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

        int newStage = Math.min(LevelData.TOTAL_LEVELS, 1 + correctCount / PIECES_PER_STAGE);
        if(newStage > level){
            level = newStage;
            levelData = LevelData.get(level);
            playfield.setBins(levelData.getBins());
            announceNewTrash();
        }
    }

    private void announceNewTrash(){
        setMessage("NEW: " + levelData.getNewKindsText() + "!", true);
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
        phase = GamePhase.GAME_OVER;
        enteringName = true;
        nameInput.setLength(0);
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

    public boolean canRestart(){
        return state == GameState.PLAYING && phase == GamePhase.GAME_OVER && !enteringName;
    }

    // starts the same mode again from the beginning
    public void restart(){
        if(!canRestart()) return;

        if(mode == GameMode.LEVELS){
            startLevels();
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
    public int getScore() {return score;}
    public int getLives() {return lives;}
    public boolean isGameOver() {return phase == GamePhase.GAME_OVER;}
    public boolean isVictory() {return victory;}
    public boolean isEnteringName() {return enteringName;}
    public String getNameInput() {return nameInput.toString();}
    public List<ScoreEntry> getTopScores() {return new ArrayList<>(topScores);}
    public String getMessage() {return messageTicks > 0 ? message : "";}
    public boolean isMessageGood() {return messageGood;}

    public String getLevelLabel(){
        return mode == GameMode.LEVELS ? levelData.getTitle() : "UNLIMITED - STAGE " + level;
    }

    public String getProgressText(){
        if(mode == GameMode.LEVELS){
            return "Pieces: " + Math.min(spawnedThisLevel, levelData.getQuota()) + "/" + levelData.getQuota();
        }
        return "Sorted: " + correctCount;
    }
}