package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.periodic.PeriodicOctaves;

public final class ContextOctaves {
    private final WorldFold fold;

    private final double scale;

    private final PeriodicOctaves octaves;

    private final boolean isIndexable;

    ContextOctaves(WorldFold fold, double scale, PeriodicOctaves octaves) {
        this.fold = fold;
        this.scale = scale;
        this.octaves = octaves;
        this.isIndexable = octaves.isIndexable();
    }

    boolean isCompiledFor(WorldFold fold, double scale) {
        return this.scale == scale && this.fold.equals(fold);
    }

    boolean isIndexable() {
        return this.isIndexable;
    }

    float sample(double x, double y, double z) {
        return this.octaves.sample(x, y, z);
    }
}
