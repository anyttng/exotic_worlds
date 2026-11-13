package com.exoticworlds.compat.tectonic.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.ScaledShiftedNoise;
import com.exoticworlds.compat.worldgen.ShiftedNoiseFold;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.DomainWarp.Divisor;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(ConfigNoise.class)
public class ConfigNoiseMixin implements ScaledShiftedNoise {
    @Unique
    private @Nullable ShiftedNoiseFold exotic_worlds$shiftedNoiseFold;

    @Unique
    private @Nullable Divisor exotic_worlds$warpDivisor;

    @Override
    public DensityFunction.NoiseHolder exotic_worlds$noise() {
        return ((ConfigNoise) (Object) this).noise();
    }

    @Override
    public double exotic_worlds$scale() {
        return ((ConfigNoise) (Object) this).scale();
    }

    @Override
    public boolean exotic_worlds$shiftsScaled() {
        return ((ConfigNoise) (Object) this).smootherScaling();
    }

    @WrapMethod(method = {InjectionTargets.DENSITY_FUNCTION_OVERRIDE_COMPUTE,
            InjectionTargets.DENSITY_FUNCTION_OVERRIDE_COMPUTE_INTERMEDIARY})
    private double exotic_worlds$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(context);
        }

        ConfigNoise self = (ConfigNoise) (Object) this;
        ShiftedNoiseFold fold = this.exotic_worlds$shiftedNoiseFold();
        double value = fold.sample(generation, context.blockX(), context.blockZ(), self.shiftX().compute(context),
                self.shiftZ().compute(context), this.exotic_worlds$warpDivisor(generation.fold(), fold));
        return value * self.multiplier() + self.offset();
    }

    @Unique
    private ShiftedNoiseFold exotic_worlds$shiftedNoiseFold() {
        ShiftedNoiseFold fold = this.exotic_worlds$shiftedNoiseFold;
        if (fold == null) {
            fold = ShiftedNoiseFold.of(this);
            this.exotic_worlds$shiftedNoiseFold = fold;
        }

        return fold;
    }

    @Unique
    private double exotic_worlds$warpDivisor(WorldFold worldFold, ShiftedNoiseFold fold) {
        Divisor divisor = this.exotic_worlds$warpDivisor;
        if (divisor == null || divisor.fold() != worldFold) {
            divisor = new Divisor(worldFold, fold.warpDivisor(worldFold));
            this.exotic_worlds$warpDivisor = divisor;
        }

        return divisor.value();
    }
}
