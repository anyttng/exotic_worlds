package com.exoticworlds.mixin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedOrder;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
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
    private static final String FIND_CLOSEST =
            "findClosest(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;";
    private static final String FIND_CLOSEST_FILTERED =
            "findClosest(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/Optional;";

    @Shadow
    @Final
    private LongSet loadedChunks;
    @WrapMethod(method = "getInSquare")
    private Stream<PoiRecord> toroidal$squareThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        PoiManager self = (PoiManager) (Object) this;
        int chunkRadius = Math.floorDiv(radius, CoordinateConstants.CHUNK_WIDTH) + 1;

        return toroidal$chunksAround(ChunkPos.containing(center), chunkRadius, transformer)
                .flatMap(chunkPos -> self.getInChunk(predicate, chunkPos, occupancy))
                .filter(record -> {
                    BlockPos pos = record.getPos();
                    BlockPos nearest = transformer.nearestCopy(center, pos);
                    return Math.abs(nearest.getX() - center.getX()) <= radius
                            && Math.abs(nearest.getZ() - center.getZ()) <= radius;
                });
    }

    @WrapMethod(method = "getInRange")
    private Stream<PoiRecord> toroidal$rangeThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        double radiusSqr = (double) radius * radius;
        return ((PoiManager) (Object) this).getInSquare(predicate, center, radius, occupancy)
                .filter(record -> SeamRange.sqr(transformer, center, record.getPos()) <= radiusSqr);
    }

    @WrapMethod(method = FIND_CLOSEST)
    private Optional<BlockPos> toroidal$closestThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy)
                .map(PoiRecord::getPos)
                .min(toroidal$byDistance(transformer, center));
    }

    @WrapMethod(method = FIND_CLOSEST_FILTERED)
    private Optional<BlockPos> toroidal$closestFilteredThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, filter, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy)
                .map(PoiRecord::getPos)
                .filter(filter)
                .min(toroidal$byDistance(transformer, center));
    }

    @WrapMethod(method = "findClosestWithType")
    private Optional<Pair<Holder<PoiType>, BlockPos>> toroidal$closestWithTypeThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<Pair<Holder<PoiType>, BlockPos>>> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy)
                .min(Comparator.comparingDouble(record -> SeamRange.sqr(transformer, center, record.getPos())))
                .map(record -> Pair.of(record.getPoiType(), record.getPos()));
    }

    @WrapMethod(method = "find")
    private Optional<BlockPos> toroidal$findThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        if (toroidal$transformer() == null) {
            return original.call(predicate, filter, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).findAll(predicate, filter, center, radius, occupancy).findFirst();
    }

    @WrapMethod(method = "getRandom")
    private Optional<BlockPos> toroidal$randomThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            PoiManager.Occupancy occupancy,
            BlockPos center,
            int radius,
            RandomSource random,
            Operation<Optional<BlockPos>> original) {
        if (toroidal$transformer() == null) {
            return original.call(predicate, filter, occupancy, center, radius, random);
        }

        List<PoiRecord> shuffled = Util.toShuffledList(
                ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy), random);
        return shuffled.stream().filter(record -> filter.test(record.getPos())).findFirst().map(PoiRecord::getPos);
    }

    @WrapMethod(method = "getCountInRange")
    private long toroidal$countThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Long> original) {
        if (toroidal$transformer() == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy).count();
    }

    @ModifyArg(
            method = "findAllClosestFirstWithType",
            at = @At(value = "INVOKE", target = InjectionTargets.STREAM_SORTED),
            index = 0)
    private Comparator<Pair<Holder<PoiType>, BlockPos>> toroidal$sortThroughSeam(
            Comparator<Pair<Holder<PoiType>, BlockPos>> original, @Local(argsOnly = true) BlockPos center) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original;
        }

        return FoldedOrder.of(original, pair -> {
            BlockPos pos = pair.getSecond();
            BlockPos nearest = transformer.nearestCopy(center, pos);
            return nearest == pos ? pair : Pair.of(pair.getFirst(), nearest);
        });
    }

    @WrapMethod(method = "ensureLoadedAndValid")
    private void toroidal$loadThroughSeam(LevelReader reader, BlockPos center, int radius, Operation<Void> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            original.call(reader, center, radius);
            return;
        }

        SectionStorageAccessor storage = (SectionStorageAccessor) this;
        LevelHeightAccessor height = storage.toroidal$getLevelHeightAccessor();
        int chunkRadius = Math.floorDiv(radius, CoordinateConstants.CHUNK_WIDTH);
        toroidal$chunksAround(ChunkPos.containing(center), chunkRadius, transformer)
                .filter(chunk -> IntStream.rangeClosed(height.getMinSectionY(), height.getMaxSectionY())
                        .anyMatch(sectionY -> !storage.toroidal$getOrLoad(SectionPos.of(chunk, sectionY).asLong())
                                .map(section -> ((PoiSection) section).isValid())
                                .orElse(false)))
                .filter(chunk -> this.loadedChunks.add(chunk.pack()))
                .forEach(chunk -> reader.getChunk(chunk.x(), chunk.z(), ChunkStatus.EMPTY));
    }

    @WrapMethod(method = "sectionsToVillage")
    private int toroidal$villageDistanceThroughSeam(SectionPos sectionPos, Operation<Integer> original) {
        WorldFold transformer = toroidal$transformer();
        if (transformer == null) {
            return original.call(sectionPos);
        }

        return original.call(transformer.fold(sectionPos));
    }

    @ModifyVariable(
            method = {"add", "remove", "release", "exists", "getType", "getDebugPoiInfo"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private BlockPos toroidal$positionThroughSeam(BlockPos pos) {
        return toroidal$levelFold().fold(pos);
    }

    @Unique
    private static Stream<ChunkPos> toroidal$chunksAround(ChunkPos center, int chunkRadius, WorldFold transformer) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, chunkRadius)
                .map(pos -> transformer.isOver(pos) ? transformer.fold(pos) : pos);

        if (!toroidal$foldsOntoItself(chunkRadius, transformer)) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.pack()));
    }

    @Unique
    private static Comparator<BlockPos> toroidal$byDistance(WorldFold transformer, BlockPos center) {
        return Comparator.comparingDouble(pos -> SeamRange.sqr(transformer, center, pos));
    }

    @Unique
    private static boolean toroidal$foldsOntoItself(int chunkRadius, WorldFold transformer) {
        int span = chunkRadius * 2 + 1;
        return transformer.bounds().x().foldsOntoItself(span) || transformer.bounds().z().foldsOntoItself(span);
    }

    @Unique
    private @Nullable WorldFold toroidal$levelTransformer;

    @Unique
    private WorldFold toroidal$levelFold() {
        WorldFold transformer = this.toroidal$levelTransformer;
        if (transformer == null) {
            transformer = ((SectionStorageAccessor) this).toroidal$getLevelHeightAccessor() instanceof ServerLevel level
                    ? WorldLoopAttachments.transformerOf(level)
                    : WorldFolds.NOOP;
            this.toroidal$levelTransformer = transformer;
        }

        return transformer;
    }

    @Unique
    private @Nullable WorldFold toroidal$transformer() {
        WorldFold transformer = toroidal$levelFold();
        return transformer.isWrapped() ? transformer : null;
    }
}
