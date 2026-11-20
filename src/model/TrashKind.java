package model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Every kind of trash in the game, in one table.
//  - single-material trash names the bin it belongs in
//  - multi-material trash lists the pieces it splits into (double-click)
// To change which bin a piece goes to, edit its line here - nothing else needs to change.
// A multi-material line may only list pieces that are declared ABOVE it.
public enum TrashKind {
    // ---- single-material trash ----
    //          name           picture file       fallback picture   bin
    PAPER_TRASH("Paper",       "paper-trash-v1",  null,              BinType.PAPER),
    PLASTIC_TRASH("Plastic",   "plastic-trash-v1", null,             BinType.PLASTIC),
    WINE_BOTTLE("Wine Bottle", "wine-bottle",     "glass-trash-v1",  BinType.GLASS),
    CAN("Can",                 "can",             "tin-trash-v1",    BinType.METAL),
    FACE_MASK("Face Mask",     "face-mask",       null,              BinType.HAZARD),

    // ---- single-material trash that only appears in UNLIMITED (see UnlimitedData) ----
    // until you draw a picture for one, it uses its fallback picture (or a colored circle with its name)
    PLASTIC_BAG("Plastic Bag",   "plastic-bag",   "plastic-trash-v1", BinType.PLASTIC),
    NEWSPAPER("Newspaper",       "newspaper",     "paper-trash-v1",   BinType.PAPER),
    BANANA_PEEL("Banana Peel",   "banana-peel",   null,               BinType.ORGANIC),
    BEER_BOTTLE("Beer Bottle",   "beer-bottle",   "glass-trash-v1",   BinType.GLASS),
    PAINT_CAN("Paint Can",       "paint-can",     null,               BinType.HAZARD),

    // ---- pieces that come out of multi-material trash ----
    COFFEE_LID("Coffee Lid",           "coffee-lid",        null,             BinType.PLASTIC),
    COFFEE_CONTAINER("Coffee Cup",     "coffee-container",  null,             BinType.RECYCLABLE),
    JAR_LID("Jar Lid",                 "metal-jar-lid",   null,             BinType.METAL),
    JAR_CONTAINER("Jar",               "jar-container",     null,             BinType.GLASS),
    BOTTLE_CAP("Bottle Cap",           "bottle-cap",        null,             BinType.METAL),
    SODA_BOTTLE_BODY("Bottle",         "soda-bottle-body",  null,             BinType.GLASS),
    NOTEBOOK_PAGES("Notebook Pages",   "notebook-pages",    "paper-trash-v1", BinType.PAPER),
    METAL_SPRING("Metal Spring",       "metal-spring",      null,             BinType.METAL),
    TAKEOUT_BOX("Takeout Box",         "takeout-box",       null,             BinType.RECYCLABLE),
    LEFTOVER_FOOD("Leftover Food",     "leftover-food",     null,             BinType.ORGANIC),
    COCONUT_SHELL("Coconut Shell",     "coconut-shell",     null,             BinType.ORGANIC),
    DRINKING_STRAW("Drinking Straw",   "drinking-straw",    null,             BinType.PLASTIC),
    SKEWER_STICK("Skewer Stick",       "skewer-stick",      null,             BinType.ORGANIC),
    BULB("Bulb", "bulb", null, BinType.HAZARD),
    FLASHLIGHT_CASE("Remote Case",         "remote-case",       null,             BinType.PLASTIC),
    BATTERY("Battery",                 "battery",           null,             BinType.HAZARD),
    JUICE_CARTON("Juice Carton",       "juice-carton",      null,             BinType.RECYCLABLE),

    // ---- multi-material trash (double-click to split into the pieces listed) ----
    COFFEE_CUP("Coffee Cup",             "coffee-cup",                null, COFFEE_LID, COFFEE_CONTAINER),
    JAM_JAR("Jam Jar",                   "jam-jar",                   null, JAR_LID, JAR_CONTAINER),
    SODA_BOTTLE("Soda Bottle",           "soda-bottle",               null, BOTTLE_CAP, SODA_BOTTLE_BODY),
    SPIRAL_NOTEBOOK("Spiral Notebook",   "spiral-notebook",           null, METAL_SPRING, NOTEBOOK_PAGES),
    TAKEOUT_WITH_LEFTOVER("Takeout Box", "takeout-box-with-leftover", null, TAKEOUT_BOX, LEFTOVER_FOOD),
    COCONUT_DRINK("Coconut Drink",       "coconut-drink",             null, COCONUT_SHELL, DRINKING_STRAW),
    JUICE_BOX("Juice Box",               "juice-box",                 null, DRINKING_STRAW, JUICE_CARTON),   // unlimited only
    OLD_FLASHLIGHT("Old Flashlight",    "old-flashlight",          null,
    BULB, FLASHLIGHT_CASE, BATTERY);

    private final String displayName;
    private final String assetName;
    private final String fallbackAsset;
    private final BinType bin;               // null for multi-material trash
    private final List<TrashKind> parts;     // empty for single-material trash

    TrashKind(String displayName, String assetName, String fallbackAsset, BinType bin){
        this.displayName = displayName;
        this.assetName = assetName;
        this.fallbackAsset = fallbackAsset;
        this.bin = bin;
        this.parts = Collections.emptyList();
    }

    TrashKind(String displayName, String assetName, String fallbackAsset, TrashKind... parts){
        this.displayName = displayName;
        this.assetName = assetName;
        this.fallbackAsset = fallbackAsset;
        this.bin = null;
        this.parts = Collections.unmodifiableList(Arrays.asList(parts));
    }

    public boolean isComposite() {return !parts.isEmpty();}
    public String getDisplayName() {return displayName;}
    public String getAssetName() {return assetName;}
    public String getFallbackAsset() {return fallbackAsset;}
    public BinType getBin() {return bin;}
    public List<TrashKind> getParts() {return parts;}
}