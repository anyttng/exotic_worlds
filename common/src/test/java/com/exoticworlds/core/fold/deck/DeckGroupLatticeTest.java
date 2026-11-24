package com.exoticworlds.core.fold.deck;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.SeamTransform;

import org.junit.jupiter.api.Test;

class DeckGroupLatticeTest {

    @Test
    void aDeckGroupStepRefusesATurnedGenerator() {
        FoldOrientation turn = FoldOrientation.QUARTER_TURN_CLOCKWISE;
        SeamTransform turned = new SeamTransform(turn.isXFlipped() ? -1 : 1, turn.isZFlipped() ? -1 : 1, 256, 0,
                turn.isSwappingAxes());
        assertThrows(IllegalArgumentException.class, () -> new DeckGroupLattice.Step(turned, true),
                "a turned generator reached the per-axis reduction");
    }
}
