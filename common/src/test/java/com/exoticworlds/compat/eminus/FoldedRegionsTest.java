package com.exoticworlds.compat.eminus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

class FoldedRegionsTest {
    private static final int TILE_CHUNKS = 16;
    private static final int TILE_ROW_Z = 0;

    @ParameterizedTest(name = "{0} chunks")
    @ValueSource(ints = {1875, 1899})
    void aTileCutByTheLapEdgeIsPlannedForTheRegionPastTheEdge(int widthChunks) {
        WorldFold worldFold = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidths(widthChunks, widthChunks)));
        CellFold fold = CellFold.of(worldFold);
        int minChunkX = worldFold.shapeView().minChunk(Direction.Axis.X);
        int pastLap = minChunkX + widthChunks;
        int tileX = Math.floorDiv(pastLap - 1, TILE_CHUNKS) * TILE_CHUNKS;
        assertTrue(pastLap - tileX > 0 && pastLap - tileX < TILE_CHUNKS, "the tile must straddle the lap edge");

        long pastEdge = region(minChunkX, TILE_ROW_Z);
        assertNotEquals(pastEdge, region(fold.foldChunkX(tileX, TILE_ROW_Z), fold.foldChunkZ(TILE_ROW_Z)),
                "the tile's first chunk stores in another region");
        assertTrue(FoldedRegions.of(LongSet.of(pastEdge), fold, TILE_CHUNKS).contains(region(tileX, TILE_ROW_Z)),
                "a region holding only the piece past the lap edge plans the tile");
        assertFalse(FoldedRegions.of(LongSet.of(region(0, TILE_ROW_Z)), fold, TILE_CHUNKS)
                .contains(region(tileX, TILE_ROW_Z)), "a region the tile never reaches plans nothing");
    }

    @ParameterizedTest(name = "{0} chunks")
    @ValueSource(ints = {1899})
    void noFirstChunkOfTheRegionReachesTheEdgeRegionWhenTheCutTileIsItsLast(int widthChunks) {
        WorldFold worldFold = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidths(widthChunks, widthChunks)));
        CellFold fold = CellFold.of(worldFold);
        int minChunkX = worldFold.shapeView().minChunk(Direction.Axis.X);
        int tileX = Math.floorDiv(minChunkX + widthChunks - 1, TILE_CHUNKS) * TILE_CHUNKS;
        int regionFirstX = Math.floorDiv(tileX, ChunkPos.REGION_SIZE) * ChunkPos.REGION_SIZE;
        assertTrue(tileX - regionFirstX == ChunkPos.REGION_SIZE - TILE_CHUNKS, "the cut tile must close its region");

        long pastEdge = region(minChunkX, TILE_ROW_Z);
        for (int firstX = regionFirstX; firstX < regionFirstX + ChunkPos.REGION_SIZE; firstX += TILE_CHUNKS) {
            assertNotEquals(pastEdge, region(fold.foldChunkX(firstX, TILE_ROW_Z), fold.foldChunkZ(TILE_ROW_Z)),
                    "a first chunk alone would already reach the region past the edge");
        }
    }

    private static long region(int chunkX, int chunkZ) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        return ChunkPos.asLong(chunk.getRegionX(), chunk.getRegionZ());
    }
}
