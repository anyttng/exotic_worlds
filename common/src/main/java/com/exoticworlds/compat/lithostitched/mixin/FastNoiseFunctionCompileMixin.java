package com.exoticworlds.compat.lithostitched.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithostitched.LapFastNoise;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import dev.worldgen.lithostitched.impl.worldgen.densityfunction.FastNoiseFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

@Mixin(FastNoiseFunction.class)
public class FastNoiseFunctionCompileMixin {
    @ModifyExpressionValue(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"))
    private Object exotic_worlds$closedNoise(Object config,
            @Local(argsOnly = true) DensityFunction.CompileContext context) {
        FoldedCompileContext folded = FoldedCompileContext.of(context);
        if (folded == null) {
            return config;
        }

        LapFastNoise noise = LapFastNoise.of(config, folded.fold(), ((FastNoiseFunction) (Object) this).xzScale());
        return noise == null ? config : noise;
    }
}
