package engine;

import model.BinType;
import model.TrashKind;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The "educate the player" text: a short fact per kind of trash (falling back to a fact per bin),
// plus the wording of the mistake rows and the lesson box on the recap screen.
// To teach something new about a piece, add a line to KIND_TIPS.
public class EcoFacts {
    private static final Map<BinType, String> BIN_TIPS = new EnumMap<>(BinType.class);
    private static final Map<TrashKind, String> KIND_TIPS = new EnumMap<>(TrashKind.class);

    static {
        BIN_TIPS.put(BinType.PAPER, "Clean, dry paper can be recycled into new paper several times.");
        BIN_TIPS.put(BinType.PLASTIC, "Plastic can last for centuries in the sea, breaking into tiny pieces that animals swallow.");
        BIN_TIPS.put(BinType.GLASS, "Glass can be recycled again and again without losing quality.");
        BIN_TIPS.put(BinType.METAL, "Metal can be recycled over and over, and it saves a lot of energy compared with making new metal.");
        BIN_TIPS.put(BinType.ORGANIC, "Food scraps and plants can rot back into soil as compost - they don't belong in the sea.");
        BIN_TIPS.put(BinType.RECYCLABLE, "Clean cardboard and cartons can be recycled, but grease and food leftovers can spoil them.");
        BIN_TIPS.put(BinType.HAZARD, "Hazardous waste holds chemicals that can poison water and sea life.");

        KIND_TIPS.put(TrashKind.PAPER_TRASH, "Paper can be recycled several times, but wet or greasy paper usually can't - keep it clean and dry.");
        KIND_TIPS.put(TrashKind.CAN, "Recycling an aluminium can uses up to 95% less energy than making a new one.");
        KIND_TIPS.put(TrashKind.FACE_MASK, "Disposable masks are made of plastic fibres and can tangle sea animals - cut the ear loops before binning.");
        KIND_TIPS.put(TrashKind.PLASTIC_BAG, "Sea turtles can mistake floating plastic bags for jellyfish and swallow them.");
        KIND_TIPS.put(TrashKind.NEWSPAPER, "Old newspaper is recycled into new paper products - one more reason to keep it dry.");
        KIND_TIPS.put(TrashKind.BANANA_PEEL, "Fruit peels can be composted into soil instead of rotting in a landfill, where they release methane.");
        KIND_TIPS.put(TrashKind.PAINT_CAN, "Leftover paint is hazardous waste - take it to a collection point so it never reaches drains or the sea.");
        KIND_TIPS.put(TrashKind.COFFEE_LID, "Coffee lids are a different plastic from the cup, so they have to be sorted on their own.");
        KIND_TIPS.put(TrashKind.COFFEE_CONTAINER, "Most paper cups have a thin plastic lining, so they need special recycling instead of the paper bin.");
        KIND_TIPS.put(TrashKind.JAR_LID, "Metal lids are recycled separately from the glass jar they close.");
        KIND_TIPS.put(TrashKind.BOTTLE_CAP, "Bottle caps are made of a different material from the bottle, so they are sorted separately.");
        KIND_TIPS.put(TrashKind.METAL_SPRING, "A metal spiral is scrap metal - it must be pulled off the paper before either one can be recycled.");
        KIND_TIPS.put(TrashKind.TAKEOUT_BOX, "Clean cardboard takeout boxes can be recycled, but greasy ones can't - the grease spoils the paper.");
        KIND_TIPS.put(TrashKind.LEFTOVER_FOOD, "Leftover food is organic waste - mixed into recycling it can contaminate everything around it.");
        KIND_TIPS.put(TrashKind.COCONUT_SHELL, "Coconut shells are organic and can be composted - they don't belong in the sea.");
        KIND_TIPS.put(TrashKind.DRINKING_STRAW, "Plastic straws are so small and light that they slip through recycling machines and end up in the sea.");
        KIND_TIPS.put(TrashKind.SKEWER_STICK, "Wooden sticks are organic and break down naturally, unlike plastic.");
        KIND_TIPS.put(TrashKind.BULB, "Some bulbs contain mercury, so they need special disposal instead of the normal bin.");
        KIND_TIPS.put(TrashKind.BATTERY, "Batteries can leak toxic metals - always take them to a battery drop-off.");
        KIND_TIPS.put(TrashKind.JUICE_CARTON, "Juice cartons are layers of paper, plastic and foil, so they have their own recycling stream.");
    }

