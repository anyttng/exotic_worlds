package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseChunk;

@Mixin(NoiseChunk.class)
public class NoiseChunkMixin {
    @ModifyReturnValue(method = "cachedClimateSampler", at = @At("RETURN"))
    private Climate.Sampler toroidal$stampTheFold(Climate.Sampler sampler) {
        ((FoldedClimateSampler) (Object) sampler).toroidal$fold(GenerationTransformerContext.context().transformer());
        return sampler;
    }
}
