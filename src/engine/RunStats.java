package engine;

import model.BinType;
import model.TrashKind;

import java.util.ArrayList;
import java.util.List;

// Everything the player did in one level (or one unlimited run): what went into each bin,
// what floated away, and what was sorted wrongly. Feeds the recap animation, which also uses
// the tick timeline at the bottom of this class (1 tick = one 16 ms game tick).
public class RunStats {
    public enum MistakeType {MISSED, WRONG_BIN, NOT_SPLIT}

    // one correctly sorted piece
    public static class SortEvent {
        private final TrashKind kind;
        private final BinType bin;

        private SortEvent(TrashKind kind, BinType bin){
            this.kind = kind;
            this.bin = bin;
        }

        public TrashKind getKind() {return kind;}
        public BinType getBin() {return bin;}
    }

    // one kind of mistake; repeats of the same mistake are merged into the count
    public static class Mistake {
        private final MistakeType type;
        private final TrashKind kind;
        private final BinType chosenBin;   // WRONG_BIN only: the bin the player dropped it in
        private int count = 1;

        private Mistake(MistakeType type, TrashKind kind, BinType chosenBin){
            this.type = type;
            this.kind = kind;
            this.chosenBin = chosenBin;
        }

        public MistakeType getType() {return type;}
        public TrashKind getKind() {return kind;}
        public BinType getChosenBin() {return chosenBin;}
        public int getCount() {return count;}
    }

    private final List<SortEvent> sorted = new ArrayList<>();
    private final List<Mistake> mistakes = new ArrayList<>();

    // ---- recording ----

    public void recordSorted(TrashKind kind, BinType bin){
        sorted.add(new SortEvent(kind, bin));
    }

    public void recordMissed(TrashKind kind){
        addMistake(MistakeType.MISSED, kind, null);
    }

    public void recordWrong(TrashKind kind, BinType chosenBin){
        addMistake(MistakeType.WRONG_BIN, kind, chosenBin);
    }

    public void recordNotSplit(TrashKind kind){
        addMistake(MistakeType.NOT_SPLIT, kind, null);
    }

    private void addMistake(MistakeType type, TrashKind kind, BinType chosenBin){
        for(Mistake m : mistakes){
            if(m.type == type && m.kind == kind && m.chosenBin == chosenBin){
                m.count++;
                return;
            }
        }
        mistakes.add(new Mistake(type, kind, chosenBin));
    }

    // ---- reading ----

    public List<SortEvent> getSorted() {return new ArrayList<>(sorted);}
    public List<Mistake> getMistakes() {return new ArrayList<>(mistakes);}
    public int getSortedCount() {return sorted.size();}

    public int getMissedCount(){
        int total = 0;
        for(Mistake m : mistakes){
            if(m.type == MistakeType.MISSED) total += m.count;
        }
        return total;
    }

    public int getWrongCount(){
        int total = 0;
        for(Mistake m : mistakes){
            if(m.type != MistakeType.MISSED) total += m.count;
        }
        return total;
    }

    // the mistake made most often (the first one on a tie): the one the recap teaches about
    public Mistake getTopMistake(){
        Mistake top = null;
        for(Mistake m : mistakes){
            if(top == null || m.count > top.count){
                top = m;
            }
        }
        return top;
    }

    // ---- recap timeline ----

    public static final int INPUT_DELAY = 30;        // clicks/keys are ignored this long, so the drop that ended the run can't skip it
    public static final int FLIGHT_TICKS = 22;       // how long a sorted piece takes to fly into its bin
    public static final int ROW_FADE_TICKS = 14;     // how long a mistake row takes to slide in
    public static final int MAX_MISTAKE_ROWS = 3;    // more kinds of mistakes than this are summed up as "+ N more"
    private static final int INTRO_TICKS = 25;

    // pieces are launched one after another; a long run speeds up so the recap stays short
    private int sortStep(){
        return Math.max(1, Math.min(14, 140 / Math.max(1, sorted.size())));
    }

    public int getLaunchTick(int index) {return INTRO_TICKS + index * sortStep();}
    public int getLandTick(int index) {return getLaunchTick(index) + FLIGHT_TICKS;}

    public int getSortedEndTick(){
        return sorted.isEmpty() ? INTRO_TICKS : getLandTick(sorted.size() - 1);
    }

    public int getMistakeRows() {return Math.min(MAX_MISTAKE_ROWS, mistakes.size());}
    public int getMistakeTick(int row) {return getSortedEndTick() + 15 + row * 20;}
    public int getLessonTick() {return getMistakeTick(Math.max(1, getMistakeRows())) + 5;}
    public int getTotalTicks() {return getLessonTick() + 45;}
}