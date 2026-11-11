package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.CarverRecording;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorCarversMixin {
    @WrapOperation(
            method = "generateCarvers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setLargeFeatureSeed(JII)V"))
    private void exotic_worlds$seedCarverFromCanonicalSource(
            WorldgenRandom random, long seed, int sourceChunkX, int sourceChunkZ, Operation<Void> original) {
        WorldFold fold =
                ShapedChunkGenerator.wrappedFoldOf((NoiseBasedChunkGenerator) (Object) this);
        if (fold == null) {
            original.call(random, seed, sourceChunkX, sourceChunkZ);
            return;
        }

        long folded = fold.foldChunkKey(ChunkPos.pack(sourceChunkX, sourceChunkZ));
        original.call(random, seed,
                ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @WrapOperation(
            method = "generateCarvers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/carver/WorldCarver;carve(Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/CarverOutput;)Z"))
    private boolean exotic_worlds$recordCarver(WorldCarver carver, WorldGenerationContext context,
            RandomSource random, ChunkPos chunkPos, ChunkPos sourceChunkPos, CarverOutput output,
            Operation<Boolean> original) {
        CarverOutput recorded = CarverRecording.outputFor(carver, output);
        return original.call(carver, context, random, chunkPos, sourceChunkPos,
                recorded != null ? recorded : output);
    }
}
