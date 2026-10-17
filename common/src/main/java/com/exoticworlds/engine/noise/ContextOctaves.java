package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;

public final class ContextOctaves {
    private final WorldFold fold;

    private final double scale;

    private final PeriodicOctaves octaves;

    private final boolean indexable;

    ContextOctaves(WorldFold fold, double scale, PeriodicOctaves octaves) {
        this.fold = fold;
        this.scale = scale;
        this.octaves = octaves;
        this.indexable = octaves.indexable();
    }

    boolean compiledFor(WorldFold fold, double scale) {
        return this.scale == scale && this.fold.equals(fold);
    }

    boolean indexable() {
        return this.indexable;
    }

    float sample(double x, double y, double z) {
        return this.octaves.sample(x, y, z);
    }
}
