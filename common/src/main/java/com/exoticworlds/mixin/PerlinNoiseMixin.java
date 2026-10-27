package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClimateCompressionCache;
import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.accessors.WrappedPerlinNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.PeriodicOctaveSampler;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.exoticworlds.shape.climate.ClimateCompression.Resolved;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(PerlinNoise.class)
public class PerlinNoiseMixin implements ClimateCompressionCache, ClimateFieldMark, IndexableNoise,
        WrappedPerlinNoise {
    @Unique
    private @Nullable Resolved exotic_worlds$climateCompression;

    @Unique
    private volatile boolean exotic_worlds$climateField;

    @Shadow
    @Final
    private ImprovedNoise[] noiseLevels;

    @Shadow
    @Final
    private DoubleList amplitudes;

    @Shadow
    @Final
    private double lowestFreqValueFactor;

    @Shadow
    @Final
    private double lowestFreqInputFactor;

    @WrapMethod(method = "getValue(DDDDDZ)D")
    private double exotic_worlds$periodicValue(double x, double y, double z, double yScale, double yFudge,
            boolean useNoiseOrigin, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(x, y, z, yScale, yFudge, useNoiseOrigin);
        }

        return exotic_worlds$sample(generation, x, y, z, yScale, yFudge, useNoiseOrigin);
    }

    @Override
    public double exotic_worlds$sampleWrapped(Context generation, double x, double y, double z, double yScale,
            double yFudge) {
        return exotic_worlds$sample(generation, x, y, z, yScale, yFudge, false);
    }

    @Unique
    private double exotic_worlds$sample(Context generation, double x, double y, double z, double yScale,
            double yFudge, boolean useNoiseOrigin) {
        return PeriodicOctaveSampler.sample(generation, this.exotic_worlds$baseScale(generation), this.noiseLevels,
                this.amplitudes, this.lowestFreqInputFactor, this.lowestFreqValueFactor, x, y, z, yScale, yFudge,
                useNoiseOrigin);
    }

    @Override
    public boolean exotic_worlds$indexable(Context context) {
        return context.wrappedFold() == null || PeriodicOctaveSampler.indexable(context,
                this.exotic_worlds$baseScale(context), this.noiseLevels, this.lowestFreqInputFactor);
    }

    @Unique
    private double exotic_worlds$baseScale(Context generation) {
        double declaredScale = generation.horizontalScale();
        return declaredScale * ClimateCompression.resolve(this, generation.fold(),
                this.exotic_worlds$climateField, this.amplitudes, this.lowestFreqInputFactor, declaredScale,
                generation.verticalShare());
    }

    @Override
    public boolean exotic_worlds$climateField() {
        return this.exotic_worlds$climateField;
    }

    @Override
    public void exotic_worlds$markClimateField() {
        this.exotic_worlds$climateField = true;
    }

    @Override
    public @Nullable Resolved exotic_worlds$climateCompression() {
        return this.exotic_worlds$climateCompression;
    }

    @Override
    public void exotic_worlds$climateCompression(Resolved resolved) {
        this.exotic_worlds$climateCompression = resolved;
    }
}
