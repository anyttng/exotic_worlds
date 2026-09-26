package com.toroidalworld.mixin;

import java.util.OptionalInt;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.BinderOrder;
import com.toroidalworld.core.ShapedChunkGenerator;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {
    @WrapMethod(method = "doFill", order = BinderOrder.FOLD)
    private ChunkAccess toroidal$bindWhileFilling(
            Blender blender,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess centerChunk,
            int cellYMin,
            int cellCountY,
            Operation<ChunkAccess> original) {
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY));
    }

    @WrapMethod(method = "doCreateBiomes", order = BinderOrder.FOLD)
    private void toroidal$bindWhileCreatingBiomes(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess protoChunk,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(blender, randomState, structureManager, protoChunk));
    }

    @WrapMethod(method = "iterateNoiseColumn", order = BinderOrder.FOLD)
    private OptionalInt toroidal$bindWhileReadingColumn(
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            int blockX,
            int blockZ,
            @Nullable MutableObject<NoiseColumn> columnReference,
            @Nullable Predicate<BlockState> tester,
            Operation<OptionalInt> original) {
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf((NoiseBasedChunkGenerator) (Object) this),
                () -> original.call(heightAccessor, randomState, blockX, blockZ, columnReference, tester));
    }
}
