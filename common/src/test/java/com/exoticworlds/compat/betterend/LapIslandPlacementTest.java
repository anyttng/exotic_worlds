package com.exoticworlds.compat.betterend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.LayerOptions;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.BlockPos;

class LapIslandPlacementTest {
    private static final int SEED = 926;

    private static final int LAP = 4096;

    private static final int MAX_HEIGHT = 128;

    private static final LayerOptions MEDIUM = new LayerOptions(150.0F, 100.0F, 0.546875F, 0.15625F, true);

    private static final WrapDomain LAP_DOMAIN = new WrapDomain(-LAP / 2, LAP / 2);

    private static final LapIslandPlacement.Grid GRID = new LapIslandPlacement.Grid(
            IslandLapAxis.of(LAP_DOMAIN, MEDIUM.distance), IslandLapAxis.of(LAP_DOMAIN, MEDIUM.distance),
            new LapFrame(TranslationLattice.unskewed(LAP_DOMAIN, LAP_DOMAIN)));

    private static final int LAP_CHUNKS = LAP / 16;

    private static final int SKEW_CHUNKS = 96;

    private static final TranslationLattice SKEWED_LATTICE = WorldFolds.of(FlatShape.latticeTorus(
            new WorldLoopBounds(-LAP_CHUNKS / 2, LAP_CHUNKS / 2, -LAP_CHUNKS / 2, LAP_CHUNKS / 2), SKEW_CHUNKS))
            .blockLattice();

    private static final LapIslandPlacement.Grid SKEWED = new LapIslandPlacement.Grid(
            IslandLapAxis.of(SKEWED_LATTICE.x(), MEDIUM.distance), IslandLapAxis.of(SKEWED_LATTICE.z(), MEDIUM.distance),
            new LapFrame(SKEWED_LATTICE));

    private static final int COPY_REACH = 3;

    private static final int WINDOW_SAMPLES = 4;

    private static final int CENTRAL_Y = 64;

    private static final BlockPos CENTRAL = new BlockPos(0, CENTRAL_Y, 0);

    private static final LapIslandPlacement.Centre NO_CENTRE = new LapIslandPlacement.Centre(false, 0, 0L);

    private static final LapIslandPlacement.Centre CENTRE = new LapIslandPlacement.Centre(true, 256, 1024L * 1024L);

    private static final OpenSimplexNoise COVERAGE = new OpenSimplexNoise(SEED);

    private static int seedOf(int x, int z) {
        int h = SEED + x * 374761393 + z * 668265263;
        h = (h ^ h >> 13) * 1274126177;
        return h ^ h >> 16;
    }

    @Test
    void aWindowOneLapAwayPlacesTheSameIslandsOneLapShifted() {
        int cells = GRID.x().cells();
        int placed = 0;
        for (LapIslandPlacement.Centre centre : new LapIslandPlacement.Centre[] {NO_CENTRE, CENTRE}) {
            for (int cellX = -2; cellX < cells + 2; cellX++) {
                for (int cellZ = -2; cellZ < cells + 2; cellZ += 3) {
                    List<LapIslandPlacement.Island> here = place(cellX, cellZ, centre);
                    List<LapIslandPlacement.Island> away = place(cellX + cells, cellZ - cells, centre);
                    String name = "cell " + cellX + ", " + cellZ + " with " + centre;
                    assertEquals(here.size(), away.size(), name);
                    for (int i = 0; i < here.size(); i++) {
                        assertEquals(here.get(i).canonical(), away.get(i).canonical(), name);
                        assertEquals(here.get(i).seated().offset(LAP, 0, -LAP), away.get(i).seated(), name);
                    }

                    placed += here.size();
                }
            }
        }

        assertTrue(placed > 0, "no window placed an island — the test measures nothing");
    }

    @Test
    void theWindowAcrossTheSeamSeatsTheFirstCellsIslandsOneLapOn() {
        int last = GRID.x().cells() - 1;
        int seatedPastTheSeam = 0;
        for (int cellZ = 0; cellZ < GRID.z().cells(); cellZ++) {
            for (LapIslandPlacement.Island island : place(last, cellZ, NO_CENTRE)) {
                BlockPos canonical = island.canonical();
                int canonicalCell = GRID.x().cell(canonical.getX());
                int expected = canonicalCell == 0 ? canonical.getX() + LAP : canonical.getX();
                assertEquals(expected, island.seated().getX(), "island " + canonical + " in cell " + canonicalCell);
                seatedPastTheSeam += canonicalCell == 0 ? 1 : 0;
            }
        }

        assertTrue(seatedPastTheSeam > 0, "no island of the first cell met the last window — the test measures nothing");
    }

