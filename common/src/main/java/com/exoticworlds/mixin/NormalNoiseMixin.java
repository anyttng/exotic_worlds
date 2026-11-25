package com.exoticworlds.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.accessors.NoiseFeatureHolder;
import com.exoticworlds.accessors.NoiseOffsetHolder;
import com.exoticworlds.accessors.NoiseScaleRungs;
import com.exoticworlds.accessors.WrappedNormalNoise;
import com.exoticworlds.accessors.WrappedPerlinNoise;
import com.exoticworlds.core.feature.NoiseFeature;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.engine.noise.ContextKeyedCache;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(NormalNoise.class)
public class NormalNoiseMixin implements ClimateFieldMark, NoiseFeatureHolder, NoiseOffsetHolder, NoiseScaleRungs,
        IndexableNoise,
        WrappedNormalNoise {
    @Shadow
    @Final
    private PerlinNoise first;

    @Shadow
    @Final
    private PerlinNoise second;

    @Shadow
    @Final
    private double valueFactor;

    @Unique
    private volatile List<NoiseFeature> exotic_worlds$noiseFeatures = List.of();

    @Unique
    private volatile NoiseOffset exotic_worlds$noiseOffset = NoiseOffset.NONE;

    @Unique
    private volatile Map<Double, Double> exotic_worlds$scaleRungs = Map.of();

    @Unique
    private final ContextKeyedCache<Boolean> exotic_worlds$indexability = new ContextKeyedCache<>();

    @Override
    public void exotic_worlds$markClimateField() {
        ((ClimateFieldMark) (Object) this.first).exotic_worlds$markClimateField();
        ((ClimateFieldMark) (Object) this.second).exotic_worlds$markClimateField();
    }

    @Override
    public List<NoiseFeature> exotic_worlds$getNoiseFeatures() {
        return this.exotic_worlds$noiseFeatures;
    }

    @Override
    public void exotic_worlds$setNoiseFeatures(List<NoiseFeature> features) {
        this.exotic_worlds$noiseFeatures = features;
        ((NoiseFeatureHolder) (Object) this.first).exotic_worlds$setNoiseFeatures(features);
        ((NoiseFeatureHolder) (Object) this.second).exotic_worlds$setNoiseFeatures(features);
    }

    @Override
    public NoiseOffset exotic_worlds$getNoiseOffset() {
        return this.exotic_worlds$noiseOffset;
    }

    @Override
    public void exotic_worlds$setNoiseOffset(NoiseOffset offset) {
        this.exotic_worlds$noiseOffset = offset;
    }

    @Override
    public Map<Double, Double> exotic_worlds$scaleRungs() {
        return this.exotic_worlds$scaleRungs;
    }

    @Override
    public void exotic_worlds$scaleRungs(Map<Double, Double> rungs) {
        this.exotic_worlds$scaleRungs = rungs;
    }

    @WrapMethod(method = "getValue(DDD)D")
    private double exotic_worlds$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(x, y, z);
        }

        return exotic_worlds$sampleWrapped(generation, x, y, z);
    }

    @Override
    public double exotic_worlds$sampleWrapped(Context generation, double x, double y, double z) {
        double value = this.exotic_worlds$indexable(generation)
                ? this.exotic_worlds$foldedValue(generation, x, y, z)
                : ContextScaledNoise.vanillaAtFoldedSlots(generation, (NormalNoise) (Object) this, x, y, z);
        return value + this.exotic_worlds$noiseOffset.get();
    }

    @Override
    public boolean exotic_worlds$indexable(Context context) {
        Boolean cached = this.exotic_worlds$indexability.find(context.getFold(), context);
        if (cached != null) {
            return cached;
        }

        return this.exotic_worlds$indexability.put(context.getFold(), context, exotic_worlds$decideIndexable(context));
    }

    @Unique
    private boolean exotic_worlds$decideIndexable(Context context) {
        if (!((IndexableNoise) (Object) this.first).exotic_worlds$indexable(context)) {
            return false;
        }

        try (Context.ScaleScope _ = exotic_worlds$detuned(context)) {
            return ((IndexableNoise) (Object) this.second).exotic_worlds$indexable(context);
        }
    }

    @Unique
    private double exotic_worlds$foldedValue(Context generation, double x, double y, double z) {
        double firstValue = ((WrappedPerlinNoise) (Object) this.first).exotic_worlds$sampleWrapped(generation, x, y, z,
                0.0, 0.0);
        try (Context.ScaleScope _ = exotic_worlds$detuned(generation)) {
            double detunedY = generation.getSlotAxes().y().hasWorldAxis()
                    ? y
                    : y * NoiseConstants.SECOND_LAYER_DETUNE;
            double secondValue = ((WrappedPerlinNoise) (Object) this.second).exotic_worlds$sampleWrapped(generation,
                    x, detunedY, z, 0.0, 0.0);
            return (firstValue + secondValue) * this.valueFactor;
        }
    }

    @Unique
    private static Context.ScaleScope exotic_worlds$detuned(Context generation) {
        return generation.withScales(generation.getHorizontalScale() * NoiseConstants.SECOND_LAYER_DETUNE,
                generation.vanillaScale() * NoiseConstants.SECOND_LAYER_DETUNE);
    }
}
