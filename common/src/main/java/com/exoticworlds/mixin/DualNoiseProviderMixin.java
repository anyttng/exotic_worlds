package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.GenerationFoldContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(DualNoiseProvider.class)
public class DualNoiseProviderMixin {
    @Shadow
    @Final
    private NormalNoise slowNoise;

    @Shadow
    @Final
    private float slowScale;

    @WrapMethod(method = "getSlowNoiseValue(Lnet/minecraft/core/BlockPos;)D")
    private double exotic_worlds$rawCoordinateNoise(BlockPos pos, Operation<Double> original) {
        Context generation = GenerationFoldContext.context();
        if (!generation.fold().isWrapped()) {
            return original.call(pos);
        }

        return ContextScaledNoise.sample(generation, this.slowNoise,
                pos.getX(), pos.getY() * this.slowScale, pos.getZ(), this.slowScale);
    }
}
