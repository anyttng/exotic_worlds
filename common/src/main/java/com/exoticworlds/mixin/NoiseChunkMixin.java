package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldedClimateSampler;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseChunk;

@Mixin(NoiseChunk.class)
public class NoiseChunkMixin {
    @Shadow
    @Final
    private int firstNoiseX;

    @Shadow
    @Final
    private int firstNoiseZ;

    @ModifyReturnValue(method = "cachedClimateSampler", at = @At("RETURN"))
    private Climate.Sampler exotic_worlds$stampTheFold(Climate.Sampler sampler) {
        WorldFold fold = GenerationFoldContext.context().fold();
        ChunkPos chunk = new ChunkPos(QuartPos.toSection(this.firstNoiseX), QuartPos.toSection(this.firstNoiseZ));
        ((FoldedClimateSampler) (Object) sampler).exotic_worlds$fold(fold.isOver(chunk) ? WorldFolds.NOOP : fold);
        return sampler;
    }
}
