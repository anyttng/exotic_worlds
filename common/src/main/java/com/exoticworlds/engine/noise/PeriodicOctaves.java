package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

final class PeriodicOctaves {
    private static final PeriodicLattice[] NO_LATTICES = {};
    private static final double[] NO_FREQUENCIES = {};
    private static final float[] NO_AMPLITUDES = {};

    private final SlotAxes axes;

    private final PeriodicLattice[] lattices;

    private final double[] frequencies;

    private final float[] amplitudes;

    private final @Nullable CuboidOctaves cuboid;

    PeriodicOctaves(SlotAxes axes, PeriodicLattice[] lattices, double[] frequencies, float[] amplitudes) {
        this(axes, lattices, frequencies, amplitudes, null);
    }

    private PeriodicOctaves(SlotAxes axes, PeriodicLattice[] lattices, double[] frequencies, float[] amplitudes,
            @Nullable CuboidOctaves cuboid) {
        this.axes = axes;
        this.lattices = lattices;
        this.frequencies = frequencies;
        this.amplitudes = amplitudes;
        this.cuboid = cuboid;
    }

    static PeriodicOctaves cuboid(CuboidOctaves cuboid) {
        return new PeriodicOctaves(SlotAxes.DEFAULT, NO_LATTICES, NO_FREQUENCIES, NO_AMPLITUDES, cuboid);
    }

    float sample(double x, double y, double z) {
        if (this.cuboid != null) {
            return this.cuboid.sample(x, y, z);
        }

        float value = 0.0F;
        for (int i = 0; i < this.lattices.length; i++) {
            float octave = PeriodicOctaveSampler.octave(this.axes, this.lattices[i], this.frequencies[i], x, y, z);
            value += this.amplitudes[i] * octave;
        }

        return value;
    }

    boolean indexable() {
        for (PeriodicLattice lattice : this.lattices) {
            if (!lattice.indexable()) {
                return false;
            }
        }

        return true;
    }
}
