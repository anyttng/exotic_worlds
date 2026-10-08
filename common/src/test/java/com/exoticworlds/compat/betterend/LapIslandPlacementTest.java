package com.exoticworlds.compat.betterend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.world.generator.LayerOptions;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.WrapDomain;

import net.minecraft.core.BlockPos;

class LapIslandPlacementTest {
    private static final int SEED = 926;

    private static final int LAP = 4096;

    private static final int MAX_HEIGHT = 128;

    private static final LayerOptions MEDIUM = new LayerOptions(150.0F, 100.0F, 0.546875F, 0.15625F, true);

    private static final LapIslandPlacement.Grid GRID = new LapIslandPlacement.Grid(
            IslandLapAxis.of(new WrapDomain(-LAP / 2, LAP / 2), MEDIUM.distance),
            IslandLapAxis.of(new WrapDomain(-LAP / 2, LAP / 2), MEDIUM.distance));

    private static final LapIslandPlacement.Centre NO_CENTRE = new LapIslandPlacement.Centre(false, 0, 0L);

    private static final LapIslandPlacement.Centre CENTRE = new LapIslandPlacement.Centre(true, 256, 1024L * 1024L);

    private static final IntBinaryOperator SEED_OF = (x, z) -> {
        int h = SEED + x * 374761393 + z * 668265263;
        h = (h ^ h >> 13) * 1274126177;
        return h ^ h >> 16;
    };

    private static final OpenSimplexNoise COVERAGE = new OpenSimplexNoise(SEED);

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

    private static List<LapIslandPlacement.Island> place(int cellX, int cellZ, LapIslandPlacement.Centre centre) {
        List<LapIslandPlacement.Island> islands = new ArrayList<>();
        LapIslandPlacement.place(islands, GRID, cellX, cellZ, MAX_HEIGHT, MEDIUM, SEED_OF, COVERAGE, centre);
        return islands;
    }
}
