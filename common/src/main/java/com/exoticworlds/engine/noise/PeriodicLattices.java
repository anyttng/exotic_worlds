package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.periodic.LapFloor;
import com.exoticworlds.engine.noise.periodic.PeriodicLattice;
import com.exoticworlds.engine.noise.periodic.PeriodicNoiseSampler;

public final class PeriodicLattices {
    private final ContextKeyedCache<PeriodicLattice> lattices = new ContextKeyedCache<>();

    public double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, Context context,
            double x, double y, double z, double yScale, double yFudge) {
        return PeriodicNoiseSampler.sample(latticeFor(permutations, xOffset, yOffset, zOffset, fold, context),
                x, y, z, yScale, yFudge);
    }

    public boolean isIndexable(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, Context context) {
        return latticeFor(permutations, xOffset, yOffset, zOffset, fold, context).isIndexable();
    }

    PeriodicLattice latticeFor(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, Context context) {
        PeriodicLattice cached = this.lattices.find(fold, context);
        if (cached != null) {
            return cached;
        }

        return this.lattices.put(fold, context, PeriodicNoiseSampler.lattice(permutations, xOffset, yOffset,
                zOffset, fold, context, LapFloor.of(fold)));
    }
}
