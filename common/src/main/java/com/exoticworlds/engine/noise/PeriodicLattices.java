package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

public final class PeriodicLattices {
    static final int CAPACITY = 8;

    private static final Entry[] EMPTY = new Entry[0];

    private record Entry(WorldFold fold, SlotAxes axes, double scale, double verticalShare, double xDivisor,
            double zDivisor, PeriodicLattice lattice) {
        boolean matches(WorldFold transformer, Context context) {
            return this.fold == transformer
                    && this.scale == context.horizontalScale()
                    && this.verticalShare == context.verticalShare()
                    && this.xDivisor == context.xDivisor()
                    && this.zDivisor == context.zDivisor()
                    && this.axes.equals(context.slotAxes());
        }
    }

    private volatile Entry[] entries = EMPTY;

    public double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, Context context,
            double x, double y, double z, double yScale, double yFudge) {
        return PeriodicNoiseSampler.sample(latticeFor(permutations, xOffset, yOffset, zOffset, transformer, context),
                x, y, z, yScale, yFudge);
    }

    PeriodicLattice latticeFor(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, Context context) {
        Entry[] current = this.entries;
        for (Entry entry : current) {
            if (entry.matches(transformer, context)) {
                return entry.lattice();
            }
        }

        PeriodicLattice lattice = PeriodicNoiseSampler.lattice(permutations, xOffset, yOffset, zOffset, transformer,
                context, LapFloor.of(transformer));
        this.entries = withNewest(current, new Entry(transformer, context.slotAxes(), context.horizontalScale(),
                context.verticalShare(), context.xDivisor(), context.zDivisor(), lattice));
        return lattice;
    }

    private static Entry[] withNewest(Entry[] current, Entry newest) {
        int kept = Math.min(current.length, CAPACITY - 1);
        Entry[] next = new Entry[kept + 1];
        System.arraycopy(current, current.length - kept, next, 0, kept);
        next[kept] = newest;
        return next;
    }
}
