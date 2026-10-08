package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class SeamDeltaTest {
    private static final long SEED = 0xDE17AL;
    private static final int SAMPLES = 2000;
    private static final int REACH = 5000;

    private static final WorldLoopBounds BOUNDS = WorldLoopBounds.ofWidth(64);
    private static final int WIDTH = 64 * 16;
    private static final int SKEW = 5 * 16;

    private static final WorldFold TORUS = WorldFolds.of(FlatShape.torus(BOUNDS));
    private static final WorldFold SKEWED = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, 5));

    @Test
    void onTheTorusEachAxisFoldsAsItsOwnDomainDoes() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            Vec3 delta = new Vec3(random.nextDouble(-REACH, REACH), 3.0, random.nextDouble(-REACH, REACH));

            Vec3 folded = SeamDelta.fold(TORUS, delta);

            assertEquals(TORUS.blockDomain(Direction.Axis.X).unwrapAround(0.0, delta.x), folded.x);
            assertEquals(delta.y, folded.y);
            assertEquals(TORUS.blockDomain(Direction.Axis.Z).unwrapAround(0.0, delta.z), folded.z);
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
        assertEquals(new Vec3(60.0 - SKEW, 0.0, 24.0), SeamDelta.fold(SKEWED, 60.0, 24.0 + WIDTH));
        assertEquals(new BlockPos(60 - SKEW, 7, 24), SeamDelta.fold(SKEWED, new BlockPos(60, 7, 24 + WIDTH)));
    }

    @Test
    void onASkewedWorldTheFoldedDeltaIsNeverLongerThanTheRawOne() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            Vec3 delta = new Vec3(random.nextDouble(-REACH, REACH), 0.0, random.nextDouble(-REACH, REACH));

            Vec3 folded = SeamDelta.fold(SKEWED, delta);

            assertEquals(SKEWED.fold(delta).x, SKEWED.fold(folded).x, 1e-9);
            assertEquals(SKEWED.fold(delta).z, SKEWED.fold(folded).z, 1e-9);
            assertTrue(folded.horizontalDistanceSqr() <= delta.horizontalDistanceSqr() + 1e-9);
        }
    }
}
