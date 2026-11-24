package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class SeamDeltaTest {
    private static final long SEED = 0xDE17AL;
    private static final int SAMPLES = 2000;
    private static final int REACH_BLOCKS = 5000;

    private static final WorldLoopBounds BOUNDS = WorldLoopBounds.ofWidth(64);
    private static final int WIDTH_BLOCKS = 64 * 16;
    private static final int SKEW_BLOCKS = 5 * 16;

    private static final WorldFold TORUS = WorldFolds.of(ShapeFixtures.torus(BOUNDS));
    private static final WorldFold SKEWED = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, 5));

    @Test
    void onTheTorusEachAxisFoldsAsItsOwnDomainDoes() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            Vec3 delta = new Vec3(random.nextDouble(-REACH_BLOCKS, REACH_BLOCKS), 3.0, random.nextDouble(-REACH_BLOCKS, REACH_BLOCKS));

            Vec3 folded = SeamDelta.fold(TORUS, delta);

            assertEquals(TORUS.getBlockDomain(Direction.Axis.X).unwrapAround(0.0, delta.x), folded.x);
            assertEquals(delta.y, folded.y);
            assertEquals(TORUS.getBlockDomain(Direction.Axis.Z).unwrapAround(0.0, delta.z), folded.z);
        }
    }

    @Test
    void aDeltaNeedingNoFoldComesBackAsTheSameInstance() {
        Vec3 delta = new Vec3(10.0, 0.0, -20.0);

        assertSame(delta, SeamDelta.fold(TORUS, delta));
        assertSame(delta, SeamDelta.fold(SKEWED, delta));
    }

    @Test
    void onASkewedWorldAZLapTakesItsXShiftWithIt() {
        assertEquals(new Vec3(60.0 - SKEW_BLOCKS, 0.0, 24.0), SeamDelta.fold(SKEWED, 60.0, 24.0 + WIDTH_BLOCKS));
        assertEquals(new BlockPos(60 - SKEW_BLOCKS, 7, 24), SeamDelta.fold(SKEWED, new BlockPos(60, 7, 24 + WIDTH_BLOCKS)));
    }

    @Test
    void onASkewedWorldTheFoldedDeltaIsNeverLongerThanTheRawOne() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            Vec3 delta = new Vec3(random.nextDouble(-REACH_BLOCKS, REACH_BLOCKS), 0.0, random.nextDouble(-REACH_BLOCKS, REACH_BLOCKS));

            Vec3 folded = SeamDelta.fold(SKEWED, delta);

            assertEquals(SKEWED.fold(delta).x, SKEWED.fold(folded).x, 1e-9);
            assertEquals(SKEWED.fold(delta).z, SKEWED.fold(folded).z, 1e-9);
            assertTrue(folded.horizontalDistanceSqr() <= delta.horizontalDistanceSqr() + 1e-9);
        }
    }
}
