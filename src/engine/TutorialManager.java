package engine;

import model.BinType;
import model.Trash;
import model.TrashKind;
import model.TrashSpawner;

import java.util.Arrays;
import java.util.List;

// A guided sample game on its own little sea. Nothing here costs lives or touches the scores.
//   intro -> drag paper -> drag plastic -> split the coffee cup -> sort its pieces -> practice -> done
public class TutorialManager {
    private final int STEP_INTRO = 0;
    private final int STEP_DRAG_PAPER = 1;
    private final int STEP_DRAG_PLASTIC = 2;
    private final int STEP_SPLIT = 3;
    private final int STEP_SORT_PARTS = 4;
    private final int STEP_PRACTICE = 5;
    private final int STEP_DONE = 6;
    private final int TOTAL_STEPS = 7;

    private final int PRACTICE_GOAL = 4;
    private final double PRACTICE_SPEED = 1.2;
    private final int PRACTICE_MAX_ON_SCREEN = 2;
    private final int FEEDBACK_TICKS = 120;
    private final double LESSON_X = GameConfig.WINDOW_WIDTH / 2.0;
    private final double LESSON_Y = 370;

    private final List<TrashKind> practicePool = Arrays.asList(
            TrashKind.PAPER_TRASH, TrashKind.PLASTIC_TRASH, TrashKind.WINE_BOTTLE, TrashKind.CAN);

