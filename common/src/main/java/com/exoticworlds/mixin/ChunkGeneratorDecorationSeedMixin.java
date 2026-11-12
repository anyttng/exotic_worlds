package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorDecorationSeedMixin {
    @WrapOperation(
            method = "applyBiomeDecoration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setDecorationSeed(JII)J"))
    private long exotic_worlds$seedDecorationFromCanonicalOrigin(
            WorldgenRandom random, long seed, int originX, int originZ, Operation<Long> original) {
        WorldFold fold = ShapedChunkGenerator.foldOf((ChunkGenerator) (Object) this);
        BlockPos folded = fold.fold(new BlockPos(originX, 0, originZ));
        return original.call(random, seed, folded.getX(), folded.getZ());
    }
}
