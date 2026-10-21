package com.exoticworlds.compat.ultimatemapatlases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapShapes;

import net.minecraft.core.BlockPos;

class AtlasBlockMarkerKeyTest {
    @Test
    void aKeyPastTheSeamFoldsToTheCanonicalBlock() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        assertEquals("block_-212, 64, 10", AtlasBlockMarkerKey.folded(shape, "block_300, 64, 10"),
                "the bed a lap up keys as the canonical one");
        assertEquals("block_100, 64, 212", AtlasBlockMarkerKey.folded(shape, "block_100, 64, -300"),
                "Z folds the same way below the lower seam");
    }

    @Test
    void aCanonicalKeyComesBackItself() {
        String key = "block_-212, 64, 10";
        assertSame(key, AtlasBlockMarkerKey.folded(MapShapes.torus(-16, 16), key), "nothing to fold");
    }

    @Test
    void aSkewedSeamMovesTheKeySideways() {
        ToroidalShape shape = MapShapes.latticeTorus(-16, 16, 8);
        BlockPos canonical = shape.fold(new BlockPos(0, 64, 500));
        assertEquals(-12, canonical.getZ(), "a lap in Z");
        assertEquals(128, Math.abs(canonical.getX()), "moves X by the skew");
        assertEquals("block_" + canonical.toShortString(), AtlasBlockMarkerKey.folded(shape, "block_0, 64, 500"),
                "and the key follows both axes");
    }

    @Test
    void aCylinderFoldsItsLoopedAxisAlone() {
        assertEquals("block_-212, 64, 5000", AtlasBlockMarkerKey.folded(MapShapes.cylinder(-16, 16),
                "block_300, 64, 5000"), "X folds, the unbounded Z stays");
    }

    @Test
    void anythingButABlockKeyIsLeftAlone() {
        ToroidalShape shape = MapShapes.torus(-16, 16);
        String pin = "pin_3_12345_0";
        String partial = "block_300, 64";
        String words = "block_a, b, c";
        String pastSeam = "block_300, 64, 10";
        assertSame(pin, AtlasBlockMarkerKey.folded(shape, pin), "a pin key");
        assertSame(partial, AtlasBlockMarkerKey.folded(shape, partial), "a key missing a coordinate");
        assertSame(words, AtlasBlockMarkerKey.folded(shape, words), "a key that is no number");
        assertSame(pastSeam, AtlasBlockMarkerKey.folded(null, pastSeam), "no shape");
    }
}
