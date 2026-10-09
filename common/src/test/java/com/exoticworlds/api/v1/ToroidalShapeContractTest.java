package com.exoticworlds.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape.Orientation;
import com.exoticworlds.api.v1.ToroidalShape.Oriented;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class ToroidalShapeContractTest {
    private static final int UNIT = 16;
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH = (MAX_CHUNK - MIN_CHUNK) * UNIT;
    private static final int LOWER = MIN_CHUNK * UNIT;
    private static final int LATTICE_SKEW_CHUNKS = 3;

    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK);
    private static final WorldLoopBounds BOTH = new WorldLoopBounds(LOOPED, LOOPED);
    private static final WorldLoopBounds X_ONLY = new WorldLoopBounds(LOOPED, AxisBounds.Unbounded.INSTANCE);

    private static ToroidalShape torus() {
        return TestShapes.of(WorldFolds.of(FlatShape.torus(BOTH)));
    }

    private static ToroidalShape mobius() {
        return TestShapes.of(
                new DeckGroupFold(FlatShape.mirrored(X_ONLY, Direction.Axis.Z, 0)));
    }

    private static ToroidalShape klein() {
        return TestShapes.of(
                new DeckGroupFold(FlatShape.mirrored(BOTH, Direction.Axis.Z, 3)));
    }

    private static ToroidalShape latticeTorus() {
        return TestShapes.of(new DeckGroupFold(FlatShape.latticeTorus(BOTH, LATTICE_SKEW_CHUNKS)));
    }

    @Nested
    class TheTorusIsUnchanged {
        @Test
        void itDecomposesPerAxisAndKeepsLocalIndices() {
            ToroidalShape shape = torus();
            assertTrue(shape.decomposesPerAxis(), "the torus stopped decomposing per axis");
            assertTrue(shape.preservesLocalIndices(), "the torus stopped preserving local indices");
            assertTrue(shape.loops(Direction.Axis.X), "the torus stopped looping x");
            assertFalse(shape.loops(Direction.Axis.Y), "y loops");
            assertEquals(MIN_CHUNK, shape.minChunk(Direction.Axis.X), "the first chunk moved");
            assertEquals(WIDTH, shape.widthBlocks(Direction.Axis.Z), "the world width moved");
        }

        @Test
        void everyFoldStillReportsTheIdentity() {
            ToroidalShape shape = torus();
            Random random = new Random(0x7150L);
            for (int sample = 0; sample < 200; sample++) {
                int x = LOWER - 3 * WIDTH + random.nextInt(6 * WIDTH);
                int z = LOWER - 3 * WIDTH + random.nextInt(6 * WIDTH);
                Oriented<BlockPos> folded = shape.foldOriented(new BlockPos(x, 64, z));
                assertTrue(folded.isIdentity(), "an unmirrored fold reported a flip");
                assertEquals(shape.fold(new BlockPos(x, 64, z)), folded.value(),
                        "the oriented fold disagrees with the plain one");
            }
        }

        @Test
        void theOldPerAxisMembersStillAnswer() {
            ToroidalShape shape = torus();
            assertEquals(LOWER, shape.foldBlock(Direction.Axis.X, LOWER + WIDTH), "the per-axis block fold moved");
            assertEquals(MIN_CHUNK, shape.foldChunk(Direction.Axis.Z, MAX_CHUNK), "the per-axis chunk fold moved");
            assertEquals(5.5, shape.foldCoord(Direction.Axis.Y, 5.5), "y stopped passing through");
            assertEquals(1.5, shape.nearestCoord(Direction.Axis.Y, 900.0, 1.5), "y stopped passing through");
        }
    }

    @Nested
    class ACoupledShapeClosesThePerAxisDoor {
        @Test
        void theSkewedLatticeAndTheBottleRefuseThePerAxisMembers() {
            for (ToroidalShape shape : new ToroidalShape[] {latticeTorus(), klein(), mobius()}) {
                assertFalse(shape.decomposesPerAxis(), "a coupled shape claims to decompose per axis");
                assertThrows(IllegalStateException.class, () -> shape.foldBlock(Direction.Axis.X, 900),
                        "the per-axis block fold answered on a coupled shape");
                assertThrows(IllegalStateException.class, () -> shape.foldChunk(Direction.Axis.Z, 90),
                        "the per-axis chunk fold answered on a coupled shape");
                assertThrows(IllegalStateException.class, () -> shape.foldCoord(Direction.Axis.X, 900.0),
                        "the per-axis coordinate fold answered on a coupled shape");
                assertThrows(IllegalStateException.class,
                        () -> shape.nearestCoord(Direction.Axis.Z, 0.0, 900.0),
                        "the per-axis nearest coordinate answered on a coupled shape");
            }
        }

        @Test
        void theWholePositionFoldsStillAnswerThere() {
            for (ToroidalShape shape : new ToroidalShape[] {latticeTorus(), klein(), mobius()}) {
                BlockPos folded = shape.fold(new BlockPos(LOWER + 3 * WIDTH, 64, LOWER + 5));
                assertTrue(folded.getX() >= LOWER && folded.getX() < LOWER + WIDTH,
                        "the whole-position fold left the world on x");
            }
        }

        @Test
        void yStillPassesThroughEvenThere() {
            ToroidalShape shape = klein();
            assertEquals(5.5, shape.foldCoord(Direction.Axis.Y, 5.5), "y stopped passing through on a bottle");
            assertEquals(7, shape.foldBlock(Direction.Axis.Y, 7), "y stopped passing through on a bottle");
            assertFalse(shape.loops(Direction.Axis.Y), "y loops on a bottle");
        }

        @Test
        void theExtentsStillAnswerBecauseTheDomainIsStillARectangle() {
            ToroidalShape shape = latticeTorus();
            assertEquals(MIN_CHUNK, shape.minChunk(Direction.Axis.X), "a coupled shape lost its x extent");
            assertEquals(WIDTH, shape.widthBlocks(Direction.Axis.Z), "a coupled shape lost its z width");
            assertThrows(IllegalArgumentException.class, () -> shape.minChunk(Direction.Axis.Y),
                    "y reported an extent");
        }
    }

    @Nested
    class SeatingABox {
        @Test
        void aBoxAWorldAwayComesBackBesideTheReference() {
            ToroidalShape shape = torus();
            AABB seated = shape.nearestCopy(new Vec3(LOWER + WIDTH - 7.5, 64.0, 0.5),
                    new AABB(LOWER + 6.0, 60.0, -1.0, LOWER + 10.0, 62.0, 1.0));

            assertEquals(new AABB(LOWER + WIDTH + 6.0, 60.0, -1.0, LOWER + WIDTH + 10.0, 62.0, 1.0), seated,
                    "the box was not carried into the copy beside the reference");
        }

        @Test
        void aBoxAlreadyNearestIsHandedBack() {
            ToroidalShape shape = torus();
            AABB box = new AABB(LOWER + 6.0, 60.0, -1.0, LOWER + 10.0, 62.0, 1.0);

            assertSame(box, shape.nearestCopy(new Vec3(LOWER + 8.0, 64.0, 0.5), box),
                    "a box that moved nothing was rebuilt");
        }

        @Test
        void aBoxStraddlingTheSeamStaysWhole() {
            ToroidalShape shape = torus();
            AABB box = new AABB(LOWER + WIDTH - 2.0, 60.0, -1.0, LOWER + WIDTH + 2.0, 62.0, 1.0);
            AABB seated = shape.nearestCopy(new Vec3(LOWER + WIDTH - 4.0, 64.0, 0.5), box);

            assertSame(box, seated, "a box across the seam was cut into the pieces the world would make of it");
        }

        @Test
        void aBoxCarriedAcrossAMirroredSeamKeepsItsSizeAndReportsTheFlip() {
            ToroidalShape shape = mobius();
            Vec3 ref = new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5);
            AABB box = new AABB(LOWER + 0.5, 60.0, -102.5, LOWER + 2.5, 62.0, -98.5);
            Oriented<AABB> seated = shape.nearestCopyOriented(ref, box);

            assertTrue(seated.orientation().flipsZ(), "the flipped copy did not report its flip");
            assertEquals(shape.nearestCopy(ref, box), seated.value(),
                    "the oriented seating disagrees with the plain one");
            assertEquals(box.getXsize(), seated.value().getXsize(), "the box changed width across the seam");
            assertEquals(box.getZsize(), seated.value().getZsize(), "the box changed depth across the seam");
            assertEquals(box.getYsize(), seated.value().getYsize(), "the box changed height across the seam");
        }
    }

    @Nested
    class ShiftingARigidGroup {
        @Test
        void oneShiftHoldsTogetherAGroupThatSeatingApartWouldTear() {
            ToroidalShape shape = torus();
            Vec3 ref = new Vec3(LOWER + WIDTH - 7.5, 64.0, 0.5);
            Vec3 anchor = new Vec3(-6.0, 64.0, 0.5);
            Vec3 member = new Vec3(-10.0, 64.0, 0.5);
            double apart = anchor.x - member.x;

            assertNotEquals(apart, shape.nearestCopy(ref, anchor).x - shape.nearestCopy(ref, member).x,
                    "the pair no longer straddles half a world width, so it proves nothing");

            SeamShift shift = shape.shiftToNearestCopy(ref, anchor);
            assertEquals(apart, shift.apply(anchor).x - shift.apply(member).x, "one shift tore the group");
            assertEquals(shape.nearestCopy(ref, anchor), shift.apply(anchor),
                    "the shift lands the anchor somewhere else than seating it does");
        }

        @Test
        void aGroupAlreadyInTheRightCopyGetsTheIdentity() {
            ToroidalShape shape = torus();
            Vec3 member = new Vec3(6.0, 64.0, 0.5);
            SeamShift shift = shape.shiftToNearestCopy(new Vec3(0.0, 64.0, 0.5), new Vec3(2.0, 64.0, 0.5));

            assertTrue(shift.isIdentity(), "a group already in the right copy was moved");
            assertTrue(shift.orientation().isIdentity(), "an identity shift reported a turn");
            assertSame(member, shift.apply(member), "an identity shift rebuilt its argument");
        }

        @Test
        void theBlockGridFormAgreesWithSeatingTheAnchor() {
            ToroidalShape shape = torus();
            BlockPos ref = new BlockPos(LOWER + WIDTH - 8, 64, 0);
            BlockPos anchor = new BlockPos(-6, 64, 0);
            BlockPos member = new BlockPos(-10, 64, 0);
            SeamShift shift = shape.shiftToNearestCopy(ref, anchor);

            assertEquals(shape.nearestCopy(ref, anchor), shift.apply(anchor),
                    "the shift lands the anchor somewhere else than seating it does");
            assertEquals(anchor.getX() - member.getX(), shift.apply(anchor).getX() - shift.apply(member).getX(),
                    "one shift tore the group on the block grid");
        }

        @Test
        void aShiftAcrossAMirroredSeamCarriesItsTurnAndKeepsABoxRigid() {
            ToroidalShape shape = mobius();
            Vec3 ref = new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5);
            Vec3 anchor = new Vec3(LOWER + 1.5, 64.0, -100.5);
            SeamShift shift = shape.shiftToNearestCopy(ref, anchor);

            assertTrue(shift.orientation().flipsZ(), "the shift across a mirrored seam reported no turn");
            assertEquals(shape.nearestCopy(ref, anchor), shift.apply(anchor),
                    "the shift lands the anchor somewhere else than seating it does");

            AABB box = new AABB(LOWER + 0.5, 60.0, -102.5, LOWER + 2.5, 62.0, -98.5);
            AABB moved = shift.apply(box);
            assertEquals(box.getXsize(), moved.getXsize(), "the box changed width under the shift");
            assertEquals(box.getZsize(), moved.getZsize(), "the box changed depth under the shift");
        }
    }

    @Nested
    class Mirroring {
        @Test
        void aLapAcrossAMirroredSeamIsReported() {
            ToroidalShape shape = mobius();
            int inside = LOWER + WIDTH / 2;
            Oriented<BlockPos> oneLap = shape.foldOriented(new BlockPos(inside + WIDTH, 64, inside));
            assertFalse(oneLap.isIdentity(), "one lap across a mirrored seam reported no flip");
            assertTrue(oneLap.orientation().flipsZ(), "the flip is not on z");
            assertFalse(oneLap.orientation().flipsX(), "the glide axis was flipped");
            assertFalse(oneLap.orientation().preservesHandedness(), "a single mirror kept handedness");

            Oriented<BlockPos> twoLaps = shape.foldOriented(new BlockPos(inside + 2 * WIDTH, 64, inside));
            assertTrue(twoLaps.isIdentity(), "two laps did not come back upright");
        }

        @Test
        void theNearestCopyAcrossAMirroredSeamCarriesItsOrientation() {
            ToroidalShape shape = mobius();
            Oriented<Vec3> nearest = shape.nearestCopyOriented(
                    new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5), new Vec3(LOWER + 1.5, 64.0, -100.5));
            assertTrue(nearest.orientation().flipsZ(), "the flipped copy did not report its flip");
            assertEquals(shape.nearestCopy(new Vec3(LOWER + WIDTH - 1.5, 64.0, 100.5),
                            new Vec3(LOWER + 1.5, 64.0, -100.5)),
                    nearest.value(), "the oriented nearest copy disagrees with the plain one");
        }

        @Test
        void aDeltaCarriedAcrossTheSeamIsTurnedWithIt() {
            Orientation flipped = new Orientation(false, true);
            assertEquals(new Vec3(1.0, 2.0, -3.0), flipped.applyToDelta(new Vec3(1.0, 2.0, 3.0)),
                    "a mirrored delta was not turned");

            Vec3 delta = new Vec3(1.0, 2.0, 3.0);
            assertSame(delta, Orientation.IDENTITY.applyToDelta(delta), "an upright delta was rebuilt");
            assertTrue(Orientation.IDENTITY.isIdentity(), "the identity orientation is not identity");
            assertTrue(new Orientation(true, true).preservesHandedness(), "a half turn reverses handedness");
        }

        @Test
        void onlyAMirroredShapeLosesItsLocalIndices() {
            assertTrue(torus().preservesLocalIndices(), "the torus lost its local indices");
            assertTrue(latticeTorus().preservesLocalIndices(), "a skew lost the local indices");
            assertFalse(mobius().preservesLocalIndices(), "a band kept its local indices");
            assertFalse(klein().preservesLocalIndices(), "a bottle kept its local indices");
        }
    }

    @Nested
    class ListingCopies {
        private static final int SKEW = LATTICE_SKEW_CHUNKS * UNIT;
        private static final int UPPER = LOWER + WIDTH;
        private static final int ORBIT_REACH = 8;
        private static final double Y = 64.0;

        private static final AABB INSIDE = new AABB(LOWER + 10, 0, LOWER + 10, LOWER + 20, 0, LOWER + 20);
        private static final AABB ACROSS_THE_Z_SEAM = new AABB(0, 0, UPPER - 8, 8, 0, UPPER + 8);
        private static final AABB WIDER_THAN_THE_WORLD =
                new AABB(LOWER - WIDTH / 2.0, 0, LOWER - WIDTH / 2.0, UPPER + WIDTH / 2.0, 0, UPPER + WIDTH / 2.0);
        private static final AABB LAPS_OUT = new AABB(
                LOWER + 5 * WIDTH - 3, 0, LOWER - 4 * WIDTH - 3, LOWER + 5 * WIDTH + 3, 0, LOWER - 4 * WIDTH + 3);
        private static final AABB ONE_WORLD_PAST_THE_EDGE = new AABB(LOWER, 0, LOWER, UPPER + WIDTH, 0, UPPER);

        private static ToroidalShape cylinder() {
            return TestShapes.of(WorldFolds.of(FlatShape.cylinder(X_ONLY)));
        }

        @Test
        void everyCopyThatMeetsTheRectangleIsListedOnceWithTheWorldFirst() {
            for (AABB box : List.of(INSIDE, ACROSS_THE_Z_SEAM, WIDER_THAN_THE_WORLD, LAPS_OUT)) {
                assertOrbit(torus(), 0, true, box, "torus");
                assertOrbit(cylinder(), 0, false, box, "cylinder");
                assertOrbit(latticeTorus(), SKEW, true, box, "lattice torus");
            }
        }

        @Test
        void aRectangleInsideTheWorldMeetsTheWorldAlone() {
            for (ToroidalShape shape : List.of(torus(), cylinder(), latticeTorus())) {
                List<SeamShift> copies = shape.copiesMeeting(INSIDE);
                assertEquals(1, copies.size(), "a rectangle inside the world met " + copies.size() + " copies");
                assertTrue(copies.getFirst().isIdentity(), "the one copy met is not the world itself");
            }
        }

        @Test
        void aSkewedCopyOneRowUpSitsTheSkewOver() {
            List<SeamShift> copies = latticeTorus().copiesMeeting(ACROSS_THE_Z_SEAM);
            assertEquals(List.of(BlockPos.ZERO, new BlockPos(SKEW, 0, WIDTH)), offsetsOf(copies),
                    "the copy across the z seam is not the skew over");
        }

        @Test
        void anEmptyRectangleMeetsNothing() {
            AABB flat = new AABB(LOWER + 10, 0, LOWER + 10, LOWER + 10, 0, LOWER + 20);
            for (ToroidalShape shape : List.of(torus(), cylinder(), latticeTorus())) {
                assertTrue(shape.copiesMeeting(flat).isEmpty(), "an empty rectangle met a copy");
                assertTrue(shape.copiesInside(flat, new Vec3(LOWER + 10, Y, LOWER + 15)).isEmpty(),
                        "a position was found inside an empty rectangle");
            }
        }

        @Test
        void aPositionOnTheMinEdgeIsInsideAndOneOnTheMaxEdgeIsNot() {
            ToroidalShape shape = torus();
            AABB inland = new AABB(LOWER, 0, LOWER, 10, 0, UPPER);
            Vec3 onMin = new Vec3(LOWER, Y, 0.5);
            assertEquals(List.of(onMin), valuesOf(shape.copiesInside(inland, onMin)), "a position on the min edge");
            assertTrue(shape.copiesInside(inland, new Vec3(10, Y, 0.5)).isEmpty(), "a position on the max edge");
            assertTrue(shape.copiesInside(inland, new BlockPos(10, 64, 0)).isEmpty(), "a block on the max edge");

            assertEquals(List.of(new Vec3(LOWER, Y, 0.5), new Vec3(UPPER, Y, 0.5)),
                    valuesOf(shape.copiesInside(ONE_WORLD_PAST_THE_EDGE, new Vec3(UPPER, Y, 0.5))),
                    "the copies of a position on both edges");
            assertEquals(List.of(new BlockPos(LOWER, 64, 0), new BlockPos(UPPER, 64, 0)),
                    valuesOf(shape.copiesInside(ONE_WORLD_PAST_THE_EDGE, new BlockPos(UPPER + WIDTH, 64, 0))),
                    "the copies of a block on both edges");
        }

        @Test
        void aCopyAcrossTheSkewedSeamLandsTheSkewOver() {
            List<Oriented<Vec3>> copies = latticeTorus().copiesInside(
                    new AABB(-64, 0, UPPER - 8, 64, 0, UPPER + 8), new Vec3(0.5, Y, LOWER + 1.5));
            assertEquals(List.of(new Vec3(SKEW + 0.5, Y, UPPER + 1.5)), valuesOf(copies),
                    "the copy of a position past the z seam");
            assertTrue(copies.getFirst().isIdentity(), "an unmirrored copy reported a flip");
        }

        @Test
        void aCopyWherePositionAlreadyIsHandsTheArgumentBack() {
            Vec3 inside = new Vec3(LOWER + 15.5, Y, LOWER + 15.5);
            Vec3 lapOut = new Vec3(UPPER + 5.5, Y, LOWER + 15.5);
            BlockPos blockLapOut = new BlockPos(UPPER + 5, 64, LOWER + 15);
            AABB pastTheSeam = new AABB(UPPER, 0, LOWER, UPPER + 16, 0, UPPER);
            ToroidalShape shape = torus();
            assertSame(inside, shape.copiesInside(INSIDE, inside).getFirst().value(), "a position inside was rebuilt");
            assertSame(lapOut, shape.copiesInside(pastTheSeam, lapOut).getFirst().value(),
                    "a position a lap out was rebuilt where its copy is itself");
            assertSame(blockLapOut, shape.copiesInside(pastTheSeam, blockLapOut).getFirst().value(),
                    "a block a lap out was rebuilt where its copy is itself");
        }

        @Test
        void aCopyAcrossAMirroredSeamReportsTheFlip() {
            List<Oriented<Vec3>> copies = mobius().copiesInside(
                    new AABB(UPPER, 0, -32, UPPER + 16, 0, 32), new Vec3(LOWER + 5.5, Y, 10.5));
            assertEquals(List.of(new Vec3(UPPER + 5.5, Y, -10.5)), valuesOf(copies),
                    "the copy across the mirrored seam");
            assertTrue(copies.getFirst().orientation().flipsZ(), "the mirrored copy did not report its flip");
        }

        private static void assertOrbit(ToroidalShape shape, int skew, boolean zLoops, AABB box, String name) {
            List<SeamShift> copies = shape.copiesMeeting(box);
            List<BlockPos> offsets = offsetsOf(copies);
            assertEquals(expectedOffsets(skew, zLoops, box), new HashSet<>(offsets),
                    name + ": copiesMeeting(" + box + ") disagrees with the orbit");
            assertEquals(new HashSet<>(offsets).size(), offsets.size(), name + ": a copy listed twice");
            for (int index = 1; index < copies.size(); index++) {
                assertFalse(copies.get(index).isIdentity(), name + ": the world itself is not listed first");
            }
        }

        private static Set<BlockPos> expectedOffsets(int skew, boolean zLoops, AABB box) {
            Set<BlockPos> expected = new HashSet<>();
            int rows = zLoops ? ORBIT_REACH : 0;
            for (int row = -rows; row <= rows; row++) {
                for (int column = -ORBIT_REACH; column <= ORBIT_REACH; column++) {
                    int dx = column * WIDTH + row * skew;
                    int dz = row * WIDTH;
                    boolean meetsX = LOWER + dx < box.maxX && box.minX < UPPER + dx;
                    boolean meetsZ = !zLoops || (LOWER + dz < box.maxZ && box.minZ < UPPER + dz);
                    if (meetsX && meetsZ) {
                        expected.add(new BlockPos(dx, 0, dz));
                    }
                }
            }

            return expected;
        }

        private static List<BlockPos> offsetsOf(List<SeamShift> copies) {
            return copies.stream().map(copy -> copy.apply(BlockPos.ZERO)).toList();
        }

        private static <T> List<T> valuesOf(List<Oriented<T>> copies) {
            return copies.stream().map(Oriented::value).toList();
        }
    }
}
