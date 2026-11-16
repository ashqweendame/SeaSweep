package model;

import java.util.ArrayList;
import java.util.List;

public class CompositeTrash extends Trash {
    private final double PART_SPREAD = 92.0;

    public CompositeTrash(TrashKind kind, double x, double baseY, double velocityX, int size, double bobPhase){
        super(kind, x, baseY, velocityX, size, bobPhase);
    }

    @Override
    public boolean isComposite() {return true;}

    @Override
    public BinType getBin() {return null;}

    // the pieces appear side by side where this trash was, floating the same way
    @Override
    public List<Trash> split(){
        List<TrashKind> parts = getKind().getParts();
        List<Trash> pieces = new ArrayList<>();

        for(int i = 0; i < parts.size(); i++){
            double offsetX = (i - (parts.size() - 1) / 2.0) * PART_SPREAD;
            double offsetY = (i % 2 == 0) ? -10 : 10;
            pieces.add(new SimpleTrash(parts.get(i), getX() + offsetX, getY() + offsetY,
                    getVelocityX(), SIZE_PART, getBobPhase() + i));
        }
        return pieces;
    }
}