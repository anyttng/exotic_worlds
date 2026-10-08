package com.exoticworlds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

@Mixin(DebugScreenOverlay.class)
public class DebugEntryChunkGenerationMixin {
    @WrapOperation(
            method = "getGameInformation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;addDebugScreenInfo(Ljava/util/List;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/core/BlockPos;)V"))
    private void toroidal$sampleThisWorldsNoise(ChunkGenerator generator, List<String> result,
            RandomState randomState, BlockPos feetPos, Operation<Void> original,
            @Local ServerLevel serverLevel) {
        GenerationTransformerContext.runWithTransformer(WorldLoopAttachments.transformerOf(serverLevel),
                () -> original.call(generator, result, randomState, feetPos));
    }

    @WrapOperation(
            method = "getGameInformation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/BiomeSource;addDebugInfo(Ljava/util/List;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"))
    private void toroidal$sampleThisWorldsClimate(BiomeSource biomeSource, List<String> result,
            BlockPos feetPos, Climate.Sampler sampler, Operation<Void> original,
            @Local ServerLevel serverLevel) {
        GenerationTransformerContext.runWithTransformer(WorldLoopAttachments.transformerOf(serverLevel),
                () -> original.call(biomeSource, result, feetPos, sampler));
    }
}
