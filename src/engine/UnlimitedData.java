package engine;

import model.BinType;
import model.TrashKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// The stages of unlimited mode.
//  - stages 1-10 introduce trash in the same order as the ten levels (taken from LevelData)
//  - stages 11+ introduce trash that never appears in the level plays: one row per stage below
// To add more unlimited-only trash: add the kind to TrashKind, then add it to a row here
// (or add a new row for a new stage). Nothing else needs to change.
public class UnlimitedData {
    public static final int BASE_STAGES = 10;

    // one row = the trash introduced on that stage (stage 11 first)
    private static final TrashKind[][] EXTRA_STAGES = {
        {TrashKind.PLASTIC_BAG, TrashKind.NEWSPAPER},   // stage 11
        {TrashKind.BANANA_PEEL},                        // stage 12
        {TrashKind.BEER_BOTTLE},                        // stage 13
        {TrashKind.PAINT_CAN},                          // stage 14
        {TrashKind.JUICE_BOX}                           // stage 15 (multi-material: double-click to split)
    };

    public static final int LAST_STAGE = BASE_STAGES + EXTRA_STAGES.length;

    private UnlimitedData() {}

    private static int clamp(int stage){
        return Math.max(1, Math.min(LAST_STAGE, stage));
    }

    // everything that can appear on this stage: all the trash introduced up to and including it
    public static List<TrashKind> getPool(int stage){
        int s = clamp(stage);
        List<TrashKind> pool = new ArrayList<>(LevelData.get(Math.min(s, BASE_STAGES)).getPool());
        for(int i = BASE_STAGES + 1; i <= s; i++){
            pool.addAll(Arrays.asList(EXTRA_STAGES[i - BASE_STAGES - 1]));
        }
        return pool;
    }

    // the trash introduced on this stage (shown in the banner, and picked three times as often)
    public static List<TrashKind> getNewKinds(int stage){
        int s = clamp(stage);
        if(s <= BASE_STAGES){
            return LevelData.get(s).getNewKinds();
        }
        return Arrays.asList(EXTRA_STAGES[s - BASE_STAGES - 1]);
    }

    // only the bins this stage's trash (and its pieces) can end up in, in screen order
    public static List<BinType> getBins(int stage){
        Set<BinType> bins = EnumSet.noneOf(BinType.class);
        for(TrashKind kind : getPool(stage)){
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

    public static String getNewKindsText(int stage){
        StringBuilder text = new StringBuilder();
        for(TrashKind kind : getNewKinds(stage)){
            if(text.length() > 0) text.append(" & ");
            text.append(kind.getDisplayName());
        }
        return text.toString();
    }
}