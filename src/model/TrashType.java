package model;

public enum TrashType {
    PAPER, PLASTIC, TIN, GLASS;

    public static TrashType getRandomType() {
        TrashType[] types = TrashType.values();
        int randomIndex = (int) (Math.random() * types.length);
        return types[randomIndex];
    }
}
