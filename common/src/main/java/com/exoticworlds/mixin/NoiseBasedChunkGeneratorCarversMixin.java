package com.exoticworlds.mixin;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.CarverRecording;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorCarversMixin {
    @WrapOperation(
            method = "applyCarvers",
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

        long folded = fold.foldChunkKey(ChunkPos.asLong(sourceChunkX, sourceChunkZ));
        original.call(random, seed,
                ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @WrapOperation(
            method = "applyCarvers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/carver/ConfiguredWorldCarver;carve(Lnet/minecraft/world/level/levelgen/carver/CarvingContext;Lnet/minecraft/world/level/chunk/ChunkAccess;Ljava/util/function/Function;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/Aquifer;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/CarvingMask;)Z"))
    private boolean exotic_worlds$recordCarver(ConfiguredWorldCarver<?> carver, CarvingContext context,
            ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter, RandomSource random, Aquifer aquifer,
            ChunkPos sourceChunkPos, CarvingMask mask, Operation<Boolean> original) {
        long @Nullable [] before = CarverRecording.before(mask);
        boolean carved = original.call(carver, context, chunk, biomeGetter, random, aquifer, sourceChunkPos, mask);
        CarverRecording.after(carver, mask, before);
        return carved;
    }
}
