package com.exoticworlds.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldLoopSizesTest {
    private static final int BORDER_CHUNKS = 1_874_999;

    @Test
    void aLoopingAxisTakesASpawnPointInsideTheWorld() {
        assertEquals(-64, WorldLoopSizes.minSpawnChunk(128));
        assertEquals(63, WorldLoopSizes.maxSpawnChunk(128));
    }

    @Test
    void anOddLapTakesTheExtraChunkOnItsUpperSide() {
        assertEquals(-32, WorldLoopSizes.minSpawnChunk(65));
        assertEquals(32, WorldLoopSizes.maxSpawnChunk(65));
    }

    @Test
    void theSpawnPointsOfALoopingAxisNameEachChunkOfTheWorldOnce() {
        for (int widthChunks : new int[] {16, 17, 64, 65, 4096}) {
            assertEquals(widthChunks,
                    WorldLoopSizes.maxSpawnChunk(widthChunks) - WorldLoopSizes.minSpawnChunk(widthChunks) + 1,
                    "spawn points of " + widthChunks);
        }
    }

    @Test
    void anEndlessAxisTakesASpawnPointAnywhereInsideTheWorldBorder() {
        assertEquals(-BORDER_CHUNKS, WorldLoopSizes.minSpawnChunk(WorldLoopSizes.ENDLESS_AXIS_WIDTH));
        assertEquals(BORDER_CHUNKS, WorldLoopSizes.maxSpawnChunk(WorldLoopSizes.ENDLESS_AXIS_WIDTH));
    }
}
