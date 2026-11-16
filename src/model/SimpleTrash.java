package model;

public class SimpleTrash extends Trash {
    public SimpleTrash(TrashKind kind, double x, double baseY, double velocityX, int size, double bobPhase){
        super(kind, x, baseY, velocityX, size, bobPhase);
    }

    @Override
    public boolean isComposite() {return false;}

    @Override
    public BinType getBin() {return getKind().getBin();}
}