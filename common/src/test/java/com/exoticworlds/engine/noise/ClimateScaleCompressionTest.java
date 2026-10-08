package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;

class ClimateScaleCompressionTest {
    private static final WorldLoopBounds BOUNDS = new WorldLoopBounds(-16, 16, -12, 12);

    private static final int SHORTER_AXIS_BLOCKS = 384;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, 7));

    @Test
    void aSkewedLatticeLapsOnItsShorterAxisLikeItsTorus() {
        assertEquals(SHORTER_AXIS_BLOCKS, ClimateScaleCompression.lapBlocks(SKEWED));
        assertEquals(ClimateScaleCompression.lapBlocks(WorldFolds.of(FlatShape.torus(BOUNDS))),
                ClimateScaleCompression.lapBlocks(SKEWED));
        assertTrue(ClimateScaleCompression.compressible(SKEWED, HORIZONTAL_SHARE));
    }

    @Test
    void theScaleLadderReadsASkewedLattice() {
        assertSame(NoiseScaleLadder.NONE, NoiseScaleLadder.of(SKEWED, List.of()));
    }
}
