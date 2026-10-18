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
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.gen.AddedStructureStarts;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin implements TransformerHolder, AddedStartsHolder {
    // Vanilla searches a radius of 112 blocks, so the answer lies in [center*16 - 104, center*16 + 120].
    @Unique
    private static final int exotic_worlds$BIOME_SEARCH_REACH_CHUNKS = 7;

    @Unique
    private WorldFold exotic_worlds$transformer = WorldFolds.NOOP;

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
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$transformer;
    }

    @Override
    public void exotic_worlds$setTransformer(WorldFold transformer) {
        this.exotic_worlds$transformer = transformer;
    }

    @ModifyArg(
            method = "generateRingPositions",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            index = 0)
    private Supplier<ChunkPos> exotic_worlds$searchOnThisWorldsNoise(Supplier<ChunkPos> search,
            @Local(name = "i") int ringIndex,
            @Local(name = "initialX") int ringChunkX,
            @Local(name = "initialZ") int ringChunkZ) {
        WorldFold transformer = this.exotic_worlds$transformer;
        if (ringIndex > 0
                && transformer.chunkOvershoot(new ChunkPos(ringChunkX, ringChunkZ)) > exotic_worlds$BIOME_SEARCH_REACH_CHUNKS) {
            ChunkPos beyondTheWorld = new ChunkPos(ringChunkX, ringChunkZ);
            return () -> beyondTheWorld;
        }

        return () -> GenerationTransformerContext.withTransformer(transformer, search);
    }

    @ModifyReturnValue(method = "generateRingPositions", at = @At("RETURN"))
    private CompletableFuture<List<ChunkPos>> exotic_worlds$ringsWithinTheWorld(
            CompletableFuture<List<ChunkPos>> original) {
        WorldFold transformer = this.exotic_worlds$transformer;
        if (!transformer.isWrapped()) {
            return original;
        }

        return original.thenApply(positions -> exotic_worlds$theWorldsShare(transformer, positions));
    }

    @Unique
    private static List<ChunkPos> exotic_worlds$theWorldsShare(WorldFold transformer, List<ChunkPos> positions) {
        List<ChunkPos> inBounds = new ArrayList<>(positions.size());
        for (ChunkPos position : positions) {
            if (!transformer.isOver(position)) {
                inBounds.add(position);
            }
        }

        if (inBounds.size() == positions.size()) {
            return positions;
        }

        if (!inBounds.isEmpty()) {
            return List.copyOf(inBounds);
        }

        return List.of(transformer.fold(positions.getFirst()));
    }
}
