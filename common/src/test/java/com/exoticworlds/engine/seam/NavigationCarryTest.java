package com.exoticworlds.engine.seam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.SeamTransform;

import net.minecraft.core.Vec3i;

class NavigationCarryTest {
    private static final int[] FOOTPRINTS = {1, 2, 5};
    private static final int[] COORDS = {0, 1, -1, 7, -7, 255, -256};
    private static final int[] SHIFTS_BLOCKS = {0, 1, -96, 512};
    private static final int NODE_Y = 64;
    private static final double HALF_BLOCK = 0.5;
    private static final double EXACT = 0.0;

    private static List<SeamTransform> moves() {
        List<SeamTransform> built = new ArrayList<>();
        for (FoldOrientation orientation : FoldOrientation.values()) {
            for (int shiftBlocks : SHIFTS_BLOCKS) {
                built.add(new SeamTransform(orientation.isXFlipped() ? -1 : 1, orientation.isZFlipped() ? -1 : 1,
                        shiftBlocks, -shiftBlocks, orientation.isSwappingAxes()));
            }
        }

        return built;
    }

    @Test
    void theBodyStandingOnACarriedNodeIsTheCarriedBody() {
        for (SeamTransform move : moves()) {
            for (int footprint : FOOTPRINTS) {
                for (int x : COORDS) {
                    for (int z : COORDS) {
                        assertBodyCarried(move, footprint, x, z);
                    }
                }
            }
        }
    }

    @Test
    void aTranslationAddsItsShift() {
        SeamTransform move = SeamTransform.translation(512, -96);
        for (int footprint : FOOTPRINTS) {
            assertEquals(new Vec3i(519, NODE_Y, -103),
                    NavigationCarry.node(move, new Vec3i(7, NODE_Y, -7), footprint));
        }
    }

    @Test
    void aMirroredWideFootprintKeepsItsLowCorner() {
        SeamTransform mirror = SeamTransform.glideZ(0, 0);

        assertEquals(-8, NavigationCarry.nodeX(mirror, 7, 3, 1));
        assertEquals(-9, NavigationCarry.nodeX(mirror, 7, 3, 2));
        assertEquals(-12, NavigationCarry.nodeX(mirror, 7, 3, 5));
        assertEquals(3, NavigationCarry.nodeZ(mirror, 7, 3, 5));
    }

    private static void assertBodyCarried(SeamTransform move, int footprint, int x, int z) {
        double half = footprint * HALF_BLOCK;
        double bodyX = x + half;
        double bodyZ = z + half;
        String what = move + " footprint " + footprint + " node " + x + ", " + z;

        assertEquals(move.applyX(bodyX, bodyZ), NavigationCarry.nodeX(move, x, z, footprint) + half, EXACT, what);
        assertEquals(move.applyZ(bodyX, bodyZ), NavigationCarry.nodeZ(move, x, z, footprint) + half, EXACT, what);
    }
}
