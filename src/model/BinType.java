package model;

// The seven bins along the bottom of the screen. The order here is the left-to-right order on screen.
public enum BinType {
    PAPER("PAPER", "paper-floater", null),
    PLASTIC("PLASTIC", "plastic-floater", null),
    GLASS("GLASS", "glass-floater", null),
    METAL("METAL", "metal-floater", "tin-floater"),
    ORGANIC("ORGANIC", "organic-floater", null),
    RECYCLABLE("RECYCLABLE", "recyclable-floater", null),
    HAZARD("HAZARD", "hazard-floater", null);

    private final String label;
    private final String assetName;
    private final String fallbackAsset;

    BinType(String label, String assetName, String fallbackAsset){
        this.label = label;
        this.assetName = assetName;
        this.fallbackAsset = fallbackAsset;
    }

    public String getLabel() {return label;}
    public String getAssetName() {return assetName;}
    public String getFallbackAsset() {return fallbackAsset;}
}