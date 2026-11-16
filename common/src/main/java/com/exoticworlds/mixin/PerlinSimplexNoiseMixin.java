package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.SimplexLattice;
import com.exoticworlds.engine.noise.SimplexSlots;
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

    @Unique
    private final SimplexSlots exotic_worlds$slots = new SimplexSlots();

    @WrapMethod(method = "getValue(DDZ)D")
    private double exotic_worlds$periodicValue(double x, double z, boolean useNoiseStart, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(x, z, useNoiseStart);
        }

        SimplexLattice[] lattices = this.exotic_worlds$slots.octaves(generation.fold(), generation.horizontalScale(),
                this.highestFreqInputFactor, this.noiseLevels.length);
        double value = 0.0;
        double valueFactor = this.highestFreqValueFactor;
        for (int i = 0; i < lattices.length; i++) {
            SimplexNoise noiseLevel = this.noiseLevels[i];
            if (noiseLevel != null) {
                value += lattices[i].sample(((SimplexNoiseAccessor) (Object) noiseLevel).exotic_worlds$permutations(),
                        useNoiseStart ? noiseLevel.xo : 0.0, useNoiseStart ? noiseLevel.yo : 0.0, x, z) * valueFactor;
            }

            valueFactor *= 2.0;
        }

        return value;
    }
}
