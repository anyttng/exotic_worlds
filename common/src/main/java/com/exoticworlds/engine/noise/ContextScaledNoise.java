package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.Noise;

public final class ContextScaledNoise {
    public static float sample(WorldFold fold, Noise noise, double scale, double x, double y, double z) {
        return PeriodicOctaveSampler.sample(fold, NoiseFrame.UNDECLARED, scale, FoldedSamplers.stackOf(noise), x, y, z);
    }

    private ContextScaledNoise() {
    }
}
