package com.exoticworlds.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.AddedStartsHolder;
import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.gen.AddedStructureStarts;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin implements FoldHolder, AddedStartsHolder {
    // Vanilla searches a radius of 112 blocks, so the answer lies in [center*16 - 104, center*16 + 120].
    @Unique
    private static final int exotic_worlds$BIOME_SEARCH_REACH_CHUNKS = 7;

    @Unique
    private static final int exotic_worlds$RING_POSITION_SLOT = 14;

    @Unique
    private static final int exotic_worlds$RING_CHUNK_X_SLOT = 17;

    @Unique
    private static final int exotic_worlds$RING_CHUNK_Z_SLOT = 18;

    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Unique
    private volatile @Nullable AddedStructureStarts exotic_worlds$addedStarts;

    @Override
    public @Nullable AddedStructureStarts exotic_worlds$addedStarts() {
        return this.exotic_worlds$addedStarts;
    }

    @Override
    public void exotic_worlds$addedStarts(AddedStructureStarts starts) {
        this.exotic_worlds$addedStarts = starts;
    }

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(WorldFold fold) {
        this.exotic_worlds$fold = fold;
    }

    @ModifyArg(
            method = "generateRingPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            index = 0)
    private Supplier<ChunkPos> exotic_worlds$searchOnThisWorldsNoise(Supplier<ChunkPos> search,
            @Local(index = exotic_worlds$RING_POSITION_SLOT) int ringIndex,
            @Local(index = exotic_worlds$RING_CHUNK_X_SLOT) int ringChunkX,
            @Local(index = exotic_worlds$RING_CHUNK_Z_SLOT) int ringChunkZ) {
        WorldFold fold = this.exotic_worlds$fold;
        if (ringIndex > 0
                && fold.chunkOvershoot(new ChunkPos(ringChunkX, ringChunkZ)) > exotic_worlds$BIOME_SEARCH_REACH_CHUNKS) {
            ChunkPos beyondTheWorld = new ChunkPos(ringChunkX, ringChunkZ);
            return () -> beyondTheWorld;
        }

        return () -> GenerationFoldContext.withFold(fold, search);
    }

    @ModifyReturnValue(method = "generateRingPositions", at = @At("RETURN"))
    private CompletableFuture<List<ChunkPos>> exotic_worlds$ringsWithinTheWorld(
            CompletableFuture<List<ChunkPos>> original) {
        WorldFold fold = this.exotic_worlds$fold;
        if (!fold.isWrapped()) {
            return original;
        }

        return original.thenApply(positions -> exotic_worlds$theWorldsShare(fold, positions));
    }

    @Unique
    private static List<ChunkPos> exotic_worlds$theWorldsShare(WorldFold fold, List<ChunkPos> positions) {
        List<ChunkPos> inBounds = new ArrayList<>(positions.size());
        for (ChunkPos position : positions) {
            if (!fold.isOver(position)) {
                inBounds.add(position);
            }
        }

        if (inBounds.size() == positions.size()) {
            return positions;
        }

        if (!inBounds.isEmpty()) {
            return List.copyOf(inBounds);
        }

        return List.of(fold.fold(positions.getFirst()));
    }
}
