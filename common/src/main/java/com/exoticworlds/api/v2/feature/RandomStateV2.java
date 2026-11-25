package com.exoticworlds.api.v2.feature;

import net.minecraft.world.level.levelgen.RandomState;

/**
 * A dimension's noise state, just built and already looped. It is built once per loaded dimension, before any chunk
 * generates, and again for a terrain preview.
 */
public interface RandomStateV2 extends MomentV2 {
    /** The state itself. Sampling it reads the looped terrain. */
    RandomState getRandomState();

    /** The dimension's sea level, in blocks. */
    int getSeaLevel();

    /**
     * Sets the amount added to every sample of each noise the calling feature shifts, the ones its
     * {@link NoiseMomentV2#isOffsetting} answers {@code true} for. Zero until it is set. The offset may be set many
     * times, each one read by the samples taken after it, which lets a feature try values and keep the one it wants.
     */
    void setNoiseOffset(double offset);
}
