package com.exoticworlds.api.v2.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class LoopSpansTest {

    @Test
    void aWidthIsCenteredOnTheOrigin() {
        LoopSpans even = LoopSpans.ofWidth(64);
        LoopSpans odd = LoopSpans.ofWidth(Direction.Axis.Z, 33);

        assertEquals(-32, even.minChunk(Direction.Axis.X));
        assertEquals(32, even.maxChunk(Direction.Axis.Z));
        assertEquals(-16, odd.minChunk(Direction.Axis.Z));
        assertEquals(17, odd.maxChunk(Direction.Axis.Z));
        assertFalse(odd.loops(Direction.Axis.X));
    }

    @Test
    void aSkewIsBroughtIntoHalfTheXWidth() {
        LoopSpans spans = LoopSpans.ofWidths(64, 128);

        assertEquals(8, spans.withSkew(72).skewChunks());
        assertEquals(24, spans.withSkew(-40).skewChunks());
        assertEquals(spans.withSkew(8), spans.withSkew(72));
    }

    @Test
    void aSkewWhereAnAxisDoesNotLoopIsRefused() {
        LoopSpans cylinder = LoopSpans.ofWidth(Direction.Axis.Z, 64);

        assertThrows(IllegalArgumentException.class, () -> cylinder.withSkew(2));
    }

    @Test
    void aSpanThatDoesNotAscendIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new LoopSpans.Span(4, 4));
    }

    @Test
    void scalingDownDividesTheWidthsAndTheSkew() {
        assertEquals(LoopSpans.ofWidths(16, 32).withSkew(2), LoopSpans.ofWidths(64, 128).withSkew(8).scaledDown(4));
    }

    @Test
    void aScaleThatDoesNotDivideTheSkewIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LoopSpans.ofWidth(64).withSkew(6).scaledDown(4));
    }

    @Test
    void theYAxisNeverLoopsAndIsRefused() {
        LoopSpans torus = LoopSpans.ofWidth(64);

        assertFalse(torus.loops(Direction.Axis.Y));
        assertThrows(IllegalArgumentException.class, () -> torus.chunkWidth(Direction.Axis.Y));
        assertThrows(IllegalArgumentException.class, () -> LoopSpans.NONE.and(Direction.Axis.Y, 0, 4));
    }

    @Test
    void onlyTwoLoopedAxesOfOneWidthAreSquare() {
        assertTrue(LoopSpans.ofWidth(64).isSquare());
        assertFalse(LoopSpans.ofWidths(64, 128).isSquare());
        assertFalse(LoopSpans.ofWidth(Direction.Axis.X, 64).isSquare());
    }
}
