package com.exoticworlds.compat.immersiveoverlays;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;

class CompassOverlayFoldTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH = (MAX_CHUNK - MIN_CHUNK) * 16;
    private static final int SKEW_CHUNKS = 3;
    private static final int SKEW = SKEW_CHUNKS * 16;
    private static final int Y = 64;
    private static final int LAPS = 3;

    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK);
    private static final WorldLoopBounds BOTH = new WorldLoopBounds(LOOPED, LOOPED);
    private static final WorldLoopBounds X_ONLY = new WorldLoopBounds(LOOPED, AxisBounds.Unbounded.INSTANCE);

    private static final BlockPos X_LAP = new BlockPos(WIDTH, 0, 0);
    private static final BlockPos Z_LAP = new BlockPos(0, 0, WIDTH);
    private static final BlockPos SKEWED_Z_LAP = new BlockPos(SKEW, 0, WIDTH);

    @Test
    void theTorusOffsetCrossesTheXSeamAtAnyLap() {
        assertShortWayAtEveryLap(WorldFolds.of(ShapeFixtures.torus(BOTH)), X_LAP, Z_LAP,
                new BlockPos(125, Y, 0), new BlockPos(-125, Y, 0), new BlockPos(-6, 0, 0));
    }

    @Test
    void theLatticeTorusOffsetCrossesTheSkewedZSeamAtAnyLap() {
        assertShortWayAtEveryLap(WorldFolds.of(FlatShape.latticeTorus(BOTH, SKEW_CHUNKS)), X_LAP, SKEWED_Z_LAP,
                new BlockPos(0, Y, 125), new BlockPos(-SKEW, Y, -125), new BlockPos(0, 0, -6));
    }

    @Test
    void theCylinderOffsetCrossesItsLoopedAxisAndKeepsTheOpenOne() {
        assertShortWayAtEveryLap(WorldFolds.of(ShapeFixtures.cylinder(X_ONLY)), X_LAP, BlockPos.ZERO,
                new BlockPos(125, Y, 40), new BlockPos(-125, Y, -30), new BlockPos(-6, 0, 70));
    }

    private static void assertShortWayAtEveryLap(WorldFold fold, BlockPos firstLap, BlockPos secondLap,
            BlockPos player, BlockPos anchor, BlockPos shortWay) {
        for (int first = -LAPS; first <= LAPS; first++) {
            for (int second = -LAPS; second <= LAPS; second++) {
                BlockPos raw = player.offset(firstLap.multiply(first)).offset(secondLap.multiply(second));
                BlockPos shown = CompassOverlayFold.shown(fold, raw);
                BlockPos seated = CompassOverlayFold.anchor(fold, raw, anchor);
                String lap = "laps " + first + ", " + second + " from " + raw;
                assertEquals(player, shown, "the shown position, " + lap);
                assertEquals(shortWay, shown.subtract(seated), "the offset to the anchor, " + lap);
            }
        }
    }
}
