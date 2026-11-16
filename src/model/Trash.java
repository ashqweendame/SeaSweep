package model;

import java.util.ArrayList;
import java.util.List;

// One piece of floating trash.
//  - SimpleTrash goes straight into a bin.
//  - CompositeTrash must be split (double-click) into SimpleTrash pieces first.
public abstract class Trash {
    public static final int SIZE_SIMPLE = 80;
    public static final int SIZE_COMPOSITE = 96;
    public static final int SIZE_PART = 76;

    private final double BOB_AMPLITUDE = 6.0;
    private final double BOB_SPEED = 0.06;
    private final double HIT_PADDING = 10.0;

    private final TrashKind kind;
    private final int size;
    private final double velocityX;   // pixels per tick; the sign is the direction it floats
    private double x;
    private double y;                 // where it is drawn (baseY plus the little bob)
    private double baseY;
    private double bobPhase;
    private boolean dragging;

    protected Trash(TrashKind kind, double x, double baseY, double velocityX, int size, double bobPhase){
        this.kind = kind;
        this.x = x;
        this.baseY = baseY;
        this.velocityX = velocityX;
        this.size = size;
        this.bobPhase = bobPhase;
        this.y = baseY + Math.sin(bobPhase) * BOB_AMPLITUDE;
    }

    // one tick of floating (a piece being dragged stays under the mouse instead)
    public void drift(){
        if(dragging) return;
        x += velocityX;
        bobPhase += BOB_SPEED;
        y = baseY + Math.sin(bobPhase) * BOB_AMPLITUDE;
    }

    public boolean contains(double px, double py){
        double reach = size / 2.0 + HIT_PADDING;
        double dx = px - x;
        double dy = py - y;
        return dx * dx + dy * dy <= reach * reach;
    }

    // true once it has floated completely off the far side of the screen
    public boolean hasWashedAway(int worldWidth){
        if(dragging) return false;
        if(velocityX > 0) return x - size / 2.0 > worldWidth;
        if(velocityX < 0) return x + size / 2.0 < 0;
        return false;
    }

    public void startDrag() {dragging = true;}
    public void stopDrag() {dragging = false;}

    public void dragTo(double newX, double newY){
        x = newX;
        y = newY;
        baseY = newY;
    }

    public void placeAt(double newX, double newBaseY){
        x = newX;
        baseY = newBaseY;
        y = baseY + Math.sin(bobPhase) * BOB_AMPLITUDE;
    }

    // single-material trash can't be split
    public List<Trash> split(){
        return new ArrayList<>();
    }

    public abstract boolean isComposite();
    public abstract BinType getBin();   // null while it is still multi-material

    public TrashKind getKind() {return kind;}
    public int getSize() {return size;}
    public double getX() {return x;}
    public double getY() {return y;}
    public double getBaseY() {return baseY;}
    public double getVelocityX() {return velocityX;}
    public double getBobPhase() {return bobPhase;}
    public boolean isDragging() {return dragging;}
}