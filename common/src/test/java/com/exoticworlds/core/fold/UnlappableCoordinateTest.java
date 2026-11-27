package com.exoticworlds.core.fold;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.deck.DeckGroupFold;
import com.exoticworlds.core.fold.loop.WorldLoopFold;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.world.FlatShape;
import com.exoticworlds.core.world.WorldLoopBounds.AxisBounds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class UnlappableCoordinateTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int SKEW_CHUNKS = 3;
    private static final int LOWER = MIN_CHUNK * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    private static final int WIDTH_BLOCKS = (MAX_CHUNK - MIN_CHUNK) * CoordinateConstants.CHUNK_WIDTH_BLOCKS;

    private static final double INSIDE = 5.5;
    private static final double SAMPLE_Y = 64.0;
    private static final double BOX_RADIUS_BLOCKS = 1.0;
    private static final double SEAT_TOLERANCE = 1e-6;
    private static final Vec3 ANCHOR = new Vec3(-20.5, 70.0, 33.5);

    private static final double[] NON_FINITE = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
    private static final double TOO_MANY_LAPS = LOWER + (double) WIDTH_BLOCKS * ((double) Integer.MAX_VALUE + 2.0);
    private static final double PAST_AN_INT_SHIFT_BLOCKS = 3.0e9;
    private static final double WITHIN_REACH_BLOCKS = 2.0e8 + 0.5;

    private static final WorldLoopBounds BOUNDS = new WorldLoopBounds(
            new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK), new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK));

    private record Subject(String name, WorldFold fold) {
    }

    private static final List<Subject> SUBJECTS = List.of(
            new Subject("torus", new WorldLoopFold(BOUNDS, ShapeOptions.DEFAULT)),
            new Subject("lattice torus",
                    new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS), ShapeOptions.DEFAULT)));

    private static List<Vec3> positions(double... coordinates) {
        List<Vec3> built = new ArrayList<>();
        for (double coordinate : coordinates) {
            built.add(new Vec3(coordinate, SAMPLE_Y, INSIDE));
            built.add(new Vec3(INSIDE, SAMPLE_Y, coordinate));
            built.add(new Vec3(coordinate, SAMPLE_Y, coordinate));
        }

        return built;
    }

    private static List<Vec3> unseatable() {
        List<Vec3> built = new ArrayList<>(positions(NON_FINITE));
        built.addAll(positions(TOO_MANY_LAPS, -TOO_MANY_LAPS, PAST_AN_INT_SHIFT_BLOCKS, -PAST_AN_INT_SHIFT_BLOCKS));
        return built;
    }

    private static void assertSameCoordinates(Vec3 expected, Vec3 actual, String what) {
        assertEquals(expected.x, actual.x, what + ": x");
        assertEquals(expected.y, actual.y, what + ": y");
        assertEquals(expected.z, actual.z, what + ": z");
    }

    private static void assertFoldLeavesIt(Subject subject, Vec3 pos) {
        WorldFold fold = subject.fold();
        String at = subject.name() + " at " + pos;
        assertSameCoordinates(pos, fold.fold(pos), at + ", fold");
        assertSameCoordinates(pos, fold.foldOriented(pos).value(), at + ", foldOriented");
        assertTrue(fold.foldTransformation(pos).isIdentity(), at + ", foldTransformation");
        assertTrue(fold.nearestCopyTransformation(ANCHOR, pos).isIdentity(), at + ", nearestCopyTransformation");
    }

    private static double plainSqrDistanceBlocks(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static AABB boxAround(Vec3 pos) {
        return new AABB(pos.x - BOX_RADIUS_BLOCKS, pos.y - BOX_RADIUS_BLOCKS, pos.z - BOX_RADIUS_BLOCKS,
                pos.x + BOX_RADIUS_BLOCKS, pos.y + BOX_RADIUS_BLOCKS, pos.z + BOX_RADIUS_BLOCKS);
    }

    private static void callEveryMember(WorldFold fold, Vec3 ref, Vec3 target) {
        fold.fold(target);
        fold.foldOriented(target);
        fold.foldTransformation(target);
        fold.nearestCopy(ref, target);
        fold.nearestCopyOriented(ref, target);
        fold.nearestCopyOriented(ref, boxAround(target));
        fold.nearestCopyTransformation(ref, target);
        fold.foldDelta(ref, target);
        fold.sqrDistanceBlocks(ref.x, ref.y, ref.z, target.x, target.y, target.z);
    }

    @Test
    void aNonFiniteCoordinateComesBackUnmoved() {
        for (Subject subject : SUBJECTS) {
            WorldFold fold = subject.fold();
            for (Vec3 pos : positions(NON_FINITE)) {
                String at = subject.name() + " at " + pos;
                assertFoldLeavesIt(subject, pos);
                assertSameCoordinates(pos, fold.nearestCopy(ANCHOR, pos), at + ", nearestCopy");
                assertSameCoordinates(pos, fold.nearestCopyOriented(ANCHOR, pos).value(),
                        at + ", nearestCopyOriented");
                assertSameCoordinates(pos.subtract(ANCHOR), fold.foldDelta(ANCHOR, pos), at + ", foldDelta");
                assertEquals(plainSqrDistanceBlocks(ANCHOR, pos),
                        fold.sqrDistanceBlocks(ANCHOR.x, ANCHOR.y, ANCHOR.z, pos.x, pos.y, pos.z),
                        at + ", sqrDistance");
            }
        }
    }

    @Test
    void aNonFiniteAnchorLeavesTheTargetUnmoved() {
        for (Subject subject : SUBJECTS) {
            WorldFold fold = subject.fold();
            for (Vec3 anchor : positions(NON_FINITE)) {
                String at = subject.name() + " around " + anchor;
                assertSameCoordinates(ANCHOR, fold.nearestCopy(anchor, ANCHOR), at + ", nearestCopy");
                assertSameCoordinates(ANCHOR, fold.nearestCopyOriented(anchor, ANCHOR).value(),
                        at + ", nearestCopyOriented");
            }
        }
    }

    @Test
    void aCoordinateTooManyLapsOutComesBackUnmoved() {
        for (Subject subject : SUBJECTS) {
            for (Vec3 pos : positions(TOO_MANY_LAPS, -TOO_MANY_LAPS)) {
                assertFoldLeavesIt(subject, pos);
            }
        }
    }

    @Test
    void noMemberThrowsOnACoordinateItCannotSeat() {
        for (Subject subject : SUBJECTS) {
            WorldFold fold = subject.fold();
            for (Vec3 pos : unseatable()) {
                assertDoesNotThrow(() -> callEveryMember(fold, ANCHOR, pos),
                        subject.name() + ", target " + pos);
                assertDoesNotThrow(() -> callEveryMember(fold, pos, ANCHOR),
                        subject.name() + ", anchor " + pos);
            }
        }
    }

    @Test
    void aCoordinateWithinReachStillSeats() {
        for (Subject subject : SUBJECTS) {
            WorldFold fold = subject.fold();
            for (Vec3 pos : positions(WITHIN_REACH_BLOCKS, -WITHIN_REACH_BLOCKS)) {
                String at = subject.name() + " at " + pos;
                Vec3 folded = fold.fold(pos);
                Vec3 seated = fold.foldTransformation(pos).apply(pos);
                assertFalse(fold.isOver(folded), at + ": fold left it at " + folded);
                assertEquals(folded.x, seated.x, SEAT_TOLERANCE, at + ": foldTransformation x");
                assertEquals(folded.z, seated.z, SEAT_TOLERANCE, at + ": foldTransformation z");
            }
        }
    }
}
