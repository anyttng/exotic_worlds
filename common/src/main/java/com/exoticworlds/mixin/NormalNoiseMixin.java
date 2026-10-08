package com.exoticworlds.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ClimateFieldMark;
import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.accessors.NoiseScaleRungs;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.GenerationTransformerContext.Context;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(NormalNoise.class)
public class NormalNoiseMixin implements ClimateFieldMark, CoastLiftCache, NoiseScaleRungs {
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

        return this.toroidal$foldedValue(generation, x, y, z) + this.toroidal$coastLift;
    }

    @Unique
    private double toroidal$foldedValue(Context generation, double x, double y, double z) {
        double firstValue = this.first.getValue(x, y, z);
        double detunedScale = generation.horizontalScale() * NoiseConstants.SECOND_LAYER_DETUNE;
        try (Context.ScaleScope scope = generation.withScale(detunedScale)) {
            double detunedY = generation.slotAxes().y().carriesWorldAxis()
                    ? y
                    : y * NoiseConstants.SECOND_LAYER_DETUNE;
            return (firstValue + this.second.getValue(x, detunedY, z)) * this.valueFactor;
        }
    }
}
