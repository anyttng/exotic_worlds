package com.exoticworlds.mixin;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.accessors.FoldedRandomState;
import com.exoticworlds.core.gen.GeneratorShapes;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.gen.BaseHeights;
import com.exoticworlds.engine.gen.SpawnSearch;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SpawnTargetPoint;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {
    @ModifyExpressionValue(
            method = {"doFill", "iterateNoiseColumn"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;finalDensity()"
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction;"))
    private DensityFunction exotic_worlds$fillUnderTheCeiling(DensityFunction finalDensity, @Local NoiseChunk noiseChunk) {
        if (GeneratorShapes.getWrappedFold((NoiseBasedChunkGenerator) (Object) this) == null) {
            return finalDensity;
        }

        return ((NoiseChunkAccessor) noiseChunk).exotic_worlds$getRandomState().router.finalDensity();
    }

    @WrapMethod(method = "createNoiseChunk", order = BinderOrder.FOLD)
    private NoiseChunk exotic_worlds$bindWhileCreatingNoiseChunk(ChunkAccess chunk, StructureManager structureManager,
            Blender blender, RandomState randomState, NoiseSettings noiseSettings, Operation<NoiseChunk> original) {
        WorldFold fold = GeneratorShapes.getFold((NoiseBasedChunkGenerator) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.getContext().withFold(fold, chunk.getPos())) {
            return original.call(chunk, structureManager, blender, randomState, noiseSettings);
        }
    }

    @WrapMethod(method = "doFill", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileFilling(NoiseChunk noiseChunk, ChunkAccess chunk, Operation<Void> original) {
        WorldFold fold = GeneratorShapes.getFold((NoiseBasedChunkGenerator) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.getContext().withFold(fold, chunk.getPos())) {
            original.call(noiseChunk, chunk);
        }
    }

    @WrapMethod(method = "buildSurface", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileBuildingSurface(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState,
            BiomeManager biomeManager, Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule,
            Operation<Void> original) {
        WorldFold fold = GeneratorShapes.getFold((NoiseBasedChunkGenerator) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.getContext().withFold(fold, chunk.getPos())) {
            original.call(chunk, noiseChunk, randomState, biomeManager, possibleBiomes, materialRule);
        }
    }

    @WrapMethod(method = "generateCarvers", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileCarving(ChunkAccess chunk, Blender blender, NoiseChunk noiseChunk,
            RandomState randomState, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion,
            MaterialRule materialRule, Operation<Void> original) {
        WorldFold fold = GeneratorShapes.getFold((NoiseBasedChunkGenerator) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.getContext().withFold(fold, chunk.getPos())) {
            original.call(chunk, blender, noiseChunk, randomState, biomeManager, carverBiomeRegion, materialRule);
        }
    }

    @WrapMethod(method = "iterateNoiseColumn", order = BinderOrder.FOLD)
    private OptionalInt exotic_worlds$bindWhileReadingColumn(LevelHeightAccessor heightAccessor, RandomState randomState,
            int blockX, int blockZ, @Nullable MutableObject<NoiseColumn> columnReference,
            @Nullable Predicate<BlockState> tester, Operation<OptionalInt> original) {
        WorldFold fold = GeneratorShapes.getFold((NoiseBasedChunkGenerator) (Object) this);
        ChunkPos columnChunk =
                new ChunkPos(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
        try (GenerationFoldContext.Context.FoldScope _ =
                GenerationFoldContext.getContext().withFold(fold, columnChunk)) {
            return original.call(heightAccessor, randomState, blockX, blockZ, columnReference, tester);
        }
    }

    @WrapMethod(method = "getBaseHeight")
    private int exotic_worlds$readFoldedBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor,
            RandomState randomState, Operation<Integer> original) {
        BaseHeights heights = ((FoldedRandomState) (Object) randomState).exotic_worlds$getBaseHeights();
        if (heights == null) {
            return original.call(x, z, type, heightAccessor, randomState);
        }

        return heights.heightAt(x, z, type, () -> original.call(x, z, type, heightAccessor, randomState));
    }

    @WrapOperation(
            method = "getOrigin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseSpawnFinder;findSpawnPosition(Ljava/util/List;"
                            + "Lnet/minecraft/world/level/levelgen/densityfunction/DensitySamplerSet;)"
                            + "Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$searchOverTheSurface(List<SpawnTargetPoint> targetPoints, DensitySamplerSet samplers,
            Operation<BlockPos> original) {
        return SpawnSearch.overSurface((NoiseBasedChunkGenerator) (Object) this,
                () -> original.call(targetPoints, samplers));
    }

    @ModifyReturnValue(method = "getOrigin", at = @At("RETURN"))
    private ChunkPos exotic_worlds$originInsideBounds(ChunkPos origin) {
        return GeneratorShapes.getFold((NoiseBasedChunkGenerator) (Object) this).fold(origin);
    }
}
