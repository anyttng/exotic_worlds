package com.exoticworlds.compat.wover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import net.minecraft.core.Direction;

class SquareLapMapTest {
    private static final int DEFAULT_SIZE = 256;

    private static final long SEED = 0x904L;

    private static final int PALETTE = 16;

    private static final LapPicker<Integer> PICKER = new LapPicker<>(
            random -> random.nextInt(PALETTE), (biome, random) -> biome);

    private static SquareLapMap<Integer> map(WorldFold fold, int size, double factor) {
        return new SquareLapMap<>(fold, size, factor, SEED, PICKER);
    }

    private static SquareLapMap<Integer> map(WorldFold fold, int size) {
        return map(fold, size, ClimateScaleCompression.NO_COMPRESSION);
    }

    @Test
    void aTorusHeldInOneBlockReadsTheSameBiomeOneLapAwayOnBothAxes() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-64, 64, -64, 64)));
        HexLapMapTest.assertPeriodic(map(fold, DEFAULT_SIZE), fold);
    }

    @Test
    void aTorusOfSeveralBlocksReadsTheSameBiomeOneLapAwayOnBothAxes() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-256, 256, -256, 256)));
        HexLapMapTest.assertPeriodic(map(fold, DEFAULT_SIZE), fold);
    }

    @Test
    void aTorusWhoseLapSplitsIntoUnevenBlocksStillRepeats() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-150, 150, -82, 82)));
        HexLapMapTest.assertPeriodic(map(fold, 64), fold);
    }

    @Test
    void aCylinderRepeatsOnItsLoopedAxisAndKeepsTheOtherOpen() {
        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 256)));
        SquareLapMap<Integer> map = map(cylinder, DEFAULT_SIZE);
        int lap = cylinder.blockDomain(Direction.Axis.X).domainLength;
        Set<Integer> seen = new HashSet<>();
        for (int z = -16384; z < 16384; z += 97) {
            for (int x = -2 * lap; x < 2 * lap; x += 101) {
                int biome = map.biomeAt(x, z);
                seen.add(biome);
                assertEquals(biome, map.biomeAt(x + lap, z), "x " + x + " z " + z);
            }
        }

        assertFalse(map.axis(Direction.Axis.Z).loops(), "the open axis was given a lap");
        assertTrue(seen.size() > 1, "the map laid one biome over the whole cylinder: " + seen);
    }

    @Test
    void aLatticeKeepsWorldWeaversCellOrShrinksItOnlyToFitAWholeCount() {
        int[] chunkWidths = {1, 3, 16, 41, 75, 128, 256, 1875};
        int[] sizes = {DEFAULT_SIZE, 64, 1024};
        for (int width : chunkWidths) {
            WorldFold fold = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(width)));
            for (int size : sizes) {
                SquareLapMap<Integer> map = map(fold, size);
                HexLapMapTest.assertCell(map.axis(Direction.Axis.X), size, 1);
                HexLapMapTest.assertCell(map.axis(Direction.Axis.Z), size, 1);
            }
        }
    }

    @Test
    void theCompactBiomesFactorShrinksTheCellOnBothAxes() {
        double[] factors = {2.0, 4.0, 1.56};
        WorldFold torus = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(256)));
        for (double factor : factors) {
            SquareLapMap<Integer> map = map(torus, DEFAULT_SIZE, factor);
            HexLapMapTest.assertCell(map.axis(Direction.Axis.X), DEFAULT_SIZE / factor, 1);
            HexLapMapTest.assertCell(map.axis(Direction.Axis.Z), DEFAULT_SIZE / factor, 1);
        }

        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 256)));
        SquareLapMap<Integer> compact = map(cylinder, DEFAULT_SIZE, 4.0);
        assertEquals(DEFAULT_SIZE / 4.0, compact.axis(Direction.Axis.Z).cellBlocks(), 1.0E-9,
                "the cylinder's open axis kept WorldWeaver's cell");
    }

    @Test
    void theSameFoldAndSeedLayTheSameMap() {
        WorldFold fold = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-80, 80, -80, 80)));
        SquareLapMap<Integer> first = map(fold, DEFAULT_SIZE);
        SquareLapMap<Integer> second = map(fold, DEFAULT_SIZE);
        for (int x = -1280; x < 1280; x += 23) {
            for (int z = -1280; z < 1280; z += 29) {
                assertEquals(first.biomeAt(x, z), second.biomeAt(x, z), "x " + x + " z " + z);
            }
        }
    }
}
