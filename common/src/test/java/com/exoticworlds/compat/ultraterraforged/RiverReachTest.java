package com.exoticworlds.compat.ultraterraforged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.terraforged.RtfLap;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.continent.SimpleContinent;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.Rivermap;

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

    @Test
    void aRiverNetworkFitsInsideHalfALap() {
        WorldFold fold = torus(NARROW_CHUNKS);
        int lap = fold.blockDomain(Direction.Axis.X).domainLength;
        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(fold)) {
            RtfLap.Frame frame = RtfLap.frame();
            float length = RiverReach.clamp(frame, Float.MAX_VALUE, 1.0F, 0.0F);
            assertTrue(length * 1.64F + 1024.0F <= lap / 2.0F + 1.0F, "a network of " + length + " leaves half a lap");
            assertTrue(RiverReach.fits(frame));
        }

        try (RtfLap.Frame.Scope _ = RtfLap.frame().bind(torus(32))) {
            assertFalse(RiverReach.fits(RtfLap.frame()), "a 1024-block world carries no rivers");
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
        float coast = RiverReach.distanceToOcean(new SlopedContinent(), SHALLOW_OCEAN, 0, 0, 1.0F, 0.0F,
                RiverReach.UNBOUNDED);
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

    private static final class SlopedContinent implements SimpleContinent {
        @Override
        public float getEdgeValue(float x, float z) {
            return SHALLOW_OCEAN + (COAST_BLOCKS - x) / EDGE_FALL_BLOCKS;
        }

        @Override
        public long getNearestCenter(float x, float z) {
            return 0L;
        }

        @Override
        public Rivermap getRivermap(int x, int z) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void apply(Cell cell, float x, float z) {
        }
    }
}
