package engine;

import model.BinType;
import model.TrashKind;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// The ten levels plus the special level. Everything about a level is one line in the table below:
// number, float speed, most pieces on screen at once, pieces to sort, hint text, and the trash introduced.
// A level's trash pool is everything introduced up to and including it.
public class LevelData {
    public static final int TOTAL_LEVELS = 11;     // levels 1-10 plus the special level
    public static final int SPECIAL_LEVEL = 11;

    private static final LevelData[] LEVELS = {
        new LevelData(1, 1.0, 2, 8,
            new String[]{"Drag each piece of trash to the bin", "that matches its material."},
            TrashKind.PAPER_TRASH, TrashKind.PLASTIC_TRASH),

        new LevelData(2, 1.0, 3, 10,
            new String[]{"Glass and metal bins join the shelf.", "The sea is moving faster!"},
            TrashKind.WINE_BOTTLE, TrashKind.CAN),

        new LevelData(3, 1.0, 3, 10,
            new String[]{"This one is made of several materials.", "Double-click it to split it, then sort each piece."},
            TrashKind.COFFEE_CUP),

        new LevelData(4, 1.1, 4, 12,
            new String[]{"Jars have more than one material.", "Double-click to split it, then sort each piece."},
            TrashKind.JAM_JAR),

        new LevelData(5, 1.1, 4, 12,
            new String[]{"Bottle and cap: split before sorting.", "Each piece goes in its own bin."},
            TrashKind.SODA_BOTTLE),

        new LevelData(6, 1.2, 4, 14,
            new String[]{"Notebooks hide a second material.", "Sort each piece."},
            TrashKind.SPIRAL_NOTEBOOK),

        new LevelData(7, 1.2, 4, 14,
            new String[]{"Takeout boxes come with leftovers.", "Sort!!!"},
            TrashKind.TAKEOUT_WITH_LEFTOVER),

        new LevelData(8, 1.3, 4, 14,
            new String[]{"Coconut drinks.", "Split first - and it's faster!"},
            TrashKind.COCONUT_DRINK),

        new LevelData(9, 1.3, 4, 14,
            new String[]{"Face masks need careful sorting.", "Check every bin before you drop!"},
            TrashKind.FACE_MASK),

        new LevelData(10, 1.5, 4, 14,
            new String[]{"Old Flashlight!", "."},
            TrashKind.OLD_FLASHLIGHT),

        new LevelData(11, 1.5, 4, 14, new String[]{"Special Level includes different kinds of trashes. Watch Out!"})
    };

    private final int number;
    private final double speed;
    private final int maxOnScreen;
    private final int quota;
    private final String[] hintLines;
    private final List<TrashKind> newKinds;

    private LevelData(int number, double speed, int maxOnScreen, int quota, String[] hintLines, TrashKind... newKinds){
        this.number = number;
        this.speed = speed;
        this.maxOnScreen = maxOnScreen;
        this.quota = quota;
        this.hintLines = hintLines;
        this.newKinds = java.util.Arrays.asList(newKinds);
    }

    public static LevelData get(int level){
        int index = Math.max(1, Math.min(TOTAL_LEVELS, level)) - 1;
        return LEVELS[index];
    }

    // everything that can appear on this level
    public List<TrashKind> getPool(){
        List<TrashKind> pool = new ArrayList<>();
        for(int i = 0; i < number; i++){
            pool.addAll(LEVELS[i].newKinds);
        }
        return pool;
    }

    // only the bins this level's trash (and its pieces) can end up in, in screen order
    public List<BinType> getBins(){
        Set<BinType> bins = EnumSet.noneOf(BinType.class);
        for(TrashKind kind : getPool()){
            if(kind.isComposite()){
                for(TrashKind part : kind.getParts()){
                    bins.add(part.getBin());
                }
            }else{
                bins.add(kind.getBin());
            }
        }
        return new ArrayList<>(bins);
    }

    public String getTitle(){
        return number == SPECIAL_LEVEL ? "SPECIAL LEVEL" : "LEVEL " + number;
    }

    public String getNewKindsText(){
        StringBuilder text = new StringBuilder();
        for(TrashKind kind : newKinds){
            if(text.length() > 0) text.append(" & ");
            text.append(kind.getDisplayName());
        }
        return text.toString();
    }

    public int getNumber() {return number;}
    public double getSpeed() {return speed;}
    public int getMaxOnScreen() {return maxOnScreen;}
    public int getQuota() {return quota;}
    public String[] getHintLines() {return hintLines;}
    public List<TrashKind> getNewKinds() {return newKinds;}
}