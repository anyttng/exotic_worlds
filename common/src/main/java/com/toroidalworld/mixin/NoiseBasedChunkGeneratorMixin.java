package com.toroidalworld.mixin;

import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.BinderOrder;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
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
    private DensityFunction toroidal$fillUnderTheCeiling(DensityFunction finalDensity, @Local NoiseChunk noiseChunk) {
        if (ShapedChunkGenerator.wrappedTransformerOf((NoiseBasedChunkGenerator) (Object) this) == null) {
            return finalDensity;
        }

        return ((NoiseChunkAccessor) noiseChunk).toroidal$randomState().router.finalDensity();
    }

    @WrapMethod(method = "doFill", order = BinderOrder.FOLD)
    private void toroidal$bindWhileFilling(NoiseChunk noiseChunk, ChunkAccess chunk, Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(noiseChunk, chunk));
    }

    @WrapMethod(method = "buildSurface", order = BinderOrder.FOLD)
    private void toroidal$bindWhileBuildingSurface(ChunkAccess chunk, NoiseChunk noiseChunk, RandomState randomState,
            BiomeManager biomeManager, Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(chunk, noiseChunk, randomState, biomeManager, possibleBiomes, materialRule));
    }

    @WrapMethod(method = "generateCarvers", order = BinderOrder.FOLD)
    private void toroidal$bindWhileCarving(ChunkAccess chunk, Blender blender, NoiseChunk noiseChunk,
            RandomState randomState, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion,
            MaterialRule materialRule, Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(chunk, blender, noiseChunk, randomState, biomeManager, carverBiomeRegion,
                        materialRule));
    }

    @WrapMethod(method = "iterateNoiseColumn", order = BinderOrder.FOLD)
    private OptionalInt toroidal$bindWhileReadingColumn(LevelHeightAccessor heightAccessor, RandomState randomState,
            int blockX, int blockZ, @Nullable MutableObject<NoiseColumn> columnReference,
            @Nullable Predicate<BlockState> tester, Operation<OptionalInt> original) {
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(heightAccessor, randomState, blockX, blockZ, columnReference, tester));
    }

    @ModifyReturnValue(method = "getOrigin", at = @At("RETURN"))
    private ChunkPos toroidal$originInsideBounds(ChunkPos origin) {
        return ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this).fold(origin);
    }
}
