package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.PeriodicSimplexSampler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

@Mixin(PerlinSimplexNoise.class)
public class PerlinSimplexNoiseMixin {
    @Shadow
    @Final
    private SimplexNoise[] noiseLevels;

    @Shadow
    @Final
    private double highestFreqValueFactor;

    @Shadow
    @Final
    private double highestFreqInputFactor;

    @WrapMethod(method = "getValue(DDZ)D")
    private double exotic_worlds$periodicValue(double x, double z, boolean useNoiseStart, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(x, z, useNoiseStart);
        }

        WorldFold fold = generation.fold();
        double baseScale = generation.horizontalScale();
        double value = 0.0;
        double factor = this.highestFreqInputFactor;
        double valueFactor = this.highestFreqValueFactor;

        for (SimplexNoise noiseLevel : this.noiseLevels) {
            if (noiseLevel != null) {
                value += PeriodicSimplexSampler.sample(
                        ((SimplexNoiseAccessor) (Object) noiseLevel).exotic_worlds$permutations(),
                        useNoiseStart ? noiseLevel.xo : 0.0,
                        useNoiseStart ? noiseLevel.yo : 0.0,
                        fold,
                        baseScale * factor,
                        x,
                        z) * valueFactor;
            }

            factor /= 2.0;
            valueFactor *= 2.0;
        }

        return value;
    }
}
