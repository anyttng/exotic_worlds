package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;

import net.minecraft.core.BlockPos;

class SeamRangeTest {
    private static final WorldFold WORLD =
            WorldFolds.of(ShapeFixtures.torus(ShapeFixtures.loopedBounds(-16, 16, -16, 16)));
    private static final WorldFold NO_FOLD = null;

    private static final BlockPos EAST_BLOCK = new BlockPos(250, 70, 0);
    private static final BlockPos WEST_BLOCK = new BlockPos(-250, 64, 8);

    @Test
    void aNullFoldGivesThePlainDistance() {
        assertEquals(EAST_BLOCK.distSqr(WEST_BLOCK), SeamRange.sqr(NO_FOLD, EAST_BLOCK, WEST_BLOCK));
    }

    @Test
    void anUnwrappedFoldGivesThePlainDistance() {
        assertEquals(EAST_BLOCK.distSqr(WEST_BLOCK), SeamRange.sqr(WorldFolds.NOOP, EAST_BLOCK, WEST_BLOCK));
    }

    @Test
    void aPairStraddlingTheSeamMeasuresTheShortWay() {
        assertEquals(12.0 * 12.0 + 6.0 * 6.0 + 8.0 * 8.0, SeamRange.sqr(WORLD, EAST_BLOCK, WEST_BLOCK));
    }

    @Test
    void theShortWayIsTheSameFromEitherSide() {
        assertEquals(SeamRange.sqr(WORLD, EAST_BLOCK, WEST_BLOCK), SeamRange.sqr(WORLD, WEST_BLOCK, EAST_BLOCK));
    }
}
