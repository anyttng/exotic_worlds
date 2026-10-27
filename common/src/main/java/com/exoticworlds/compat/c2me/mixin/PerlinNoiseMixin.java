package com.exoticworlds.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.accessors.WrappedPerlinNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(PerlinNoise.class)
public class PerlinNoiseMixin {
    @WrapMethod(method = "@MixinSquared:Handler")
    @TargetHandler(
            mixin = "com.ishland.c2me.opts.math.mixin.MixinOctavePerlinNoiseSampler",
            name = "getValue")
    private double exotic_worlds$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(x, y, z);
        }

        // The zeros are what C2ME's loop passes every octave, kept so the two walks differ in nothing but the fold.
        return ((WrappedPerlinNoise) (Object) this).exotic_worlds$sampleWrapped(generation, x, y, z, 0.0, 0.0);
    }
}
