package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;

public final class PeriodicLattices {
    private final ContextKeyedCache<PeriodicLattice> lattices = new ContextKeyedCache<>();

    public double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, Context context,
            double x, double y, double z, double yScale, double yFudge) {
        return PeriodicNoiseSampler.sample(latticeFor(permutations, xOffset, yOffset, zOffset, transformer, context),
                x, y, z, yScale, yFudge);
    }

    public boolean indexable(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, Context context) {
        return latticeFor(permutations, xOffset, yOffset, zOffset, transformer, context).indexable();
    }

    PeriodicLattice latticeFor(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, Context context) {
        PeriodicLattice cached = this.lattices.find(transformer, context);
        if (cached != null) {
            return cached;
        }

        return this.lattices.put(transformer, context, PeriodicNoiseSampler.lattice(permutations, xOffset, yOffset,
                zOffset, transformer, context, LapFloor.of(transformer)));
    }
}
