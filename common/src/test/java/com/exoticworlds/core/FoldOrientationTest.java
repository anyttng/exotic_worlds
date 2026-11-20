package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class FoldOrientationTest {
    private static final double TOLERANCE = 1.0e-3;

    private static final Vec3 DELTA = new Vec3(2.0, 7.0, 3.0);
    private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
    private static final Vec3 OFF_CENTER_OFFSET = new Vec3(0.25, 0.625, 0.875);

    private static final List<Direction> HORIZONTAL =
            List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static final List<FoldOrientation> TURNS = List.of(
            FoldOrientation.IDENTITY,
            FoldOrientation.QUARTER_TURN_CLOCKWISE,
            FoldOrientation.HALF_TURN,
            FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE);

    private static Vec3 faceCenter(Direction face) {
        return BLOCK_CENTER.add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
    }

    private static FoldOrientation inverseOf(FoldOrientation orientation) {
        return new SeamTransform(
                orientation.flipsX() ? -1 : 1, orientation.flipsZ() ? -1 : 1, 0, 0, orientation.swapsAxes())
                .inverse()
                .orientation();
    }

    private static void assertClose(Vec3 expected, Vec3 actual, String what) {
        assertEquals(expected.x, actual.x, TOLERANCE, what + " on x");
        assertEquals(expected.y, actual.y, TOLERANCE, what + " on y");
        assertEquals(expected.z, actual.z, TOLERANCE, what + " on z");
    }

    @Nested
    class Group {
        @Test
        void fourQuarterTurnsComposeToTheIdentity() {
            for (FoldOrientation quarter : List.of(
                    FoldOrientation.QUARTER_TURN_CLOCKWISE, FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE)) {
                FoldOrientation twice = quarter.compose(quarter);
                assertEquals(FoldOrientation.HALF_TURN, twice, quarter + " twice is not a half turn");
                assertEquals(FoldOrientation.IDENTITY, twice.compose(twice), "four of " + quarter + " leave a turn");
            }
        }

        @Test
        void anOrientationComposedWithItsInverseIsTheIdentity() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                FoldOrientation inverse = inverseOf(orientation);
                assertEquals(FoldOrientation.IDENTITY, orientation.compose(inverse),
                        orientation + " then its inverse leaves a turn");
                assertEquals(FoldOrientation.IDENTITY, inverse.compose(orientation),
                        "the inverse of " + orientation + " then itself leaves a turn");
            }
        }

        @Test
        void theClockwiseTurnUndoesTheCounterclockwiseOne() {
            assertEquals(FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE,
                    inverseOf(FoldOrientation.QUARTER_TURN_CLOCKWISE), "a clockwise turn is undone by another");
        }

        @Test
        void composingIsApplyingTheFirstThenTheNext() {
            for (FoldOrientation first : FoldOrientation.values()) {
                for (FoldOrientation next : FoldOrientation.values()) {
                    assertEquals(next.applyToDelta(first.applyToDelta(DELTA)),
                            first.compose(next).applyToDelta(DELTA),
                            first + " then " + next + " is not their composition");
                }
            }
        }

        @Test
        void aTurnComposedWithAMirrorIsADiagonalMirror() {
            assertEquals(FoldOrientation.MIRROR_DIAGONAL,
                    FoldOrientation.QUARTER_TURN_CLOCKWISE.compose(FoldOrientation.MIRROR_X),
                    "a turn then a mirror has no value");
            assertEquals(FoldOrientation.MIRROR_ANTIDIAGONAL,
                    FoldOrientation.MIRROR_X.compose(FoldOrientation.QUARTER_TURN_CLOCKWISE),
                    "a mirror then a turn has no value");
        }

        @Test
        void onlyTheTurnsKeepHandedness() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(TURNS.contains(orientation), orientation.preservesHandedness(),
                        orientation + " reports the wrong handedness");
            }
        }

        @Test
        void thePartsNameTheirOwnValue() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertSame(orientation,
                        FoldOrientation.of(orientation.swapsAxes(), orientation.flipsX(), orientation.flipsZ()),
                        orientation + " is not the value of its own parts");
            }
        }
    }

    @Nested
    class Faces {
        @Test
        void theQuarterTurnsFollowTheGameCompass() {
            for (Direction face : HORIZONTAL) {
                assertEquals(face.getClockWise(), FoldOrientation.QUARTER_TURN_CLOCKWISE.applyToFace(face),
                        face + " did not turn clockwise");
                assertEquals(face.getCounterClockWise(),
                        FoldOrientation.QUARTER_TURN_COUNTERCLOCKWISE.applyToFace(face),
                        face + " did not turn counterclockwise");
            }
        }

        @Test
        void aVerticalFaceIsLeftAlone() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(Direction.UP, orientation.applyToFace(Direction.UP), orientation + " moved up");
                assertEquals(Direction.DOWN, orientation.applyToFace(Direction.DOWN), orientation + " moved down");
            }
        }

        @Test
        void aFaceTurnsWithItsNormal() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                for (Direction face : HORIZONTAL) {
                    assertClose(orientation.applyToFace(face).getUnitVec3(),
                            orientation.applyToDelta(face.getUnitVec3()),
                            orientation + " turns the " + face + " face away from its normal");
                }
            }
        }
    }

    @Nested
    class BlockOffsets {
        @Test
        void aPointOnAFaceLandsOnTheTurnedFace() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                for (Direction face : HORIZONTAL) {
                    assertEquals(faceCenter(orientation.applyToFace(face)),
                            orientation.applyToBlockOffset(faceCenter(face)),
                            orientation + " carries the " + face + " face center off the turned face");
                }
            }
        }

        @Test
        void anOffsetTurnsAboutTheBlockCenterAsADeltaDoes() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(orientation.applyToDelta(OFF_CENTER_OFFSET.subtract(BLOCK_CENTER)),
                        orientation.applyToBlockOffset(OFF_CENTER_OFFSET).subtract(BLOCK_CENTER),
                        orientation + " turns an in-block offset and a delta apart");
            }
        }

        @Test
        void theIdentityHandsItsArgumentsBack() {
            assertSame(DELTA, FoldOrientation.IDENTITY.applyToDelta(DELTA), "an upright delta was rebuilt");
            assertSame(OFF_CENTER_OFFSET, FoldOrientation.IDENTITY.applyToBlockOffset(OFF_CENTER_OFFSET),
                    "an upright offset was rebuilt");
        }
    }
}
