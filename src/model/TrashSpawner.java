package model;
import java.util.Random;

public class TrashSpawner {
    private Random rand;

    public TrashSpawner(){
        this.rand = new Random();
    }

    public Trash generateNextTrash(int fixedDropX, TrashType leftType, TrashType rightType){
        TrashType spawnType = (rand.nextBoolean()) ? leftType : rightType;

        switch(spawnType){
            case PAPER: return new Paper(fixedDropX);
            case PLASTIC: return new Plastic(fixedDropX);
            case TIN: return new Tin(fixedDropX);
            case GLASS: return new Glass(fixedDropX);
            default: throw new IllegalArgumentException("Unknown TrashType: " + spawnType);
        }
    }
}