    @Test
    void theCentralIslandSitsOnTheOriginCopyNearestTheWindow() {
        int last = GRID.x().cells() - 1;
        List<LapIslandPlacement.Island> nearOrigin = place(0, 0, CENTRE);
        List<LapIslandPlacement.Island> pastTheSeam = place(last + GRID.x().cells(), 0, CENTRE);
        assertTrue(nearOrigin.stream().anyMatch(island -> island.seated().equals(new BlockPos(0, 64, 0))),
                "the central island is missing beside the origin: " + nearOrigin);
        assertTrue(pastTheSeam.stream().noneMatch(island -> island.seated().getX() == 0),
                "the central island stays at the origin a lap away: " + pastTheSeam);
    }

    @Test
    void onASkewedGridAWindowOneLatticeVectorAwayPlacesTheSameIslandsShifted() {
        int cellsX = SKEWED.x().cells();
        int cellsZ = SKEWED.z().cells();
        int placed = 0;
        for (LapIslandPlacement.Centre centre : new LapIslandPlacement.Centre[] {NO_CENTRE, CENTRE}) {
            for (int cellX = -2; cellX < cellsX + 2; cellX++) {
                for (int cellZ = -2; cellZ < cellsZ + 2; cellZ += 3) {
                    List<LapIslandPlacement.Island> here = place(SKEWED, cellX, cellZ, centre);
                    String name = "cell " + cellX + ", " + cellZ + " with " + centre;
                    assertShifted(here, place(SKEWED, cellX + cellsX, cellZ, centre),
                            SKEWED_LATTICE.x().domainLength, 0, name + " one X lap on");
                    assertShifted(here, place(SKEWED, cellX, cellZ + cellsZ, centre), SKEWED_LATTICE.skew(),
                            SKEWED_LATTICE.z().domainLength, name + " one Z lap on");
                    placed += here.size();
                }
            }
        }

        assertTrue(placed > 0, "no window placed an island — the test measures nothing");
    }

    @Test
    void onASkewedGridTheCentralIslandSitsOnTheOriginCopyNearestTheWindowInBlocks() {
        for (int cellX = -2; cellX < SKEWED.x().cells() + 2; cellX++) {
            for (int cellZ = -2; cellZ < SKEWED.z().cells() + 2; cellZ++) {
                double middleZ = (cellZ + 0.5) * SKEWED.z().cellBlocks();
                double middleX = SKEWED.frame().blockX((cellX + 0.5) * SKEWED.x().cellBlocks(), middleZ);
                BlockPos expected = nearestOriginCopy(middleX, middleZ).atY(CENTRAL_Y);
                List<LapIslandPlacement.Island> islands = place(SKEWED, cellX, cellZ, CENTRE);
                assertTrue(islands.stream().anyMatch(island -> island.canonical().equals(CENTRAL)
                        && island.seated().equals(expected)),
                        "cell " + cellX + ", " + cellZ + " misses the central island at " + expected + ": " + islands);
            }
        }
    }

    @Test
    void onASkewedGridTheInnerVoidIsRoundInBlocks() {
        int removed = 0;
        for (int cellX = -2; cellX < SKEWED.x().cells() + 2; cellX++) {
            for (int cellZ = -2; cellZ < SKEWED.z().cells() + 2; cellZ += 2) {
                List<LapIslandPlacement.Island> cleared = place(SKEWED, cellX, cellZ, CENTRE);
                for (LapIslandPlacement.Island island : place(SKEWED, cellX, cellZ, NO_CENTRE)) {
                    BlockPos canonical = island.canonical();
                    BlockPos copy = nearestOriginCopy(canonical.getX(), canonical.getZ());
                    long x = (long) canonical.getX() - copy.getX();
                    long z = (long) canonical.getZ() - copy.getZ();
                    boolean inVoid = x * x + z * z < CENTRE.innerVoidSquared();
                    assertEquals(!inVoid, cleared.contains(island),
                            "island " + canonical + " " + Math.sqrt((double) (x * x + z * z)) + " blocks from the origin copy "
                                    + copy + " in cell " + cellX + ", " + cellZ);
                    removed += inVoid ? 1 : 0;
                }
            }
        }

        assertTrue(removed > 0, "no island fell inside the void — the test measures nothing");
    }

