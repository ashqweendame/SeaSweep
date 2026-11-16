package engine;

import model.BinType;
import model.Trash;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// The sea: the floating trash, the bins along the bottom, and the mouse handling
// (grab, drag, drop, double-click to split). Rules such as score and lives live elsewhere.
public class Playfield {
    public enum PressResult {NONE, GRABBED, SPLIT}
    public enum DropResult {NONE, CORRECT, WRONG, NEEDS_SPLIT}

    private final int width;
    private final int height;
    private final List<Trash> items = new ArrayList<>();
    private final List<BinSlot> slots = new ArrayList<>();

    private Trash dragged;
    private double grabDx;
    private double grabDy;
    private Trash lastDropped;
    private boolean removeOnWrong = true;

    public Playfield(int width, int height){
        this.width = width;
        this.height = height;
    }

    // ---- bins ----

    public void setBins(List<BinType> types){
        slots.clear();
        int count = types.size();
        if(count == 0) return;

        int slotWidth = Math.min(170, (width - 40) / count);
        int binWidth = slotWidth - 8;
        int binHeight = binWidth * 175 / 170;
        int startX = (width - slotWidth * count) / 2 + 4;
        int y = height - 8 - binHeight;

        for(int i = 0; i < count; i++){
            slots.add(new BinSlot(types.get(i), startX + i * slotWidth, y, binWidth, binHeight));
        }
    }

    public List<BinSlot> getBinSlots(){
        return new ArrayList<>(slots);
    }

    private BinSlot slotAt(double x, double y){
        for(BinSlot slot : slots){
            if(slot.acceptsPoint(x, y)){
                return slot;
            }
        }
        return null;
    }

    // the bin under the piece that is being dragged (for the highlight)
    public BinSlot getHoveredSlot(){
        if(dragged == null) return null;
        return slotAt(dragged.getX(), dragged.getY());
    }

    // ---- items ----

    public void add(Trash trash) {items.add(trash);}
    public int size() {return items.size();}
    public boolean isEmpty() {return items.isEmpty();}
    public List<Trash> getItems() {return new ArrayList<>(items);}
    public Trash getDragged() {return dragged;}
    public Trash getLastDropped() {return lastDropped;}
    public void setRemoveOnWrong(boolean removeOnWrong) {this.removeOnWrong = removeOnWrong;}

    public void clear(){
        items.clear();
        dragged = null;
        lastDropped = null;
    }

    // true when a new piece can wash in without landing on top of another one
    public boolean hasRoomFor(Trash candidate){
        for(Trash other : items){
            double reach = (other.getSize() + candidate.getSize()) / 2.0;
            if(Math.abs(other.getX() - candidate.getX()) < reach
                    && Math.abs(other.getY() - candidate.getY()) < reach){
                return false;
            }
        }
        return true;
    }

    // one tick: everything floats on. Returns how many pieces washed off the far side.
    public int update(){
        int washedAway = 0;
        Iterator<Trash> it = items.iterator();
        while(it.hasNext()){
            Trash trash = it.next();
            if(trash == dragged) continue;

            trash.drift();
            if(trash.hasWashedAway(width)){
                it.remove();
                washedAway++;
            }
        }
        return washedAway;
    }

    // ---- mouse ----

    public PressResult press(double x, double y, int clickCount){
        // of the pieces under the pointer, grab the one whose center is closest
        // (the top-most one wins a tie)
        Trash hit = null;
        double bestDistance = Double.MAX_VALUE;
        for(int i = items.size() - 1; i >= 0; i--){
            Trash candidate = items.get(i);
            if(candidate.contains(x, y)){
                double distance = Math.hypot(candidate.getX() - x, candidate.getY() - y);
                if(distance < bestDistance){
                    bestDistance = distance;
                    hit = candidate;
                }
            }
        }
        if(hit == null) return PressResult.NONE;

        if(clickCount >= 2 && hit.isComposite()){
            items.remove(hit);
            for(Trash piece : hit.split()){
                piece.placeAt(piece.getX(), clampToBand(piece.getY(), piece.getSize()));
                items.add(piece);
            }
            return PressResult.SPLIT;
        }

        dragged = hit;
        hit.startDrag();
        grabDx = hit.getX() - x;
        grabDy = hit.getY() - y;
        items.remove(hit);
        items.add(hit);   // draw it on top
        return PressResult.GRABBED;
    }

    public void dragTo(double x, double y){
        if(dragged == null) return;

        double newX = Math.max(0, Math.min(width, x + grabDx));
        double newY = Math.max(0, Math.min(height, y + grabDy));
        dragged.dragTo(newX, newY);
    }

    public DropResult release(){
        if(dragged == null) return DropResult.NONE;

        Trash trash = dragged;
        dragged = null;
        trash.stopDrag();
        lastDropped = null;

        BinSlot slot = slotAt(trash.getX(), trash.getY());
        if(slot == null){
            floatBackIntoBand(trash);   // dropped on open water: keeps floating from there
            return DropResult.NONE;
        }

        if(trash.isComposite()){
            floatBackIntoBand(trash);   // must be split before it can be sorted
            return DropResult.NEEDS_SPLIT;
        }

        lastDropped = trash;
        if(slot.getType() == trash.getBin()){
            items.remove(trash);
            return DropResult.CORRECT;
        }

        if(removeOnWrong){
            items.remove(trash);
        }else{
            floatBackIntoBand(trash);
        }
        return DropResult.WRONG;
    }

    private void floatBackIntoBand(Trash trash){
        trash.placeAt(trash.getX(), clampToBand(trash.getY(), trash.getSize()));
    }

    private double clampToBand(double y, int size){
        double min = GameConfig.BAND_TOP + size / 2.0;
        double max = GameConfig.BAND_BOTTOM - size / 2.0;
        return Math.max(min, Math.min(max, y));
    }
}