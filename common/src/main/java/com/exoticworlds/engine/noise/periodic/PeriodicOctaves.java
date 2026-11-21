package com.exoticworlds.engine.noise.periodic;

import com.exoticworlds.engine.noise.CompiledOctaves;
import com.exoticworlds.engine.noise.SlotAxes;

public final class PeriodicOctaves implements CompiledOctaves {
    private final SlotAxes axes;

    private final PeriodicLattice[] lattices;

    private final double[] frequencies;

    private final float[] amplitudes;

    PeriodicOctaves(SlotAxes axes, PeriodicLattice[] lattices, double[] frequencies, float[] amplitudes) {
        this.axes = axes;
        this.lattices = lattices;
        this.frequencies = frequencies;
        this.amplitudes = amplitudes;
    }

    @Override
    public float sample(double x, double y, double z) {
        float value = 0.0F;
        for (int i = 0; i < this.lattices.length; i++) {
            float octave = PeriodicOctaveSampler.octave(this.axes, this.lattices[i], this.frequencies[i], x, y, z);
            value += this.amplitudes[i] * octave;
        }

        return value;
    }

    @Override
    public boolean indexable() {
        for (PeriodicLattice lattice : this.lattices) {
            if (!lattice.indexable()) {
                return false;
            }
        }

        return true;
    }
}
