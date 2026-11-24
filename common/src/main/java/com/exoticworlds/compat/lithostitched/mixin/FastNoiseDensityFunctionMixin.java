package com.exoticworlds.compat.lithostitched.mixin;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.FoldableFastNoise;
import com.exoticworlds.compat.lithostitched.ClosedNoise;
import com.exoticworlds.compat.lithostitched.LapFastNoise;
import com.exoticworlds.compat.lithostitched.LapFastNoiseConfig;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.logging.LogUtils;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;
import dev.worldgen.lithostitched.impl.worldgen.densityfunction.FastNoiseDensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(FastNoiseDensityFunction.class)
public class FastNoiseDensityFunctionMixin implements FoldableFastNoise {
    @Unique
    private static final Logger exotic_worlds$LOGGER = LogUtils.getLogger();

    @Unique
    private volatile @Nullable ClosedNoise exotic_worlds$closedNoise;

    @Override
    public @Nullable Object exotic_worlds$closedConfig(WorldFold fold) {
        FastNoiseDensityFunction self = (FastNoiseDensityFunction) (Object) this;
        LapFastNoise noise = this.exotic_worlds$closedNoise(fold, self);
        return noise == null ? null : new LapFastNoiseConfig(self.config().value(), noise);
    }

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double exotic_worlds$closedCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(context);
        }

        FastNoiseDensityFunction self = (FastNoiseDensityFunction) (Object) this;
        LapFastNoise noise = this.exotic_worlds$closedNoise(generation.getFold(), self);
        if (noise == null) {
            return original.call(context);
        }

        return noise.get(context.blockX() * self.xzScale() + self.shiftX().compute(context),
                context.blockY() * self.yScale() + self.shiftY().compute(context),
                context.blockZ() * self.xzScale() + self.shiftZ().compute(context));
    }

    @Unique
    private @Nullable LapFastNoise exotic_worlds$closedNoise(WorldFold fold, FastNoiseDensityFunction self) {
        ClosedNoise closed = this.exotic_worlds$closedNoise;
        if (closed == null || closed.fold() != fold) {
            closed = new ClosedNoise(fold, exotic_worlds$lapOf(fold, self));
            this.exotic_worlds$closedNoise = closed;
        }

        return closed.noise();
    }

    @Unique
    private static @Nullable LapFastNoise exotic_worlds$lapOf(WorldFold fold, FastNoiseDensityFunction self) {
        FastNoiseConfig config = self.config().value();
        if (!LapFastNoise.isSamplingXz(self.xzScale())) {
            return null;
        }

        LapFastNoise.Refusal refusal = LapFastNoise.refusal(config);
        if (refusal != null) {
            exotic_worlds$LOGGER.warn("[ew/lithostitched-compat] fast_noise_left_open config={} reason={} value={}",
                    self.config().getRegisteredName(), refusal.reason().getId(), refusal.value());
            return null;
        }

        return LapFastNoise.of(config, fold, self.xzScale());
    }
}