    private EcoFacts() {}

    public static String tipFor(TrashKind kind){
        String tip = KIND_TIPS.get(kind);
        if(tip != null) return tip;
        if(kind.isComposite()){
            return "It is made of different materials: split it so each material is recycled on its own.";
        }
        return BIN_TIPS.get(kind.getBin());
    }

    // ---- the one-line text of a mistake row ----

    public static String describe(RunStats.Mistake m){
        TrashKind kind = m.getKind();
        String name = kind.getDisplayName() + (m.getCount() > 1 ? " x" + m.getCount() : "");

        switch(m.getType()){
            case MISSED:
                return name + " floated away - " + whereItGoes(kind);
            case WRONG_BIN:
                return name + " - you chose " + m.getChosenBin().getLabel() + ", " + whereItGoes(kind);
            default:
                return name + " - dropped whole. Split it first, then sort each piece";
        }
    }

    private static String whereItGoes(TrashKind kind){
        if(kind.isComposite()){
            return "it splits into " + partBins(kind) + " pieces";
        }
        return "it belongs in " + kind.getBin().getLabel();
    }

    // ---- the lesson box ----

    // after mistakes: explain the one made most often
    public static String lessonFor(RunStats.Mistake m){
        TrashKind kind = m.getKind();
        String name = kind.getDisplayName();

        if(kind.isComposite()){
            String lead = m.getType() == RunStats.MistakeType.MISSED ? name + " floated away. " : "";
            return lead + "The " + name + " is made of " + partList(kind)
                    + ". Double-click it to split it, then sort each piece into its own bin.";
        }
        if(m.getType() == RunStats.MistakeType.WRONG_BIN && m.getChosenBin() != null){
            return name + " belongs in the " + kind.getBin().getLabel() + " bin, not the "
                    + m.getChosenBin().getLabel() + " bin. " + tipFor(kind);
        }
        return name + " goes in the " + kind.getBin().getLabel() + " bin. " + tipFor(kind);
    }

    // after a clean run: teach about the last piece the player sorted
    public static String praiseFor(RunStats stats){
        List<RunStats.SortEvent> events = stats.getSorted();
        if(events.isEmpty()){
            return "Millions of tonnes of plastic end up in the ocean every year - sorting trash properly keeps it out.";
        }
        TrashKind kind = events.get(events.size() - 1).getKind();
        return "Nice sorting! " + kind.getDisplayName() + " goes in the " + kind.getBin().getLabel()
                + " bin. " + tipFor(kind);
    }

    // ---- helpers ----

    // "PLASTIC + RECYCLABLE": the distinct bins a multi-material piece's parts go to
    private static String partBins(TrashKind kind){
        Set<String> labels = new LinkedHashSet<>();
        for(TrashKind part : kind.getParts()){
            labels.add(part.getBin().getLabel());
        }
        return String.join(" + ", labels);
    }

    // "Coffee Lid (PLASTIC) and Coffee Cup (RECYCLABLE)"
    private static String partList(TrashKind kind){
        List<String> items = new ArrayList<>();
        for(TrashKind part : kind.getParts()){
            items.add(part.getDisplayName() + " (" + part.getBin().getLabel() + ")");
        }
        if(items.size() == 1) return items.get(0);

        String last = items.remove(items.size() - 1);
        return String.join(", ", items) + " and " + last;
    }
}