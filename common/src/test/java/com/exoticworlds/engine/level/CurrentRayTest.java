package com.exoticworlds.engine.level;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ForeignFrame;
import com.exoticworlds.core.ForeignSpan;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.world.phys.Vec3;

class CurrentRayTest {
    private static final int WORLD_CHUNKS = 32;
    private static final int PLOT_ORIGIN = 10000;
    private static final int LOG_PLOT_SIZE = 7;
    private static final int LOG_SIDE_LENGTH = 7;
    private static final int PLOT_MIN_CHUNK = PLOT_ORIGIN << LOG_PLOT_SIZE;
    private static final int PLOT_MAX_CHUNK = (PLOT_ORIGIN + (1 << LOG_SIDE_LENGTH)) << LOG_PLOT_SIZE;
    private static final int PLOT_MIN_BLOCK = PLOT_MIN_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int PLOT_MAX_BLOCK = PLOT_MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final double Y = 64.5;

    private static final FlatShape SHAPE = ShapeFixtures.torus(WorldLoopBounds.ofWidth(WORLD_CHUNKS));
    private static final ForeignSpan PLOT_CHUNKS = new ForeignSpan(PLOT_MIN_CHUNK, PLOT_MAX_CHUNK);
    private static final WorldFold FRAMED = WorldFolds.of(SHAPE, List.of(new ForeignFrame(PLOT_CHUNKS, PLOT_CHUNKS)));
    private static final WorldFold BARE = WorldFolds.of(SHAPE);

    private static final Vec3 INLAND = new Vec3(10.5, Y, 10.5);
    private static final Vec3 INLAND_FAR_SIDE = new Vec3(100.5, Y, -50.5);
    private static final Vec3 SHORT_OF_THE_GRID = new Vec3(PLOT_MIN_BLOCK - 0.5, Y, 10.5);
    private static final Vec3 INSIDE_THE_GRID_ON_X = new Vec3(PLOT_MIN_BLOCK + 8.5, Y, 10.5);
    private static final Vec3 BEFORE_THE_GRID_ON_Z = new Vec3(10.5, Y, PLOT_MIN_BLOCK - 1000.5);
    private static final Vec3 PAST_THE_GRID_ON_Z = new Vec3(10.5, Y, PLOT_MAX_BLOCK + 1000.5);

    @Test
    void aRayInsideTheBoundsMeetsNoFrame() {
        assertFalse(meets(FRAMED, INLAND, INLAND_FAR_SIDE));
    }

    @Test
    void aFarRayStoppingShortOfTheGridMeetsNoFrame() {
        assertFalse(meets(FRAMED, INLAND, SHORT_OF_THE_GRID));
    }

    @Test
    void aRayEndingInsideTheGridOnXMeetsTheFrame() {
        assertTrue(meets(FRAMED, INLAND, INSIDE_THE_GRID_ON_X));
        assertTrue(meets(FRAMED, INSIDE_THE_GRID_ON_X, INLAND));
    }

    @Test
    void aRayCrossingTheGridOnZMeetsTheFrameWithNeitherEndInside() {
        assertTrue(meets(FRAMED, BEFORE_THE_GRID_ON_Z, PAST_THE_GRID_ON_Z));
    }

    @Test
    void aFoldWithoutAFrameMeetsNone() {
        assertFalse(meets(WorldFolds.NOOP, INLAND, INSIDE_THE_GRID_ON_X));
        assertFalse(meets(BARE, INLAND, INSIDE_THE_GRID_ON_X));
    }

    @Test
    void theAnswerIsTheReadersFold() {
        CurrentRay outer = CurrentRay.enter(INLAND, INSIDE_THE_GRID_ON_X);
        try {
            assertFalse(CurrentRay.meetsForeignFrame(BARE));
            assertTrue(CurrentRay.meetsForeignFrame(FRAMED));
        } finally {
            CurrentRay.exit(outer);
        }
    }

    @Test
    void noRayMeetsNoFrame() {
        assertFalse(CurrentRay.meetsForeignFrame(FRAMED));
    }

    @Test
    void aNestedRayRestoresTheOuterOne() {
        CurrentRay none = CurrentRay.enter(INLAND, INSIDE_THE_GRID_ON_X);
        try {
            CurrentRay outer = CurrentRay.enter(INLAND, INLAND_FAR_SIDE);
            try {
                assertFalse(CurrentRay.meetsForeignFrame(FRAMED));
            } finally {
                CurrentRay.exit(outer);
            }

            assertTrue(CurrentRay.meetsForeignFrame(FRAMED));
        } finally {
            CurrentRay.exit(none);
        }

        assertFalse(CurrentRay.meetsForeignFrame(FRAMED));
    }

    private static boolean meets(WorldFold fold, Vec3 from, Vec3 to) {
        CurrentRay outer = CurrentRay.enter(from, to);
        try {
            return CurrentRay.meetsForeignFrame(fold);
        } finally {
            CurrentRay.exit(outer);
        }
    }
}
