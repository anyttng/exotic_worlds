package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ContextScaledNoise;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseBasedStateProvider;
import net.minecraft.world.level.levelgen.synth.Noise;

@Mixin(NoiseBasedStateProvider.class)
public class NoiseBasedStateProviderMixin {
    @Shadow
    @Final
    protected Noise noise;

    @WrapMethod(method = "getNoiseValue(Lnet/minecraft/core/BlockPos;D)F")
    private float exotic_worlds$rawCoordinateNoise(BlockPos pos, double scale, Operation<Float> original) {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        if (fold == null) {
            return original.call(pos, scale);
        }

        return ContextScaledNoise.sample(fold, this.noise, scale, pos.getX(), pos.getY() * scale, pos.getZ());
    }
}
