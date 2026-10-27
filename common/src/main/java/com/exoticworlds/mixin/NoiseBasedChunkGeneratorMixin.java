package com.exoticworlds.mixin;

import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
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
        if (ShapedChunkGenerator.wrappedFoldOf((NoiseBasedChunkGenerator) (Object) this) == null) {
            return finalDensity;
        }

        return ((NoiseChunkAccessor) noiseChunk).exotic_worlds$randomState().router.finalDensity();
    }

    @WrapMethod(method = "doFill", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileFilling(NoiseChunk noiseChunk, ChunkAccess chunk, Operation<Void> original) {
        GenerationFoldContext.runWithFold(
                ShapedChunkGenerator.foldOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(noiseChunk, chunk));
    }

    @WrapMethod(method = "buildSurface", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileBuildingSurface(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState,
            BiomeManager biomeManager, Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule,
            Operation<Void> original) {
        GenerationFoldContext.runWithFold(
                ShapedChunkGenerator.foldOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(chunk, noiseChunk, randomState, biomeManager, possibleBiomes, materialRule));
    }

    @WrapMethod(method = "generateCarvers", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileCarving(ChunkAccess chunk, Blender blender, NoiseChunk noiseChunk,
            RandomState randomState, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion,
            MaterialRule materialRule, Operation<Void> original) {
        GenerationFoldContext.runWithFold(
                ShapedChunkGenerator.foldOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(chunk, blender, noiseChunk, randomState, biomeManager, carverBiomeRegion,
                        materialRule));
    }

    @WrapMethod(method = "iterateNoiseColumn", order = BinderOrder.FOLD)
    private OptionalInt exotic_worlds$bindWhileReadingColumn(LevelHeightAccessor heightAccessor, RandomState randomState,
            int blockX, int blockZ, @Nullable MutableObject<NoiseColumn> columnReference,
            @Nullable Predicate<BlockState> tester, Operation<OptionalInt> original) {
        return GenerationFoldContext.withFold(
                ShapedChunkGenerator.foldOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(heightAccessor, randomState, blockX, blockZ, columnReference, tester));
    }

    @ModifyReturnValue(method = "getOrigin", at = @At("RETURN"))
    private ChunkPos exotic_worlds$originInsideBounds(ChunkPos origin) {
        return ShapedChunkGenerator.foldOf((NoiseBasedChunkGenerator) (Object) this).fold(origin);
    }
}
