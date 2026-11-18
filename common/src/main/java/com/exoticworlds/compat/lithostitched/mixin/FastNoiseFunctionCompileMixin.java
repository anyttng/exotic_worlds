package com.exoticworlds.compat.lithostitched.mixin;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.lithostitched.LapFastNoise;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;
import dev.worldgen.lithostitched.impl.worldgen.densityfunction.FastNoiseFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

@Mixin(FastNoiseFunction.class)
public class FastNoiseFunctionCompileMixin {
    @Unique
    private static final Logger exotic_worlds$LOGGER = LogUtils.getLogger();

    @ModifyExpressionValue(method = InjectionTargets.DENSITY_FUNCTION_COMPILE_SAMPLER,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"))
    private Object exotic_worlds$closedNoise(Object config,
            @Local(argsOnly = true) DensityFunction.CompileContext context) {
        FastNoiseFunction function = (FastNoiseFunction) (Object) this;
        FoldedCompileContext folded = FoldedCompileContext.of(context);
        if (folded == null || !(config instanceof FastNoiseConfig fastNoise)
                || !LapFastNoise.samplesXz(function.xzScale())) {
            return config;
        }

        LapFastNoise.Refusal refusal = LapFastNoise.refusal(fastNoise);
        if (refusal != null) {
            exotic_worlds$LOGGER.warn("[ew/lithostitched-compat] fast_noise_left_open config={} reason={} value={}",
                    function.config().getRegisteredName(), refusal.reason().id(), refusal.value());
            return config;
        }

        return LapFastNoise.of(fastNoise, folded.fold(), function.xzScale());
    }
}
