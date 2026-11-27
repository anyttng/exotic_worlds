package com.exoticworlds.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.accessors.NoiseHalfHolder;
import com.exoticworlds.accessors.NoiseOriginHolder;
import com.exoticworlds.accessors.NoiseScaleRungs;
import com.exoticworlds.accessors.WrappedNormalNoise;
import com.exoticworlds.accessors.WrappedPerlinNoise;
import com.exoticworlds.core.feature.NoiseOffset;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.ContextKeyedCache;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.NoiseOrigin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(NormalNoise.class)
public class NormalNoiseMixin implements NoiseOriginHolder, NoiseScaleRungs, IndexableNoise,
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
    private volatile NoiseOrigin exotic_worlds$noiseOrigin = NoiseOrigin.NONE;

    @Unique
    private final ContextKeyedCache<NoiseOffset> exotic_worlds$offsets = new ContextKeyedCache<>();

    @Unique
    private volatile Map<Double, Double> exotic_worlds$scaleRungs = Map.of();

    @Unique
    private final ContextKeyedCache<Boolean> exotic_worlds$indexability = new ContextKeyedCache<>();

    @Override
    public NoiseOrigin exotic_worlds$getNoiseOrigin() {
        return this.exotic_worlds$noiseOrigin;
    }

    @Override
    public void exotic_worlds$setNoiseOrigin(NoiseOrigin origin) {
        this.exotic_worlds$noiseOrigin = origin;
        ((NoiseHalfHolder) (Object) this.first).exotic_worlds$setNoiseOrigin(origin, false);
        ((NoiseHalfHolder) (Object) this.second).exotic_worlds$setNoiseOrigin(origin, true);
    }

    @Override
    public double exotic_worlds$getOffsetAt(Context context) {
        NoiseOrigin origin = this.exotic_worlds$noiseOrigin;
        if (origin.noise() == null) {
            return 0.0;
        }

        WorldFold fold = context.getFold();
        NoiseOffset offset = this.exotic_worlds$offsets.find(fold, context);
        if (offset == null) {
            offset = this.exotic_worlds$offsets.put(fold, context,
                    origin.offsetAt(fold, context.getHorizontalScale(), context.getVerticalShare()));
        }

        return offset.get();
    }

    @Override
    public Map<Double, Double> exotic_worlds$getScaleRungs() {
        return this.exotic_worlds$scaleRungs;
    }

    @Override
    public void exotic_worlds$setScaleRungs(Map<Double, Double> rungs) {
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
        double value = this.exotic_worlds$isIndexable(generation)
                ? this.exotic_worlds$foldedValue(generation, x, y, z)
                : ContextScaledNoise.vanillaAtFoldedSlots(generation, (NormalNoise) (Object) this, x, y, z);
        return value + exotic_worlds$getOffsetAt(generation);
    }

    @Override
    public boolean exotic_worlds$isIndexable(Context context) {
        Boolean cached = this.exotic_worlds$indexability.find(context.getFold(), context);
        if (cached != null) {
            return cached;
        }

        return this.exotic_worlds$indexability.put(context.getFold(), context, exotic_worlds$areBothHalvesIndexable(context));
    }

    @Unique
    private boolean exotic_worlds$areBothHalvesIndexable(Context context) {
        if (!((IndexableNoise) (Object) this.first).exotic_worlds$isIndexable(context)) {
            return false;
        }

        try (Context.ScaleScope _ = exotic_worlds$detuned(context)) {
            return ((IndexableNoise) (Object) this.second).exotic_worlds$isIndexable(context);
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
                generation.getVanillaScale() * NoiseConstants.SECOND_LAYER_DETUNE);
    }
}
