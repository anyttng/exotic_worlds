package com.exoticworlds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

    private static final int[] GRID_WIDTHS = {16, 4};
    private static final int SOURCE_CHUNK_X = 2;
    private static final int SOURCE_CHUNK_Z = -7;
    private static final int SHIFT_CHUNKS_X = 3;
    private static final int SHIFT_CHUNKS_Z = -5;

    private static SeamTransform wholeChunkMove(FoldOrientation orientation, int width) {
        return new SeamTransform(
                orientation.flipsX() ? -1 : 1,
                orientation.flipsZ() ? -1 : 1,
                SHIFT_CHUNKS_X * width,
                SHIFT_CHUNKS_Z * width,
                orientation.swapsAxes());
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
        void theInverseIsTheOrientationOfTheInverseMove() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(inverseOf(orientation), orientation.inverse(),
                        orientation + " is undone by another orientation than its inverse move");
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
    class LocalIndices {
        @Test
        void aLocalPairLandsOnTheLowBitsOfTheMovedCell() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                for (int width : GRID_WIDTHS) {
                    assertFollowsTheMove(orientation, width);
                }
            }
        }

        private static void assertFollowsTheMove(FoldOrientation orientation, int width) {
            SeamTransform move = wholeChunkMove(orientation, width);
            for (int localZ = 0; localZ < width; localZ++) {
                for (int localX = 0; localX < width; localX++) {
                    BlockPos moved = move.apply(
                            new BlockPos(SOURCE_CHUNK_X * width + localX, 0, SOURCE_CHUNK_Z * width + localZ));
                    String cell = orientation + " at (" + localX + ", " + localZ + ") on width " + width;
                    assertEquals(Math.floorMod(moved.getX(), width),
                            orientation.applyToLocalX(localX, localZ, width), cell + " misses the moved x");
                    assertEquals(Math.floorMod(moved.getZ(), width),
                            orientation.applyToLocalZ(localX, localZ, width), cell + " misses the moved z");
                }
            }
        }

        @Test
        void permutingByACompositionIsPermutingByTheFirstThenTheNext() {
            for (FoldOrientation first : FoldOrientation.values()) {
                for (FoldOrientation next : FoldOrientation.values()) {
                    for (int width : GRID_WIDTHS) {
                        assertComposes(first, next, width);
                    }
                }
            }
        }

        private static void assertComposes(FoldOrientation first, FoldOrientation next, int width) {
            FoldOrientation composed = first.compose(next);
            for (int localZ = 0; localZ < width; localZ++) {
                for (int localX = 0; localX < width; localX++) {
                    int firstX = first.applyToLocalX(localX, localZ, width);
                    int firstZ = first.applyToLocalZ(localX, localZ, width);
                    String cell = first + " then " + next + " at (" + localX + ", " + localZ + ") on width " + width;
                    assertEquals(next.applyToLocalX(firstX, firstZ, width),
                            composed.applyToLocalX(localX, localZ, width), cell + " is not their composition on x");
                    assertEquals(next.applyToLocalZ(firstX, firstZ, width),
                            composed.applyToLocalZ(localX, localZ, width), cell + " is not their composition on z");
                }
            }
        }

        @Test
        void theIndexTableSendsEachCellWhereItsPairLands() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                for (int width : GRID_WIDTHS) {
                    assertTableFollowsThePair(orientation, width);
                }
            }
        }

        private static void assertTableFollowsThePair(FoldOrientation orientation, int width) {
            int[] table = orientation.localIndexTable(width);
            assertEquals(width * width, table.length, orientation + " on width " + width + " has a short table");
            for (int localZ = 0; localZ < width; localZ++) {
                for (int localX = 0; localX < width; localX++) {
                    int landed = orientation.applyToLocalZ(localX, localZ, width) * width
                            + orientation.applyToLocalX(localX, localZ, width);
                    assertEquals(landed, table[localZ * width + localX],
                            orientation + " at (" + localX + ", " + localZ + ") on width " + width
                                    + " lands apart from its pair");
                }
            }
        }
    }

    @Nested
    class States {
        @Test
        void theMirrorThenTheRotationTurnAFaceAsTheOrientationDoes() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                for (Direction face : HORIZONTAL) {
                    assertEquals(orientation.applyToFace(face),
                            orientation.stateRotation().rotate(orientation.stateMirror().mirror(face)),
                            orientation + " turns the " + face + " face apart from its mirror and rotation");
                }
            }
        }

        @Test
        void aFacingStateTurnsWithItsFace() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                for (Direction face : HORIZONTAL) {
                    BlockState state = Blocks.FURNACE.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, face);
                    assertEquals(orientation.applyToFace(face),
                            orientation.applyToState(state).getValue(BlockStateProperties.HORIZONTAL_FACING),
                            orientation + " turns a state facing " + face + " apart from its face");
                }
            }
        }

        @Test
        void theIdentityHandsTheStateBack() {
            BlockState state = Blocks.FURNACE.defaultBlockState();
            assertSame(state, FoldOrientation.IDENTITY.applyToState(state), "an upright state was rebuilt");
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
