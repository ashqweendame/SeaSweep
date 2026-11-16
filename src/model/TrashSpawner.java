package model;

import java.util.List;
import java.util.Random;

// Factory: the only place that builds Trash objects.
public class TrashSpawner {
    private final Random rand = new Random();

    // a new piece washing in from the left or right edge, at a random height inside the band
    public Trash createTrash(TrashKind kind, int worldWidth, double speed, int bandTop, int bandBottom){
        boolean fromLeft = rand.nextBoolean();
        int size = sizeOf(kind);

        double x = fromLeft ? -size : worldWidth + size;
        double velocity = (fromLeft ? 1 : -1) * speed * (0.85 + rand.nextDouble() * 0.3);
        double minY = bandTop + size / 2.0 + 8;
        double maxY = bandBottom - size / 2.0 - 8;
        double baseY = minY + rand.nextDouble() * (maxY - minY);

        return build(kind, x, baseY, velocity, size);
    }

    // a piece placed exactly where you say (velocity 0 = it just bobs on the spot)
    public Trash createTrashAt(TrashKind kind, double x, double baseY, double velocity){
        return build(kind, x, baseY, velocity, sizeOf(kind));
    }

    // picks from the pool; the kinds introduced on this level show up three times as often
    public TrashKind pickKind(List<TrashKind> pool, List<TrashKind> featured){
        int total = 0;
        for(TrashKind kind : pool){
            total += featured.contains(kind) ? 3 : 1;
        }

        int roll = rand.nextInt(total);
        for(TrashKind kind : pool){
            roll -= featured.contains(kind) ? 3 : 1;
            if(roll < 0){
                return kind;
            }
        }
        return pool.get(0);
    }

    private int sizeOf(TrashKind kind){
        return kind.isComposite() ? Trash.SIZE_COMPOSITE : Trash.SIZE_SIMPLE;
    }

    private Trash build(TrashKind kind, double x, double baseY, double velocity, int size){
        double phase = rand.nextDouble() * Math.PI * 2;
        if(kind.isComposite()){
            return new CompositeTrash(kind, x, baseY, velocity, size, phase);
        }
        return new SimpleTrash(kind, x, baseY, velocity, size, phase);
    }
}