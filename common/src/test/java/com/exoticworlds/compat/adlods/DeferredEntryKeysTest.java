package com.exoticworlds.compat.adlods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

class DeferredEntryKeysTest {
    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(-16, 16);
    private static final int SKEW_CHUNKS = 3;
    private static final BlockPos ORIGIN = new BlockPos(-250, 12, 40);
    private static final BlockPos PAST_MIN_X = new BlockPos(-18 * 16 + 5, 12, 40);
    private static final BlockPos PAST_MAX_Z = new BlockPos(30, -20, 17 * 16 + 2);

    @Test
    void aTorusFilesAPointPastTheSeamUnderTheChunkThatContinuesIt() {
        Map<ChunkPos, Map<BlockPos, BlockPos>> keyed = keyedByRawChunk(PAST_MIN_X, PAST_MAX_Z);
        DeferredEntryKeys.fold(keyed, WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(LOOPED, LOOPED))));

        assertEquals(Set.of(new ChunkPos(14, 2), new ChunkPos(1, -15)), keyed.keySet());
        assertEquals(Map.of(PAST_MIN_X, ORIGIN), keyed.get(new ChunkPos(14, 2)), "the point stays raw");
        assertEquals(Map.of(PAST_MAX_Z, ORIGIN), keyed.get(new ChunkPos(1, -15)), "the point stays raw");
    }

    @Test
    void aSkewedLatticeTorusFilesThePointInsideTheBounds() {
        WorldFold fold = WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(LOOPED, LOOPED), SKEW_CHUNKS));
        Map<ChunkPos, Map<BlockPos, BlockPos>> keyed = keyedByRawChunk(PAST_MIN_X);
        DeferredEntryKeys.fold(keyed, fold);

        ChunkPos key = keyed.keySet().iterator().next();
        assertFalse(fold.isOver(key), () -> "key " + key);
        assertEquals(fold.fold(ChunkPos.containing(PAST_MIN_X)), key);
        assertEquals(Map.of(PAST_MIN_X, ORIGIN), keyed.get(key), "the point stays raw");
    }

    @Test
    void aCylinderFoldsOnlyItsLoopingAxis() {
        BlockPos farAlongZ = new BlockPos(30, 12, 90 * 16);
        Map<ChunkPos, Map<BlockPos, BlockPos>> keyed = keyedByRawChunk(PAST_MIN_X, farAlongZ);
        DeferredEntryKeys.fold(keyed, WorldFolds.of(ShapeFixtures.cylinder(
                new WorldLoopBounds(LOOPED, AxisBounds.Unbounded.INSTANCE))));

        assertEquals(Set.of(new ChunkPos(14, 2), new ChunkPos(1, 90)), keyed.keySet());
    }

    @Test
    void twoRawKeysOfOneChunkMeetUnderItsKey() {
        BlockPos inside = new BlockPos(14 * 16 + 1, 12, 40);
        BlockPos otherOrigin = new BlockPos(200, 12, 40);
        Map<ChunkPos, Map<BlockPos, BlockPos>> keyed = keyedByRawChunk(PAST_MIN_X);
        keyed.computeIfAbsent(ChunkPos.containing(inside), chunk -> new ConcurrentHashMap<>()).put(inside,
                otherOrigin);
        DeferredEntryKeys.fold(keyed, WorldFolds.of(ShapeFixtures.torus(new WorldLoopBounds(LOOPED, LOOPED))));

        assertEquals(Map.of(new ChunkPos(14, 2), Map.of(PAST_MIN_X, ORIGIN, inside, otherOrigin)), keyed);
    }

    @Test
    void anUnwrappedLevelKeepsTheKeysAsTheModWritesThem() {
        Map<ChunkPos, Map<BlockPos, BlockPos>> keyed = keyedByRawChunk(PAST_MIN_X, PAST_MAX_Z);
        Map<ChunkPos, Map<BlockPos, BlockPos>> before = Map.copyOf(keyed);
        DeferredEntryKeys.fold(keyed, WorldFolds.NOOP);

        assertEquals(before, keyed);
    }

    private static Map<ChunkPos, Map<BlockPos, BlockPos>> keyedByRawChunk(BlockPos... points) {
        Map<ChunkPos, Map<BlockPos, BlockPos>> keyed = new ConcurrentHashMap<>();
        for (BlockPos point : points) {
            keyed.computeIfAbsent(ChunkPos.containing(point), chunk -> new ConcurrentHashMap<>()).put(point, ORIGIN);
        }

        return keyed;
    }
}
