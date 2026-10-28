package engine;

import io.HighScoreManager;
import model.Trash;
import model.TrashSpawner;
import model.TrashType;

public class GameManager {
    private int displayScore = 0;
    private double currentFallingSpeed = 3.5;
    
    private final double MAX_FALL_SPEED = 9.0;
    private final double FRAME_ACCELERATION = 0.35;

    private int playerLives = 3;
    private TrashType leftFloaterType = TrashType.PAPER;
    private TrashType rightFloaterType = TrashType.PLASTIC;

    private boolean gameIsOver = false;
    private final TrashSpawner spawner;
    private Trash activeTrash;
    private final HighScoreManager highScoreManager;

    private final int FIXED_DROP_X = 250;
    private final int MISS_LINE_Y = 510;

    public GameManager(){
        this.spawner = new TrashSpawner();
        this.highScoreManager = new HighScoreManager();
        this.activeTrash = spawner.generateNextTrash(FIXED_DROP_X, leftFloaterType, rightFloaterType);
    }

    public void processFrameUpdate(double deltaTime){
        if(!gameIsOver){
            if(activeTrash != null){
                activeTrash.applyContinuousGravity(currentFallingSpeed);
                if(activeTrash.hasReachedMissLine(MISS_LINE_Y)){
                    handleMiss();
                }
            }
        }
    }

    public void executePlayerAction(String direction){
        if(!gameIsOver && activeTrash != null){
            TrashType targetedFloater = direction.equalsIgnoreCase("left") ? leftFloaterType : rightFloaterType;

            if(activeTrash.getType() == targetedFloater){
                displayScore += 1;

                
                if(displayScore > 0 && displayScore % 60 == 0){
                    leftFloaterType = TrashType.getRandomType();
                    rightFloaterType = TrashType.getRandomType();
                    while(leftFloaterType == rightFloaterType) {
                        rightFloaterType = TrashType.getRandomType();
                    }
                }else if(displayScore > 0 && displayScore % 20 == 0){
                    currentFallingSpeed += FRAME_ACCELERATION;
                    if(currentFallingSpeed > MAX_FALL_SPEED){
                        currentFallingSpeed = MAX_FALL_SPEED;
                    }
                    swapFloaters();
                }
            }else {handleMiss();}

            spawnNextTrash();
        }
    }

    private void handleMiss(){
        playerLives--;
        if(playerLives <= 0){
            this.gameIsOver = true;
            highScoreManager.saveIfRecords(displayScore);
        }else {spawnNextTrash();}
    }

    private void swapFloaters(){
        TrashType temp = leftFloaterType;
        leftFloaterType = rightFloaterType;
        rightFloaterType = temp;
    }

    private void spawnNextTrash(){
        activeTrash = null;
        if(!gameIsOver){
            activeTrash = spawner.generateNextTrash(FIXED_DROP_X, rightFloaterType, leftFloaterType);
        }

    }
    public Trash getActiveTrash() {return activeTrash;}
    public int getDisplayScore() {return displayScore;}
    public int getPlayerLives() {return playerLives;}
    public String getLeftFloaterLabel() {return leftFloaterType.toString();}
    public String getRightFloaterLabel() {return rightFloaterType.toString();}
    public boolean isGameOver() {return gameIsOver;}
    public int getMissLineY(){return MISS_LINE_Y;}
}
