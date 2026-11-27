package com.exoticworlds.mixin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.AddedStartsHolder;
import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.engine.gen.structure.AddedStructureStarts;
import com.exoticworlds.engine.gen.structure.RingPositions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin implements FoldHolder, AddedStartsHolder {
    @Shadow
    @Final
    private RandomState randomState;

    @Shadow
    @Final
    private BiomeSource biomeSource;

    @Shadow
    @Final
    private long concentricRingsSeed;

    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Unique
    private volatile @Nullable AddedStructureStarts exotic_worlds$addedStarts;

    @Override
    public @Nullable AddedStructureStarts exotic_worlds$getAddedStarts() {
        return this.exotic_worlds$addedStarts;
    }

    @Override
    public void exotic_worlds$setAddedStarts(AddedStructureStarts starts) {
        this.exotic_worlds$addedStarts = starts;
    }

    @Override
    public WorldFold exotic_worlds$getFold() {
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
            @Local(name = "i") int ringIndex,
            @Local(name = "initialX") int ringChunkX,
            @Local(name = "initialZ") int ringChunkZ) {
        return RingPositions.searchOnTheWorld(this.exotic_worlds$fold, ringIndex,
                new ChunkPos(ringChunkX, ringChunkZ), search);
    }

    @ModifyReturnValue(method = "generateRingPositions", at = @At("RETURN"))
    private CompletableFuture<List<ChunkPos>> exotic_worlds$ringsWithinTheWorld(
            CompletableFuture<List<ChunkPos>> original,
            @Local(argsOnly = true) ConcentricRingsStructurePlacement placement) {
        WorldFold fold = this.exotic_worlds$fold;
        if (!fold.isWrapped()) {
            return original;
        }

        return original.thenApply(positions -> RingPositions.worldsShare(fold, positions,
                center -> exotic_worlds$searchAround(placement, center)));
    }

    @Unique
    private ChunkPos exotic_worlds$searchAround(ConcentricRingsStructurePlacement placement, ChunkPos center) {
        Pair<BlockPos, Holder<Biome>> closest = this.biomeSource.findBiomeHorizontal(
                SectionPos.sectionToBlockCoord(center.x(), RingPositions.RING_CHUNK_CENTER_BLOCKS),
                0,
                SectionPos.sectionToBlockCoord(center.z(), RingPositions.RING_CHUNK_CENTER_BLOCKS),
                RingPositions.BIOME_SEARCH_RADIUS_BLOCKS,
                placement.preferredBiomes()::contains,
                RandomSource.create(this.concentricRingsSeed),
                this.randomState);
        return closest == null ? center : ChunkPos.containing(closest.getFirst());
    }
}