    @Test
    void aSkewedWindowSeesEveryIslandWithinOneCellOfItsPoints() {
        int cellsX = SKEWED.x().cells();
        int cellsZ = SKEWED.z().cells();
        Set<BlockPos> region = new HashSet<>();
        for (int cellX = -COPY_REACH; cellX < cellsX + COPY_REACH; cellX++) {
            for (int cellZ = -COPY_REACH; cellZ < cellsZ + COPY_REACH; cellZ++) {
                place(SKEWED, cellX, cellZ, NO_CENTRE).forEach(island -> region.add(island.seated()));
            }
        }

        double reach = Math.min(SKEWED.x().cellBlocks(), SKEWED.z().cellBlocks());
        int seen = 0;
        for (int cellX = 0; cellX < cellsX; cellX += 3) {
            for (int cellZ = 0; cellZ < cellsZ; cellZ += 3) {
                Set<BlockPos> window = new HashSet<>();
                place(SKEWED, cellX, cellZ, NO_CENTRE).forEach(island -> window.add(island.seated()));
                for (int i = 0; i < WINDOW_SAMPLES; i++) {
                    for (int k = 0; k < WINDOW_SAMPLES; k++) {
                        double pointZ = (cellZ + (k + 0.5) / WINDOW_SAMPLES) * SKEWED.z().cellBlocks();
                        double pointX = SKEWED.frame().blockX(
                                (cellX + (i + 0.5) / WINDOW_SAMPLES) * SKEWED.x().cellBlocks(), pointZ);
                        for (BlockPos island : region) {
                            if (Math.hypot(island.getX() - pointX, island.getZ() - pointZ) <= reach) {
                                assertTrue(window.contains(island), "cell " + cellX + ", " + cellZ + " misses "
                                        + island + " near " + pointX + ", " + pointZ);
                                seen++;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(seen > 0, "no island lay within a cell of a window's points — the test measures nothing");
    }

    private static void assertShifted(List<LapIslandPlacement.Island> here, List<LapIslandPlacement.Island> away,
            int shiftX, int shiftZ, String name) {
        assertEquals(here.size(), away.size(), name);
        for (int i = 0; i < here.size(); i++) {
            assertEquals(here.get(i).canonical(), away.get(i).canonical(), name);
            assertEquals(here.get(i).seated().offset(shiftX, 0, shiftZ), away.get(i).seated(), name);
        }
    }

    private static BlockPos nearestOriginCopy(double x, double z) {
        BlockPos nearest = BlockPos.ZERO;
        double best = Double.MAX_VALUE;
        for (int lapsX = -COPY_REACH; lapsX <= COPY_REACH; lapsX++) {
            for (int lapsZ = -COPY_REACH; lapsZ <= COPY_REACH; lapsZ++) {
                int copyX = lapsX * SKEWED_LATTICE.x().domainLength + lapsZ * SKEWED_LATTICE.skew();
                int copyZ = lapsZ * SKEWED_LATTICE.z().domainLength;
                double squared = (x - copyX) * (x - copyX) + (z - copyZ) * (z - copyZ);
                if (squared < best) {
                    best = squared;
                    nearest = new BlockPos(copyX, 0, copyZ);
                }
            }
        }

        return nearest;
    }

    private static List<LapIslandPlacement.Island> place(int cellX, int cellZ, LapIslandPlacement.Centre centre) {
        return place(GRID, cellX, cellZ, centre);
    }

    private static List<LapIslandPlacement.Island> place(LapIslandPlacement.Grid grid, int cellX, int cellZ,
            LapIslandPlacement.Centre centre) {
        List<LapIslandPlacement.Island> islands = new ArrayList<>();
        LapIslandPlacement.place(islands, grid, cellX, cellZ, MAX_HEIGHT, MEDIUM, LapIslandPlacementTest::seedOf,
                COVERAGE, centre);
        return islands;
    }
}
