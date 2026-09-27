package com.toroidalworld.engine.noise;

final class PeriodicOctaves {
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

    float sample(double x, double y, double z) {
        float value = 0.0F;
        for (int i = 0; i < this.lattices.length; i++) {
            float octave = PeriodicOctaveSampler.octave(this.axes, this.lattices[i], this.frequencies[i], x, y, z);
            value += this.amplitudes[i] * octave;
        }

        return value;
    }
}
