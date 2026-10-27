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
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseBasedStateProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(NoiseBasedStateProvider.class)
public class NoiseBasedStateProviderMixin {
    @Shadow
    @Final
    protected NormalNoise noise;

    @WrapMethod(method = "getNoiseValue(Lnet/minecraft/core/BlockPos;D)D")
    private double exotic_worlds$rawCoordinateNoise(BlockPos pos, double scale, Operation<Double> original) {
        Context generation = GenerationFoldContext.context();
        if (!generation.fold().isWrapped()) {
            return original.call(pos, scale);
        }

        return ContextScaledNoise.sample(generation, this.noise,
                pos.getX(), pos.getY() * scale, pos.getZ(), scale);
    }
}
