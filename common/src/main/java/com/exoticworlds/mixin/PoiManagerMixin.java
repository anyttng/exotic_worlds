package com.exoticworlds.mixin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.fold.FoldedOrder;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.exoticworlds.engine.seam.PoiSeamSearch;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.status.ChunkStatus;

@Mixin(value = PoiManager.class, priority = 1100)
public class PoiManagerMixin {
    @Unique
    private static final String FIND_CLOSEST =
            "findClosest(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;";

    @Unique
    private static final String FIND_CLOSEST_FILTERED =
            "findClosest(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;";

    @Shadow
    @Final
    private LongSet loadedChunks;

    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void exotic_worlds$bindLevel(CallbackInfo ci) {
        this.exotic_worlds$bound.bind(((SectionStorageAccessor) this).exotic_worlds$getLevelHeightAccessor());
    }

    @WrapMethod(method = "getInSquare")
    private Stream<PoiRecord> exotic_worlds$squareThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            return original.call(predicate, center, radiusBlocks, occupancy);
        }

        PoiManager self = (PoiManager) (Object) this;
        int radiusChunks = Math.floorDiv(radiusBlocks, CoordinateConstants.CHUNK_WIDTH_BLOCKS) + 1;

        return PoiSeamSearch.chunksAround(fold, ChunkPos.containing(center), radiusChunks)
                .flatMap(chunkPos -> self.getInChunk(predicate, chunkPos, occupancy))
                .filter(record -> PoiSeamSearch.isWithinSquare(fold, center, radiusBlocks, record.getPos()));
    }

    @WrapMethod(method = "getInRange")
    private Stream<PoiRecord> exotic_worlds$rangeThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            return original.call(predicate, center, radiusBlocks, occupancy);
        }

        double radiusSqr = (double) radiusBlocks * radiusBlocks;
        return ((PoiManager) (Object) this).getInSquare(predicate, center, radiusBlocks, occupancy)
                .filter(record -> SeamRange.sqr(fold, center, record.getPos()) <= radiusSqr);
    }

    @WrapMethod(method = FIND_CLOSEST)
    private Optional<BlockPos> exotic_worlds$closestThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            return original.call(predicate, center, radiusBlocks, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radiusBlocks, occupancy)
                .map(PoiRecord::getPos)
                .min(PoiSeamSearch.byDistance(fold, center));
    }

    @WrapMethod(method = FIND_CLOSEST_FILTERED)
    private Optional<BlockPos> exotic_worlds$closestFilteredThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            return original.call(predicate, filter, center, radiusBlocks, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radiusBlocks, occupancy)
                .map(PoiRecord::getPos)
                .filter(filter)
                .min(PoiSeamSearch.byDistance(fold, center));
    }

    @WrapMethod(method = "findClosestWithType")
    private Optional<Pair<Holder<PoiType>, BlockPos>> exotic_worlds$closestWithTypeThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Optional<Pair<Holder<PoiType>, BlockPos>>> original) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            return original.call(predicate, center, radiusBlocks, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radiusBlocks, occupancy)
                .min(Comparator.comparingDouble(record -> SeamRange.sqr(fold, center, record.getPos())))
                .map(record -> Pair.of(record.getPoiType(), record.getPos()));
    }

    @WrapMethod(method = "find")
    private Optional<BlockPos> exotic_worlds$findThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        if (this.exotic_worlds$bound.getWrapped() == null) {
            return original.call(predicate, filter, center, radiusBlocks, occupancy);
        }

        return ((PoiManager) (Object) this).findAll(predicate, filter, center, radiusBlocks, occupancy).findFirst();
    }

    @WrapMethod(method = "getRandom")
    private Optional<BlockPos> exotic_worlds$randomThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            PoiManager.Occupancy occupancy,
            BlockPos center,
            int radiusBlocks,
            RandomSource random,
            Operation<Optional<BlockPos>> original) {
        if (this.exotic_worlds$bound.getWrapped() == null) {
            return original.call(predicate, filter, occupancy, center, radiusBlocks, random);
        }

        List<PoiRecord> shuffled = Util.toShuffledList(
                ((PoiManager) (Object) this).getInRange(predicate, center, radiusBlocks, occupancy), random);
        return shuffled.stream().filter(record -> filter.test(record.getPos())).findFirst().map(PoiRecord::getPos);
    }

    @WrapMethod(method = "getCountInRange")
    private long exotic_worlds$countThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radiusBlocks,
            PoiManager.Occupancy occupancy,
            Operation<Long> original) {
        if (this.exotic_worlds$bound.getWrapped() == null) {
            return original.call(predicate, center, radiusBlocks, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radiusBlocks, occupancy).count();
    }

    @ModifyArg(
            method = "findAllClosestFirstWithType",
            at = @At(value = "INVOKE", target = InjectionTargets.STREAM_SORTED),
            index = 0)
    private Comparator<Pair<Holder<PoiType>, BlockPos>> exotic_worlds$sortThroughSeam(
            Comparator<Pair<Holder<PoiType>, BlockPos>> original, @Local(argsOnly = true) BlockPos center) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            return original;
        }

        return FoldedOrder.of(original, pair -> {
            BlockPos pos = pair.getSecond();
            BlockPos nearest = fold.nearestCopy(center, pos);
            return nearest == pos ? pair : Pair.of(pair.getFirst(), nearest);
        });
    }

    @WrapMethod(method = "ensureLoadedAndValid")
    private void exotic_worlds$loadThroughSeam(LevelReader reader, BlockPos center, int radiusBlocks, Operation<Void> original) {
        WorldFold fold = this.exotic_worlds$bound.getWrapped();
        if (fold == null) {
            original.call(reader, center, radiusBlocks);
            return;
        }

        SectionStorageAccessor storage = (SectionStorageAccessor) this;
        LevelHeightAccessor height = storage.exotic_worlds$getLevelHeightAccessor();
        int radiusChunks = Math.floorDiv(radiusBlocks, CoordinateConstants.CHUNK_WIDTH_BLOCKS);
        PoiSeamSearch.chunksAround(fold, ChunkPos.containing(center), radiusChunks)
                .filter(chunk -> IntStream.rangeClosed(height.getMinSectionY(), height.getMaxSectionY())
                        .anyMatch(sectionY -> !storage.exotic_worlds$getOrLoad(SectionPos.of(chunk, sectionY).asLong())
                                .map(section -> ((PoiSection) section).isValid())
                                .orElse(false)))
                .filter(chunk -> this.loadedChunks.add(chunk.pack()))
                .forEach(chunk -> reader.getChunk(chunk.x(), chunk.z(), ChunkStatus.EMPTY));
    }

    @WrapMethod(method = "sectionsToVillage")
    private int exotic_worlds$villageDistanceThroughSeam(SectionPos sectionPos, Operation<Integer> original) {
        return original.call(this.exotic_worlds$bound.getFold().fold(sectionPos));
    }

    @ModifyVariable(
            method = {"add", "remove", "release", "exists", "getType", "getDebugPoiInfo"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private BlockPos exotic_worlds$positionThroughSeam(BlockPos pos) {
        return this.exotic_worlds$bound.getFold().fold(pos);
    }
}
