package model;

public abstract class Trash {
    private final TrashType type;
    private int xPosition;
    private double yPosition;

    public Trash(TrashType type, int startX){
        this.type = type;
        this.xPosition = startX; //where the object is centered 250
        this.yPosition = 250.0; 
    }

    public void applyContinuousGravity(double speed){
        this.yPosition += speed; //the +0.001 
    }

    public boolean hasReachedMissLine(int missLineY){
        return this.yPosition >= missLineY; //the Y of the discard floater(490)
    }

    public TrashType getType() {return this.type;}
    public int getxPosition() {return this.xPosition;}
    public int getyPosition() {return (int) this.yPosition;}

}
