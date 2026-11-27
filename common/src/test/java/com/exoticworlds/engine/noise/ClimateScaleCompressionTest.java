package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

class ClimateScaleCompressionTest {
    private static final WorldLoopBounds BOUNDS = ShapeFixtures.loopedBounds(-16, 16, -12, 12);

    private static final int SHORTER_AXIS_BLOCKS = 384;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, 7),
            ShapeOptions.DEFAULT);

    @Test
    void aSkewedLatticeLapsOnItsShorterAxisLikeItsTorus() {
        assertEquals(SHORTER_AXIS_BLOCKS, ClimateScaleCompression.lapBlocks(SKEWED));
        assertEquals(ClimateScaleCompression.lapBlocks(WorldFolds.of(ShapeFixtures.torus(BOUNDS))),
                ClimateScaleCompression.lapBlocks(SKEWED));
        assertTrue(ClimateScaleCompression.isCompressible(SKEWED, HORIZONTAL_SHARE));
    }

    @Test
    void theScaleLadderReadsASkewedLattice() {
        assertSame(NoiseScaleLadder.NONE, NoiseScaleLadder.of(SKEWED, List.of(), null));
    }
}
