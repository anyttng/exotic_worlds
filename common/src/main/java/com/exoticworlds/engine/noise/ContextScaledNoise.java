package com.exoticworlds.engine.noise;

import com.exoticworlds.accessors.ContextOctavesHolder;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;

public final class ContextScaledNoise {
    private static final double NO_SHIFT = 0.0;

    public static float sample(WorldFold fold, Noise noise, double scale, double x, double y, double z) {
        ContextOctaves octaves = octavesFor(fold, noise, scale);
        return octaves.isIndexable()
                ? octaves.sample(x, y, z)
                : FoldedSamplers.vanillaAtFold(fold.getBlockLattice(), noise, scale, x, y, z, NO_SHIFT, NO_SHIFT);
    }

    private static ContextOctaves octavesFor(WorldFold fold, Noise noise, double scale) {
        NoiseStack stack = FoldedSamplers.getStack(noise);
        ContextOctavesHolder holder = (ContextOctavesHolder) (Object) stack;
        ContextOctaves held = holder.exotic_worlds$getContextOctaves();
        if (held != null && held.isCompiledFor(fold, scale)) {
            return held;
        }

        ContextOctaves built = new ContextOctaves(fold, scale,
                FoldedSamplers.octaves(fold, NoiseFrame.UNDECLARED, scale, null, stack));
        holder.exotic_worlds$setContextOctaves(built);
        return built;
    }

    private ContextScaledNoise() {
    }
}
