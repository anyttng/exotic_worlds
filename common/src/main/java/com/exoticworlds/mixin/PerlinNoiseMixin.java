package com.exoticworlds.mixin;

import java.util.Arrays;


import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClimateCompressionCache;
import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.accessors.NoiseHalfHolder;
import com.exoticworlds.accessors.WrappedPerlinNoise;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.NoiseOrigin;
import com.exoticworlds.engine.noise.periodic.PeriodicOctaveSampler;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression;
import com.exoticworlds.shape.feature.compactbiomes.ClimateCompression.Resolved;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(PerlinNoise.class)
public class PerlinNoiseMixin implements ClimateCompressionCache, ClimateFieldMark, IndexableNoise,
        NoiseHalfHolder, WrappedPerlinNoise {
    @Unique
    private @Nullable Resolved exotic_worlds$climateCompression;

    @Unique
    private volatile boolean exotic_worlds$climateField;

    @Unique
    private volatile NoiseOrigin exotic_worlds$noiseOrigin = NoiseOrigin.NONE;

    @Unique
    private volatile boolean exotic_worlds$isSecondHalf;

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

    @WrapMethod(method = "getValue(DDDDD)D")
    private double exotic_worlds$periodicValue(double x, double y, double z, double yScale, double yFudge, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(x, y, z, yScale, yFudge);
        }

        return exotic_worlds$sampleWrapped(generation, x, y, z, yScale, yFudge);
    }

    @Override
    public double exotic_worlds$sampleWrapped(Context generation, double x, double y, double z, double yScale,
            double yFudge) {
        return PeriodicOctaveSampler.sample(generation, generation.getHorizontalScale(),
                this.exotic_worlds$octaveFactors(generation), this.noiseLevels, this.amplitudes,
                this.lowestFreqInputFactor, this.lowestFreqValueFactor, x, y, z, yScale, yFudge);
    }

    @Override
    public boolean exotic_worlds$indexable(Context context) {
        return context.getWrappedFold() == null || PeriodicOctaveSampler.indexable(context,
                context.getHorizontalScale(), this.exotic_worlds$octaveFactors(context), this.noiseLevels,
                this.lowestFreqInputFactor);
    }

    @Unique
    private DoubleList exotic_worlds$octaveFactors(Context generation) {
        double declaredScale = generation.getHorizontalScale();
        WorldFold fold = generation.getFold();
        double verticalShare = generation.verticalShare();
        return ClimateCompression.resolve(this, fold, declaredScale, verticalShare,
                () -> exotic_worlds$octaveFactorsAt(fold, declaredScale, verticalShare));
    }

    @Unique
    private DoubleList exotic_worlds$octaveFactorsAt(WorldFold fold, double declaredScale, double verticalShare) {
        double[] octaveFactors = new double[this.noiseLevels.length];
        Arrays.fill(octaveFactors, NoiseFeature.UNCHANGED);
        NoiseOrigin origin = this.exotic_worlds$noiseOrigin;
        if (origin.noise() == null) {
            return DoubleList.of(octaveFactors);
        }

        boolean isSecondHalf = this.exotic_worlds$isSecondHalf;
        double noiseScale = isSecondHalf ? declaredScale / NoiseConstants.SECOND_LAYER_DETUNE : declaredScale;
        double[] layerFactors = origin.layerFactorsAt(fold, noiseScale, verticalShare);
        int layer = isSecondHalf ? 1 : 0;
        for (int octave = 0; octave < octaveFactors.length; octave++) {
            if (this.noiseLevels[octave] != null) {
                octaveFactors[octave] = layerFactors[layer];
                layer += 2;
            }
        }

        return DoubleList.of(octaveFactors);
    }

    @Override
    public void exotic_worlds$setNoiseOrigin(NoiseOrigin origin, boolean isSecondHalf) {
        this.exotic_worlds$noiseOrigin = origin;
        this.exotic_worlds$isSecondHalf = isSecondHalf;
        this.exotic_worlds$climateCompression = null;
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
