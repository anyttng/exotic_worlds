package com.exoticworlds.compat.tectonic.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.DomainWarp.Divisor;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.exoticworlds.shape.climate.ClimateCompression;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(ConfigNoise.class)
public class ConfigNoiseMixin {
    @Unique
    private static final String COMPUTE = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D";

    @Unique
    private static final String COMPUTE_INTERMEDIARY =
            "method_40464(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D";

    @Unique
    private static final double NO_Y = 0.0;

    @Unique
    private static final double UNSCALED_SHIFT = 1.0;

    @Unique
    private @Nullable Divisor exotic_worlds$warpDivisor;

    @WrapMethod(method = {COMPUTE, COMPUTE_INTERMEDIARY})
    private double exotic_worlds$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(context);
        }

        ConfigNoise self = (ConfigNoise) (Object) this;
        double shiftScale = self.smootherScaling() ? self.scale() : UNSCALED_SHIFT;
        double xzScale = NoiseScaleLadder.installedScale(self.noise(), self.scale());
        double verticalShare = GenerationFoldContext.verticalShare(xzScale, NO_Y);
        double value = ContextScaledNoise.sampleWarped(generation, self.noise(), context.blockX(), NO_Y,
                context.blockZ(), self.shiftX().compute(context) * shiftScale,
                self.shiftZ().compute(context) * shiftScale,
                this.exotic_worlds$warpDivisor(generation.fold(), self, xzScale, verticalShare), xzScale,
                self.scale(), verticalShare);
        return value * self.multiplier() + self.offset();
    }

    @Unique
    private double exotic_worlds$warpDivisor(WorldFold fold, ConfigNoise self, double xzScale,
            double verticalShare) {
        Divisor divisor = this.exotic_worlds$warpDivisor;
        if (divisor == null || divisor.fold() != fold) {
            divisor = new Divisor(fold, ClimateCompression.warpDivisor(self.noise(), fold, xzScale, verticalShare));
            this.exotic_worlds$warpDivisor = divisor;
        }

        return divisor.value();
    }
}
