package com.exoticworlds.compat.terraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class RiverReachTest {
    private static final int NARROW_CHUNKS = 256;

    private static final int WIDE_CHUNKS = 5000;

    private static final int CONTINENT_SCALE = 3000;

    private static final int PRESET_ROOTS = 8;

    private static final float SHALLOW_OCEAN = 0.5F;

    private static final float COAST_BLOCKS = 396.5F;

    private static final float EDGE_FALL_BLOCKS = 1000.0F;

    private static final float COAST_TOLERANCE = 0.01F;

    private static final float CARVE_BLOCKS = 443.0F;

    private static final RiverReach REACH = new RiverReach(CARVE_BLOCKS);

    private static final float ROOT_START = 400.0F;

    private static final float RUN_PER_START = 2.0F;

    private static final float FULL_SIZE = 1.0F;

    private static final float SCALE_TOLERANCE = 1.0E-6F;

    private static final float CAP_TOLERANCE = 0.01F;

    private static final int HALF_NARROW_CHUNKS = 128;

    @Test
    void aRootStopsItsCarveWidthShortOfHalfTheNarrowestLap() {
        WorldFold fold = WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-NARROW_CHUNKS, NARROW_CHUNKS,
                -HALF_NARROW_CHUNKS, HALF_NARROW_CHUNKS)));
        int lapZ = fold.blockDomain(Direction.Axis.Z).domainLength;
        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(fold)) {
            assertEquals(lapZ / 2.0F - CARVE_BLOCKS, REACH.clamp(RtfLap.frame(), Float.MAX_VALUE),
                    "the narrower lap caps a root on every direction");
        }

        WorldFold cylinder = cylinder(NARROW_CHUNKS);
        int lapX = cylinder.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(cylinder)) {
            assertEquals(lapX / 2.0F - CARVE_BLOCKS, REACH.clamp(RtfLap.frame(), Float.MAX_VALUE),
                    "a cylinder caps a root by its looping lap alone");
        }
    }

    @Test
    void aWideLapKeepsTheNetworkAtFullSize() {
        assertEquals(FULL_SIZE, scale(torus(WIDE_CHUNKS)), "a wide torus");
        assertEquals(FULL_SIZE, scale(torus(78)), "a 2496-block torus");
        assertEquals(FULL_SIZE, scale(cylinder(NARROW_CHUNKS)), "an 8192-block cylinder");
    }

    @Test
    void aNarrowLapShrinksTheNetworkSoARootRunsAsFarAgainAsItsStart() {
        assertNarrowed(torus(77), 2464);
        assertNarrowed(torus(64), 2048);
        assertNarrowed(torus(32), 1024);
        assertNarrowed(torus(16), 512);
        assertNarrowed(cylinder(32), 1024);
    }

    private static void assertNarrowed(WorldFold fold, int lap) {
        float scale = lap / 2.0F / (CARVE_BLOCKS + RUN_PER_START * ROOT_START);
        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(fold)) {
            assertEquals(scale, REACH.scale(RtfLap.frame()), SCALE_TOLERANCE, "a lap of " + lap + " blocks");
            assertEquals(RUN_PER_START * ROOT_START * scale, REACH.clamp(RtfLap.frame(), Float.MAX_VALUE),
                    CAP_TOLERANCE, "a lap of " + lap + " blocks caps a root at twice its start");
        }
    }

    private static float scale(WorldFold fold) {
        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(fold)) {
            return REACH.scale(RtfLap.frame());
        }
    }

    @Test
    void rootsPerContinentFollowTheSnappedCell() {
        assertEquals(3, rootCount(torus(NARROW_CHUNKS)), "an 8192-block torus snaps the cell to 4096 of 12000");
        assertEquals(8, rootCount(torus(WIDE_CHUNKS)), "a wide torus keeps the preset's cell and its roots");
        assertEquals(7, rootCount(cylinder(NARROW_CHUNKS)), "a cylinder shrinks the cell on its looping axis alone");
        assertEquals(1, rootCount(torus(32)), "a root count never rounds to zero");
    }

    @Test
    void theCoastLiesBetweenTheSteps() {
        RiverReach.EdgeField sloped = (x, z) -> SHALLOW_OCEAN + (COAST_BLOCKS - x) / EDGE_FALL_BLOCKS;
        float coast = RiverReach.distanceToOcean(sloped, SHALLOW_OCEAN, 0, 0, 1.0F, 0.0F, RiverReach.UNBOUNDED);
        assertEquals(COAST_BLOCKS, coast, COAST_TOLERANCE,
                "a coast at 396.5 blocks never rounds to the 400-block step a root starts on");
    }

    private static int rootCount(WorldFold fold) {
        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(fold)) {
            return RiverReach.rootCount(RtfLap.frame(), PRESET_ROOTS, CONTINENT_SCALE);
        }
    }

    private static WorldFold torus(int chunks) {
        return WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-chunks, chunks, -chunks, chunks)));
    }

    private static WorldFold cylinder(int chunks) {
        return WorldFolds.of(ShapeFixtures.cylinder(new WorldLoopBounds(new AxisBounds.Looped(-chunks, chunks),
                AxisBounds.Unbounded.INSTANCE)));
    }
}
