package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseChunk;

@Mixin(NoiseChunk.class)
public class NoiseChunkMixin {
    @ModifyReturnValue(method = "cachedClimateSampler", at = @At("RETURN"))
    private Climate.Sampler exotic_worlds$stampTheFold(Climate.Sampler sampler) {
        ((FoldedClimateSampler) (Object) sampler).exotic_worlds$fold(GenerationFoldContext.context().fold());
        return sampler;
    }
}
