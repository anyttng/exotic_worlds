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
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedOrder;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
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
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        PoiManager self = (PoiManager) (Object) this;
        int chunkRadius = Math.floorDiv(radius, CoordinateConstants.CHUNK_WIDTH) + 1;

        return exotic_worlds$chunksAround(new ChunkPos(center), chunkRadius, transformer)
                .flatMap(chunkPos -> self.getInChunk(predicate, chunkPos, occupancy))
                .filter(record -> {
                    BlockPos pos = record.getPos();
                    BlockPos nearest = transformer.nearestCopy(center, pos);
                    return Math.abs(nearest.getX() - center.getX()) <= radius
                            && Math.abs(nearest.getZ() - center.getZ()) <= radius;
                });
    }

    @WrapMethod(method = "getInRange")
    private Stream<PoiRecord> exotic_worlds$rangeThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Stream<PoiRecord>> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        double radiusSqr = (double) radius * radius;
        return ((PoiManager) (Object) this).getInSquare(predicate, center, radius, occupancy)
                .filter(record -> SeamRange.sqr(transformer, center, record.getPos()) <= radiusSqr);
    }

    @WrapMethod(method = FIND_CLOSEST)
    private Optional<BlockPos> exotic_worlds$closestThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy)
                .map(PoiRecord::getPos)
                .min(exotic_worlds$byDistance(transformer, center));
    }

    @WrapMethod(method = FIND_CLOSEST_FILTERED)
    private Optional<BlockPos> exotic_worlds$closestFilteredThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return original.call(predicate, filter, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy)
                .map(PoiRecord::getPos)
                .filter(filter)
                .min(exotic_worlds$byDistance(transformer, center));
    }

    @WrapMethod(method = "findClosestWithType")
    private Optional<Pair<Holder<PoiType>, BlockPos>> exotic_worlds$closestWithTypeThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<Pair<Holder<PoiType>, BlockPos>>> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy)
                .min(Comparator.comparingDouble(record -> SeamRange.sqr(transformer, center, record.getPos())))
                .map(record -> Pair.of(record.getPoiType(), record.getPos()));
    }

    @WrapMethod(method = "find")
    private Optional<BlockPos> exotic_worlds$findThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Optional<BlockPos>> original) {
        if (this.exotic_worlds$bound.wrapped() == null) {
            return original.call(predicate, filter, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).findAll(predicate, filter, center, radius, occupancy).findFirst();
    }

    @WrapMethod(method = "getRandom")
    private Optional<BlockPos> exotic_worlds$randomThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            PoiManager.Occupancy occupancy,
            BlockPos center,
            int radius,
            RandomSource random,
            Operation<Optional<BlockPos>> original) {
        if (this.exotic_worlds$bound.wrapped() == null) {
            return original.call(predicate, filter, occupancy, center, radius, random);
        }

        List<PoiRecord> shuffled = Util.toShuffledList(
                ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy), random);
        return shuffled.stream().filter(record -> filter.test(record.getPos())).findFirst().map(PoiRecord::getPos);
    }

    @WrapMethod(method = "getCountInRange")
    private long exotic_worlds$countThroughSeam(
            Predicate<Holder<PoiType>> predicate,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            Operation<Long> original) {
        if (this.exotic_worlds$bound.wrapped() == null) {
            return original.call(predicate, center, radius, occupancy);
        }

        return ((PoiManager) (Object) this).getInRange(predicate, center, radius, occupancy).count();
    }

    @ModifyArg(
            method = "findAllClosestFirstWithType",
            at = @At(value = "INVOKE", target = InjectionTargets.STREAM_SORTED),
            index = 0)
    private Comparator<Pair<Holder<PoiType>, BlockPos>> exotic_worlds$sortThroughSeam(
            Comparator<Pair<Holder<PoiType>, BlockPos>> original, @Local(argsOnly = true) BlockPos center) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
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
    private void exotic_worlds$loadThroughSeam(LevelReader reader, BlockPos center, int radius, Operation<Void> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            original.call(reader, center, radius);
            return;
        }

        SectionStorageAccessor storage = (SectionStorageAccessor) this;
        LevelHeightAccessor height = storage.exotic_worlds$getLevelHeightAccessor();
        int chunkRadius = Math.floorDiv(radius, CoordinateConstants.CHUNK_WIDTH);
        exotic_worlds$chunksAround(new ChunkPos(center), chunkRadius, transformer)
                .filter(chunk -> IntStream.range(height.getMinSection(), height.getMaxSection())
                        .anyMatch(sectionY -> !storage.exotic_worlds$getOrLoad(SectionPos.of(chunk, sectionY).asLong())
                                .map(section -> ((PoiSection) section).isValid())
                                .orElse(false)))
                .filter(chunk -> this.loadedChunks.add(chunk.toLong()))
                .forEach(chunk -> reader.getChunk(chunk.x, chunk.z, ChunkStatus.EMPTY));
    }

    @WrapMethod(method = "sectionsToVillage")
    private int exotic_worlds$villageDistanceThroughSeam(SectionPos sectionPos, Operation<Integer> original) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return original.call(sectionPos);
        }

        return original.call(transformer.fold(sectionPos));
    }

    @ModifyVariable(
            method = {"add", "remove", "release", "exists", "getType"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private BlockPos exotic_worlds$positionThroughSeam(BlockPos pos) {
        return this.exotic_worlds$bound.fold().fold(pos);
    }

    @Unique
    private static Stream<ChunkPos> exotic_worlds$chunksAround(ChunkPos center, int chunkRadius, WorldFold transformer) {
        Stream<ChunkPos> wrapped = ChunkPos.rangeClosed(center, chunkRadius)
                .map(pos -> transformer.isOver(pos) ? transformer.fold(pos) : pos);

        if (!exotic_worlds$foldsOntoItself(chunkRadius, transformer)) {
            return wrapped;
        }

        LongSet seen = new LongOpenHashSet();
        return wrapped.filter(chunkPos -> seen.add(chunkPos.toLong()));
    }

    @Unique
    private static Comparator<BlockPos> exotic_worlds$byDistance(WorldFold transformer, BlockPos center) {
        return Comparator.comparingDouble(pos -> SeamRange.sqr(transformer, center, pos));
    }

    @Unique
    private static boolean exotic_worlds$foldsOntoItself(int chunkRadius, WorldFold transformer) {
        int span = chunkRadius * 2 + 1;
        return transformer.bounds().x().foldsOntoItself(span) || transformer.bounds().z().foldsOntoItself(span);
    }

}
