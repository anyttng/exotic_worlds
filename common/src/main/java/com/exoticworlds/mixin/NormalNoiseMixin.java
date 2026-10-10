package com.exoticworlds.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.accessors.IndexableNoise;
import com.exoticworlds.accessors.NoiseScaleRungs;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(NormalNoise.class)
public class NormalNoiseMixin implements ClimateFieldMark, CoastLiftCache, NoiseScaleRungs, IndexableNoise {
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
    private volatile double toroidal$coastLift;

    @Unique
    private volatile Map<Double, Double> toroidal$scaleRungs = Map.of();

    @Override
    public void toroidal$markClimateField() {
        ((ClimateFieldMark) (Object) this.first).toroidal$markClimateField();
        ((ClimateFieldMark) (Object) this.second).toroidal$markClimateField();
    }

    @Override
    public double toroidal$coastLift() {
        return this.toroidal$coastLift;
    }

    @Override
    public void toroidal$coastLift(double lift) {
        this.toroidal$coastLift = lift;
    }

    @Override
    public Map<Double, Double> toroidal$scaleRungs() {
        return this.toroidal$scaleRungs;
    }

    @Override
    public void toroidal$scaleRungs(Map<Double, Double> rungs) {
        this.toroidal$scaleRungs = rungs;
    }

    @WrapMethod(method = "getValue(DDD)D")
    private double toroidal$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z);
        }

        double value = this.toroidal$indexable(generation)
                ? this.toroidal$foldedValue(generation, x, y, z)
                : ContextScaledNoise.vanillaAtFoldedSlots(generation, (NormalNoise) (Object) this, x, y, z);
        return value + this.toroidal$coastLift;
    }

    @Override
    public boolean toroidal$indexable(Context context) {
        if (!((IndexableNoise) (Object) this.first).toroidal$indexable(context)) {
            return false;
        }

        try (Context.ScaleScope _ = toroidal$detuned(context)) {
            return ((IndexableNoise) (Object) this.second).toroidal$indexable(context);
        }
    }

    @Unique
    private double toroidal$foldedValue(Context generation, double x, double y, double z) {
        double firstValue = this.first.getValue(x, y, z);
        try (Context.ScaleScope _ = toroidal$detuned(generation)) {
            double detunedY = generation.slotAxes().y().carriesWorldAxis()
                    ? y
                    : y * NoiseConstants.SECOND_LAYER_DETUNE;
            return (firstValue + this.second.getValue(x, detunedY, z)) * this.valueFactor;
        }
    }

    @Unique
    private static Context.ScaleScope toroidal$detuned(Context generation) {
        return generation.withScales(generation.horizontalScale() * NoiseConstants.SECOND_LAYER_DETUNE,
                generation.vanillaScale() * NoiseConstants.SECOND_LAYER_DETUNE);
    }
}
