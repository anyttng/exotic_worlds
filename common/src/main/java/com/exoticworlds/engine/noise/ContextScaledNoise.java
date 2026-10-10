package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.Noise;

public final class ContextScaledNoise {
    private static final double NO_SHIFT = 0.0;

    public static float sample(WorldFold fold, Noise noise, double scale, double x, double y, double z) {
        PeriodicOctaves octaves = PeriodicOctaveSampler.compile(fold, NoiseFrame.UNDECLARED, scale, null,
                FoldedSamplers.stackOf(noise));
        return octaves.indexable()
                ? octaves.sample(x, y, z)
                : FoldedSamplers.vanillaAtFold(fold.blockLattice(), noise, scale, x, y, z, NO_SHIFT, NO_SHIFT);
    }

    private ContextScaledNoise() {
    }
}
