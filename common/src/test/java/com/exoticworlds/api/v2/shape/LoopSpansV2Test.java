package com.exoticworlds.api.v2.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class LoopSpansV2Test {

    @Test
    void aWidthIsCenteredOnTheOrigin() {
        LoopSpansV2 even = LoopSpansV2.ofWidth(64);
        LoopSpansV2 odd = LoopSpansV2.ofWidth(Direction.Axis.Z, 33);

        assertEquals(-32, even.minChunk(Direction.Axis.X));
        assertEquals(32, even.maxChunk(Direction.Axis.Z));
        assertEquals(-16, odd.minChunk(Direction.Axis.Z));
        assertEquals(17, odd.maxChunk(Direction.Axis.Z));
        assertFalse(odd.loops(Direction.Axis.X));
    }

    @Test
    void aSkewIsBroughtIntoHalfTheXWidth() {
        LoopSpansV2 spans = LoopSpansV2.ofWidths(64, 128);

        assertEquals(8, spans.withSkew(72).skewChunks());
        assertEquals(24, spans.withSkew(-40).skewChunks());
        assertEquals(spans.withSkew(8), spans.withSkew(72));
    }

    @Test
    void aSkewWhereAnAxisDoesNotLoopIsRefused() {
        LoopSpansV2 cylinder = LoopSpansV2.ofWidth(Direction.Axis.Z, 64);

        assertThrows(IllegalArgumentException.class, () -> cylinder.withSkew(2));
    }

    @Test
    void aSpanThatDoesNotAscendIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new LoopSpansV2.Span(4, 4));
    }

    @Test
    void scalingDownDividesTheWidthsAndTheSkew() {
        assertEquals(LoopSpansV2.ofWidths(16, 32).withSkew(2), LoopSpansV2.ofWidths(64, 128).withSkew(8).scaledDown(4));
    }

    @Test
    void aScaleThatDoesNotDivideTheSkewIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LoopSpansV2.ofWidth(64).withSkew(6).scaledDown(4));
    }

    @Test
    void theYAxisNeverLoopsAndIsRefused() {
        LoopSpansV2 torus = LoopSpansV2.ofWidth(64);

        assertFalse(torus.loops(Direction.Axis.Y));
        assertThrows(IllegalArgumentException.class, () -> torus.chunkWidth(Direction.Axis.Y));
        assertThrows(IllegalArgumentException.class, () -> LoopSpansV2.NONE.and(Direction.Axis.Y, 0, 4));
    }

    @Test
    void onlyTwoLoopedAxesOfOneWidthAreSquare() {
        assertTrue(LoopSpansV2.ofWidth(64).isSquare());
        assertFalse(LoopSpansV2.ofWidths(64, 128).isSquare());
        assertFalse(LoopSpansV2.ofWidth(Direction.Axis.X, 64).isSquare());
    }
}
