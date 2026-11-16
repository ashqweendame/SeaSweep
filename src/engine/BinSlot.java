package engine;

import model.BinType;

// One bin on screen: which bin it is and where it sits.
public class BinSlot {
    private final int DROP_REACH = 40;   // dropping a little above the bin still counts

    private final BinType type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public BinSlot(BinType type, int x, int y, int width, int height){
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean acceptsPoint(double px, double py){
        return px >= x && px <= x + width && py >= y - DROP_REACH && py <= y + height;
    }

    public BinType getType() {return type;}
    public int getX() {return x;}
    public int getY() {return y;}
    public int getWidth() {return width;}
    public int getHeight() {return height;}
}