    private final Playfield field = new Playfield(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
    private final TrashSpawner spawner = new TrashSpawner();

    private int step = STEP_INTRO;
    private int practiceCorrect = 0;
    private int spawnTimer = 0;
    private String feedback = "";
    private boolean feedbackGood = true;
    private int feedbackTicks = 0;

    public void start(){
        practiceCorrect = 0;
        feedbackTicks = 0;
        goToStep(STEP_INTRO);
    }

    // ================= one tick =================

    public void update(){
        if(feedbackTicks > 0){
            feedbackTicks--;
        }

        int washedAway = field.update();

        if(step == STEP_PRACTICE){
            if(washedAway > 0){
                setFeedback("Washed away! That costs a life in the real game.", false);
            }
            if(spawnTimer > 0){
                spawnTimer--;
            }else if(field.size() < PRACTICE_MAX_ON_SCREEN){
                TrashKind kind = spawner.pickKind(practicePool, practicePool);
                Trash trash = spawner.createTrash(kind, GameConfig.WINDOW_WIDTH, PRACTICE_SPEED,
                        GameConfig.BAND_TOP, GameConfig.BAND_BOTTOM);
                if(field.hasRoomFor(trash)){
                    field.add(trash);
                    spawnTimer = 80;
                }
            }
        }
    }

    // ENTER or the NEXT button: only the intro needs it (the last step is handled by GameManager)
    public void confirm(){
        if(step == STEP_INTRO){
            goToStep(STEP_DRAG_PAPER);
        }
    }

    // ================= mouse =================

    public void mousePressed(double x, double y, int clickCount){
        if(!isInteractive()) return;

        Playfield.PressResult result = field.press(x, y, clickCount);
        if(result == Playfield.PressResult.SPLIT && step == STEP_SPLIT){
            setFeedback("Split! Now drag each piece to its bin.", true);
            goToStep(STEP_SORT_PARTS);
        }
    }

    public void mouseDragged(double x, double y){
        if(isInteractive()){
            field.dragTo(x, y);
        }
    }

    public void mouseReleased(){
        if(!isInteractive()) return;

        switch(field.release()){
            case CORRECT:
                onCorrect();
                break;
            case WRONG:
                if(step == STEP_PRACTICE){
                    setFeedback("Wrong bin! That costs a life in the real game.", false);
                }else{
                    setFeedback("Not that bin - read the bin names and try again.", false);
                }
                break;
            case NEEDS_SPLIT:
                setFeedback("Double-click it first to split it into pieces.", false);
                break;
            default:
                break;
        }
    }

    private void onCorrect(){
        if(step == STEP_DRAG_PAPER || step == STEP_DRAG_PLASTIC){
            setFeedback("Correct!", true);
            goToStep(step + 1);
        }else if(step == STEP_SORT_PARTS){
            if(field.isEmpty()){
                setFeedback("Correct! Cup sorted.", true);
                goToStep(STEP_PRACTICE);
            }else{
                setFeedback("Correct! One more piece.", true);
            }
        }else if(step == STEP_PRACTICE){
            practiceCorrect++;
            if(practiceCorrect >= PRACTICE_GOAL){
                setFeedback("Correct! Practice complete.", true);
                goToStep(STEP_DONE);
            }else{
                setFeedback("Correct!", true);
            }
        }
    }

    private boolean isInteractive(){
        return step != STEP_INTRO && step != STEP_DONE;
    }

    // ================= steps =================

    private void goToStep(int newStep){
        step = newStep;

        if(newStep != STEP_SORT_PARTS){
            field.clear();   // the split pieces must survive into the "sort the pieces" step
        }
        field.setRemoveOnWrong(step == STEP_PRACTICE);

        if(step == STEP_INTRO || step == STEP_DRAG_PAPER || step == STEP_DRAG_PLASTIC){
            field.setBins(Arrays.asList(BinType.PAPER, BinType.PLASTIC));
        }

        if(step == STEP_DRAG_PAPER){
            field.add(spawner.createTrashAt(TrashKind.PAPER_TRASH, LESSON_X, LESSON_Y, 0));
        }else if(step == STEP_DRAG_PLASTIC){
            field.add(spawner.createTrashAt(TrashKind.PLASTIC_TRASH, LESSON_X, LESSON_Y, 0));
        }else if(step == STEP_SPLIT){
            field.setBins(Arrays.asList(BinType.PLASTIC, BinType.RECYCLABLE));
            field.add(spawner.createTrashAt(TrashKind.COFFEE_CUP, LESSON_X, LESSON_Y, 0));
        }else if(step == STEP_PRACTICE){
            field.setBins(Arrays.asList(BinType.PAPER, BinType.PLASTIC, BinType.GLASS, BinType.METAL));
            practiceCorrect = 0;
            spawnTimer = 20;
        }else if(step == STEP_DONE){
            field.setBins(Arrays.asList(BinType.PAPER, BinType.PLASTIC, BinType.GLASS, BinType.METAL));
        }
    }

    private void setFeedback(String text, boolean good){
        feedback = text;
        feedbackGood = good;
        feedbackTicks = FEEDBACK_TICKS;
    }

    public boolean isFinished(){
        return step == STEP_DONE;
    }

    // ================= text the screen shows =================

    public String getTitle(){
        if(step == STEP_INTRO) return "WELCOME, SWEEPER!";
        if(step == STEP_DRAG_PAPER) return "SORTING BY DRAGGING";
        if(step == STEP_DRAG_PLASTIC) return "ONE MORE MATERIAL";
        if(step == STEP_SPLIT) return "MULTI-MATERIAL TRASH";
        if(step == STEP_SORT_PARTS) return "SORT EACH PIECE";
        if(step == STEP_PRACTICE) return "PRACTICE ROUND  " + practiceCorrect + "/" + PRACTICE_GOAL;
        return "YOU'RE READY!";
    }

    public String[] getBodyLines(){
        if(step == STEP_INTRO){
            return new String[]{
                "Trash washes in from the left and right of the sea.",
                "Drag each piece to the bin with its material.",
                "Pieces that float off the other side cost a life!"
            };
        }
        if(step == STEP_DRAG_PAPER){
            return new String[]{
                "Press and hold a piece, drag it onto a bin,",
                "then let go. Drag the PAPER piece to the PAPER bin."
            };
        }
        if(step == STEP_DRAG_PLASTIC){
            return new String[]{
                "A wrong bin costs a life in the real game.",
                "Drag the PLASTIC piece to the PLASTIC bin."
            };
        }
        if(step == STEP_SPLIT){
            return new String[]{
                "This cup is made of different materials.",
                "A purple badge shows how many pieces it has.",
                "Double-click it to split it apart!"
            };
        }
        if(step == STEP_SORT_PARTS){
            return new String[]{
                "Drag each piece to its own bin:",
                "the lid is PLASTIC, the cup is RECYCLABLE."
            };
        }
        if(step == STEP_PRACTICE){
            return new String[]{
                "Pieces float by on their own now - sort " + PRACTICE_GOAL + " of them.",
                "Drop them in the right bin before they drift away!"
            };
        }
        return new String[]{
            "Wrong bins and washed-away trash cost lives.",
            "Levels add new trash, new bins and more speed.",
            "Unlimited mode never ends - how long can you last?"
        };
    }

    public String getPrompt(){
        if(step == STEP_INTRO) return "Click NEXT or press ENTER     (ESC: menu)";
        if(step == STEP_DONE) return "Click PLAY LEVELS or press ENTER     (ESC: menu)";
        if(step == STEP_SPLIT) return "Double-click the cup";
        return "Drag the pieces onto the bins";
    }

    // the panel shows a button on these two steps only
    public String getButtonLabel(){
        if(step == STEP_INTRO) return "NEXT";
        if(step == STEP_DONE) return "PLAY LEVELS";
        return "";
    }

    public String getFeedback() {return feedbackTicks > 0 ? feedback : "";}
    public boolean isFeedbackGood() {return feedbackGood;}
    public int getStepNumber() {return step + 1;}
    public int getTotalSteps() {return TOTAL_STEPS;}
    public Playfield getPlayfield() {return field;}
}