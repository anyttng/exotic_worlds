package com.exoticworlds.compat.lithostitched.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithostitched.ClosedNoise;
import com.exoticworlds.compat.lithostitched.LapFastNoise;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.FastNoiseDensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(FastNoiseDensityFunction.class)
public class FastNoiseDensityFunctionMixin {
    @Unique
    private volatile @Nullable ClosedNoise exotic_worlds$closedNoise;

    @WrapMethod(method = {InjectionTargets.DENSITY_FUNCTION_OVERRIDE_COMPUTE,
            InjectionTargets.DENSITY_FUNCTION_OVERRIDE_COMPUTE_INTERMEDIARY})
    private double exotic_worlds$closedCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationFoldContext.wrappedContext();
        if (generation == null) {
            return original.call(context);
        }

        FastNoiseDensityFunction self = (FastNoiseDensityFunction) (Object) this;
        LapFastNoise noise = this.exotic_worlds$closedNoise(generation.fold(), self);
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
            closed = new ClosedNoise(fold, LapFastNoise.of(self.config().value(), fold, self.xzScale()));
            this.exotic_worlds$closedNoise = closed;
        }

        return closed.noise();
    }
}
