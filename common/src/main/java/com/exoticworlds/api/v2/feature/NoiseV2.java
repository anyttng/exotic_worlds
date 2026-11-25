package com.exoticworlds.api.v2.feature;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/** One noise of the dimension's terrain as it is compiled onto the loop. */
public interface NoiseV2 extends MomentV2 {
    /** The noise being compiled. */
    Holder<NormalNoise.NoiseParameters> getNoise();

    /** How many layers the noise sums. */
    int getLayerCount();

    /** The frequency the layer at {@code layer} samples at, counted from zero. */
    double getLayerFrequency(int layer);

    /** The horizontal scale the terrain samples this noise at. */
    double getScale();

    /** The noise's vertical scale as a share of its horizontal one, negative where the noise declares none. */
    double getVerticalShare();
}
