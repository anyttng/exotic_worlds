package com.exoticworlds.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.world.level.ChunkPos;

class JourneyMapFoldGridRegionsTest {
    private static final ToroidalShape ON_GRID = MapShapes.torus(-64, 64);
    private static final ToroidalShape OFF_GRID = MapShapes.torus(-937, 938);

    @Test
    void anUnshapedWorldKeepsTheGridsOwnRegions() {
        assertEquals(row(0, -2, -1, 0, 1, 2, 3), regions(JourneyMapFold.gridRegions(null, -2, 0, 3, 0)));
    }

    @Test
    void aGridInsideTheWorldKeepsItsOwnRegions() {
        assertEquals(row(0, 0, 1, 2), regions(JourneyMapFold.gridRegions(OFF_GRID, 0, 0, 2, 0)));
    }

    @Test
    void aGridPastTheSeamOnTheRegionGridFoldsOntoTheFarEdge() {
        assertEquals(row(0, -2, 1), regions(JourneyMapFold.gridRegions(ON_GRID, 1, 0, 2, 0)),
                "raw region 2 is chunks 64..95, which fold to -64..-33, region -2");
    }

    @Test
    void aGridPastTheSeamOffTheRegionGridTakesEveryRegionItsChunksFoldInto() {
        assertEquals(row(0, -30, -29, -28, 29), regions(JourneyMapFold.gridRegions(OFF_GRID, 29, 0, 30, 0)),
                "chunks 938..991 lie past the +X edge at 938 and fold to -937..-884, regions -30..-28");
    }

    @Test
    void aGridWhollyPastTheMinEdgeShiftsByOneWidth() {
        assertEquals(row(0, 26, 27, 28), regions(JourneyMapFold.gridRegions(OFF_GRID, -32, 0, -31, 0)),
                "chunks -1024..-961 fold to 851..914");
    }

    @Test
    void aGridWiderThanTheWorldTakesEveryRegionOnce() {
        assertEquals(row(0, -2, -1, 0, 1), regions(JourneyMapFold.gridRegions(ON_GRID, -3, 0, 2, 0)));
    }

    @Test
    void aGridPastALatticeSeamFoldsOntoTheRegionTheSkewMovesItTo() {
        assertEquals(Set.of(ChunkPos.pack(-1, -2)),
                regions(JourneyMapFold.gridRegions(MapShapes.latticeTorus(-64, 64, 32), 0, 2, 0, 2)),
                "raw region (0, 2) lies one Z lap up and 512 blocks over, so it folds onto (-1, -2), not (0, -2)");
    }

    @Test
    void anUnboundedAxisShowsOnlyTheRegionsInsideTheBounds() {
        assertTrue(JourneyMapFold.regionInView(null, 3, 0, 0.0, 0.0, 4.0, 1.0));
        assertFalse(JourneyMapFold.regionInView(null, 4, 0, 0.0, 0.0, 4.0, 1.0), "the bounds' max is exclusive");
    }

    @Test
    void aTileWhoseCopyMeetsTheBoundsIsShown() {
        assertTrue(JourneyMapFold.regionInView(OFF_GRID, -30, 0, 25.0, -1.0, 36.0, 1.0),
                "region -30 is blocks -15360..-14849, its copy one world over 14640..15151 lies inside 12800..18431");
        assertTrue(JourneyMapFold.regionInView(OFF_GRID, 26, 0, 25.0, -1.0, 36.0, 1.0));
    }

    @Test
    void aTileNoCopyOfWhichMeetsTheBoundsIsHidden() {
        assertFalse(JourneyMapFold.regionInView(OFF_GRID, 0, 0, 25.0, -1.0, 36.0, 1.0),
                "region 0's copies sit at 0 and at 30000 blocks, both outside 12800..18431");
    }

    @Test
    void aLatticeTileIsShownWhereTheSkewedRowPutsIt() {
        ToroidalShape lattice = MapShapes.latticeTorus(-64, 64, 32);
        assertTrue(JourneyMapFold.regionInView(lattice, -1, -2, 0.0, 2.0, 1.0, 3.0),
                "region (-1, -2) one Z lap up and 512 blocks over sits on raw region (0, 2)");
        assertFalse(JourneyMapFold.regionInView(lattice, 0, -2, 0.0, 2.0, 1.0, 3.0),
                "region (0, -2) was shown where only an unskewed copy would put it");
    }

    private static Set<Long> regions(long[] packed) {
        Set<Long> regions = new HashSet<>();
        for (long region : packed) {
            regions.add(region);
        }

        return regions;
    }

    private static Set<Long> row(int z, int... xs) {
        Set<Long> regions = new HashSet<>();
        for (int x : xs) {
            regions.add(ChunkPos.pack(x, z));
        }

        return regions;
    }
}
