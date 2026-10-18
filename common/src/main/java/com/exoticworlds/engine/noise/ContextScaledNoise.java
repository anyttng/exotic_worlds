package com.exoticworlds.engine.noise;

import com.exoticworlds.accessors.ContextOctavesHolder;
import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;

public final class ContextScaledNoise {
    private static final double NO_SHIFT = 0.0;

    public static float sample(WorldFold fold, Noise noise, double scale, double x, double y, double z) {
        ContextOctaves octaves = octavesFor(fold, noise, scale);
        return octaves.indexable()
                ? octaves.sample(x, y, z)
                : FoldedSamplers.vanillaAtFold(fold.blockLattice(), noise, scale, x, y, z, NO_SHIFT, NO_SHIFT);
    }

    private static ContextOctaves octavesFor(WorldFold fold, Noise noise, double scale) {
        NoiseStack stack = FoldedSamplers.stackOf(noise);
        ContextOctavesHolder holder = (ContextOctavesHolder) (Object) stack;
        ContextOctaves held = holder.exotic_worlds$contextOctaves();
        if (held != null && held.compiledFor(fold, scale)) {
            return held;
        }

        ContextOctaves built = new ContextOctaves(fold, scale,
                PeriodicOctaveSampler.compile(fold, NoiseFrame.UNDECLARED, scale, null, stack));
        holder.exotic_worlds$contextOctaves(built);
        return built;
    }

    private ContextScaledNoise() {
    }
}
