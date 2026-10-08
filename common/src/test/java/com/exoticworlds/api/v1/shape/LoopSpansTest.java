package com.exoticworlds.api.v1.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldLoopBounds;

import net.minecraft.core.Direction;

class LoopSpansTest {

    @Test
    void anUnskewedSpanAnswersNoSkew() {
        assertEquals(FlatShape.NO_SKEW, LoopSpans.ofWidths(64, 128).skewChunks());
    }

    @Test
    void aSkewIsBroughtIntoTheHalfOpenRangeAroundZero() {
        assertEquals(8, LoopSpans.ofWidth(64).withSkew(8).skewChunks());
        assertEquals(-4, LoopSpans.ofWidth(64).withSkew(60).skewChunks());
        assertEquals(32, LoopSpans.ofWidth(64).withSkew(32).skewChunks());
        assertEquals(FlatShape.NO_SKEW, LoopSpans.ofWidth(64).withSkew(64).skewChunks());
    }

    @Test
    void theSkewTakesPartInEquality() {
        assertNotEquals(LoopSpans.ofWidth(64), LoopSpans.ofWidth(64).withSkew(8));
        assertEquals(LoopSpans.ofWidth(64).withSkew(-4), LoopSpans.ofWidth(64).withSkew(60));
        assertEquals(LoopSpans.ofWidth(64).withSkew(-4).hashCode(), LoopSpans.ofWidth(64).withSkew(60).hashCode());
    }

    @Test
    void aSkewNeedsBothAxesLooping() {
        assertThrows(IllegalArgumentException.class, () -> LoopSpans.ofWidth(Direction.Axis.X, 64).withSkew(8));
    }

    @Test
    void scalingDownDividesTheSkewWithTheWidths() {
        assertEquals(LoopSpans.ofWidths(16, 32).withSkew(2), LoopSpans.ofWidths(64, 128).withSkew(8).scaledDown(4));
    }

    @Test
    void aScaleThatDoesNotDivideTheSkewIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LoopSpans.ofWidth(64).withSkew(6).scaledDown(4));
    }

    @Test
    void theFlatShapeOfSpansCarriesTheSkew() {
        assertEquals(FlatShape.latticeTorus(WorldLoopBounds.ofWidths(64, 128), 8),
                FlatShape.of(LoopSpans.ofWidths(64, 128).withSkew(8)));
        assertEquals(FlatShape.torus(WorldLoopBounds.ofWidths(64, 128)), FlatShape.of(LoopSpans.ofWidths(64, 128)));
    }
}
