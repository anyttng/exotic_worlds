package com.exoticworlds.api.v2.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.core.DeckGroupFold;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class WorldGeometryContractTest {
    private static final int UNIT = 16;
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH = (MAX_CHUNK - MIN_CHUNK) * UNIT;
    private static final int LOWER = MIN_CHUNK * UNIT;
    private static final int UPPER = LOWER + WIDTH;
    private static final int LATTICE_SKEW_CHUNKS = 3;
    private static final int SAMPLES = 200;
    private static final int LAPS_SAMPLED = 6;
    private static final Vec3 CARRIED_DELTA = new Vec3(1.0, 2.0, 3.0);

    private static final AxisBounds.Looped LOOPED = new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK);
    private static final WorldLoopBounds BOTH = new WorldLoopBounds(LOOPED, LOOPED);
    private static final WorldLoopBounds X_ONLY = new WorldLoopBounds(LOOPED, AxisBounds.Unbounded.INSTANCE);

    private static final LoopSpans X_SPANS = LoopSpans.of(Direction.Axis.X, MIN_CHUNK, MAX_CHUNK);
    private static final LoopSpans BOTH_SPANS = X_SPANS.and(Direction.Axis.Z, MIN_CHUNK, MAX_CHUNK);

    private static final AABB INSIDE = new AABB(LOWER + 10, 0, LOWER + 10, LOWER + 20, 8, LOWER + 20);
    private static final AABB ACROSS_THE_CORNER = new AABB(UPPER - 8, 0, UPPER - 6, UPPER + 8, 8, UPPER + 10);
    private static final AABB WIDER_THAN_THE_WORLD =
            new AABB(LOWER - WIDTH / 2.0, 0, LOWER - WIDTH / 2.0, UPPER + WIDTH / 2.0, 8, UPPER + WIDTH / 2.0);
    private static final List<AABB> BOXES = List.of(INSIDE, ACROSS_THE_CORNER, WIDER_THAN_THE_WORLD);

    private record Twins(String name, ToroidalShape v1, WorldGeometry v2, LoopSpans spans) {
        static Twins of(String name, WorldFold fold, LoopSpans spans) {
            return new Twins(name, fold.shapeView(), fold.geometryView(), spans);
        }
    }

    private static List<Twins> shapes() {
        return List.of(
                Twins.of("torus", WorldFolds.of(ShapeFixtures.torus(BOTH)), BOTH_SPANS),
                Twins.of("lattice torus",
                        new DeckGroupFold(FlatShape.latticeTorus(BOTH, LATTICE_SKEW_CHUNKS), GenerationOptions.DEFAULT),
                        BOTH_SPANS.withSkew(LATTICE_SKEW_CHUNKS)),
                Twins.of("cylinder", WorldFolds.of(ShapeFixtures.cylinder(X_ONLY)), X_SPANS));
    }

    @Test
    void theSpansAreTheOnesTheShapeWasDeclaredWith() {
        for (Twins twins : shapes()) {
            assertEquals(twins.spans(), twins.v2().spans(), twins.name() + ": the spans");
        }
    }

    @Test
    void theWholePositionMembersAnswerWhatV1Answers() {
        Random random = new Random(0x1667L);
        for (Twins twins : shapes()) {
            ToroidalShape v1 = twins.v1();
            WorldGeometry v2 = twins.v2();
            assertEquals(v1.preservesLocalIndices(), v2.preservesLocalIndices(), twins.name() + ": local indices");
            for (int sample = 0; sample < SAMPLES; sample++) {
                Vec3 ref = randomPosition(random);
                Vec3 target = randomPosition(random);
                BlockPos refBlock = BlockPos.containing(ref);
                BlockPos targetBlock = BlockPos.containing(target);
                AABB box = new AABB(target, target.add(3.0, 2.0, 5.0));
                String at = twins.name() + " at " + ref + " and " + target;

                assertEquals(v1.fold(target), v2.fold(target), at);
                assertEquals(v1.fold(targetBlock), v2.fold(targetBlock), at);
                assertEquals(v1.fold(ChunkPos.containing(targetBlock)), v2.fold(ChunkPos.containing(targetBlock)), at);
                assertEquals(v1.nearestCopy(ref, target), v2.nearestCopy(ref, target), at);
                assertEquals(v1.nearestCopy(refBlock, targetBlock), v2.nearestCopy(refBlock, targetBlock), at);
                assertEquals(v1.nearestCopy(ref, box), v2.nearestCopy(ref, box), at);
                assertEquals(v1.shortestDelta(ref, target), v2.shortestDelta(ref, target), at);
                assertEquals(v1.sqrDistance(ref, target), v2.sqrDistance(ref, target), at);
                assertEquals(v1.boxesOverlap(box, ACROSS_THE_CORNER), v2.boxesOverlap(box, ACROSS_THE_CORNER), at);
            }
        }
    }

    @Test
    void theOrientedMembersAnswerWhatV1Answers() {
        Random random = new Random(0x0715L);
        for (Twins twins : shapes()) {
            ToroidalShape v1 = twins.v1();
            WorldGeometry v2 = twins.v2();
            for (int sample = 0; sample < SAMPLES; sample++) {
                Vec3 ref = randomPosition(random);
                Vec3 target = randomPosition(random);
                BlockPos refBlock = BlockPos.containing(ref);
                BlockPos targetBlock = BlockPos.containing(target);
                AABB box = new AABB(target, target.add(3.0, 2.0, 5.0));
                String at = twins.name() + " at " + ref + " and " + target;

                assertTwin(v1.foldOriented(target), v2.foldOriented(target), at);
                assertTwin(v1.foldOriented(targetBlock), v2.foldOriented(targetBlock), at);
                assertTwin(v1.foldOriented(ChunkPos.containing(targetBlock)), v2.foldOriented(ChunkPos.containing(targetBlock)), at);
                assertTwin(v1.nearestCopyOriented(ref, target), v2.nearestCopyOriented(ref, target), at);
                assertTwin(v1.nearestCopyOriented(refBlock, targetBlock),
                        v2.nearestCopyOriented(refBlock, targetBlock), at);
                assertTwin(v1.nearestCopyOriented(ref, box), v2.nearestCopyOriented(ref, box), at);
            }

            for (AABB box : BOXES) {
                Vec3 pos = new Vec3(LOWER + 15.5, 64.0, LOWER + 15.5);
                String at = twins.name() + " over " + box;
                assertTwins(v1.split(box), v2.split(box), at);
                assertTwins(v1.copiesInside(box, pos), v2.copiesInside(box, pos), at);
                assertTwins(v1.copiesInside(box, BlockPos.containing(pos)),
                        v2.copiesInside(box, BlockPos.containing(pos)), at);
            }
        }
    }

    @Test
    void theShiftsAnswerWhatV1Answers() {
        Random random = new Random(0x5417L);
        for (Twins twins : shapes()) {
            ToroidalShape v1 = twins.v1();
            WorldGeometry v2 = twins.v2();
            for (int sample = 0; sample < SAMPLES; sample++) {
                Vec3 ref = randomPosition(random);
                Vec3 anchor = randomPosition(random);
                String at = twins.name() + " at " + ref + " and " + anchor;
                assertTwin(v1.shiftToNearestCopy(ref, anchor), v2.shiftToNearestCopy(ref, anchor), anchor, at);
                assertTwin(v1.shiftToNearestCopy(BlockPos.containing(ref), BlockPos.containing(anchor)),
                        v2.shiftToNearestCopy(BlockPos.containing(ref), BlockPos.containing(anchor)), anchor, at);
            }

            for (AABB box : BOXES) {
                List<com.exoticworlds.api.v1.SeamShift> expected = v1.copiesMeeting(box);
                List<SeamShift> actual = v2.copiesMeeting(box);
                String at = twins.name() + " over " + box;
                assertEquals(expected.size(), actual.size(), at + ": the number of copies");
                for (int index = 0; index < expected.size(); index++) {
                    assertTwin(expected.get(index), actual.get(index), Vec3.ZERO, at);
                }
            }
        }
    }

    @Test
    void aMemberThatMovedNothingHandsItsArgumentBack() {
        Vec3 pos = new Vec3(LOWER + 15.5, 64.0, LOWER + 15.5);
        BlockPos block = BlockPos.containing(pos);
        for (Twins twins : shapes()) {
            WorldGeometry geometry = twins.v2();
            assertSame(pos, geometry.fold(pos), twins.name() + ": a position inside was rebuilt");
            assertSame(block, geometry.fold(block), twins.name() + ": a block inside was rebuilt");
            assertSame(pos, geometry.nearestCopy(pos, pos), twins.name() + ": a nearest position was rebuilt");
            assertSame(INSIDE, geometry.nearestCopy(pos, INSIDE), twins.name() + ": a nearest box was rebuilt");
            assertSame(pos, geometry.foldOriented(pos).value(), twins.name() + ": an oriented fold rebuilt it");
            assertSame(INSIDE, geometry.split(INSIDE).getFirst().value(), twins.name() + ": the one piece");
            assertSame(pos, geometry.copiesInside(INSIDE, pos).getFirst().value(), twins.name() + ": the one copy");

            SeamShift shift = geometry.shiftToNearestCopy(pos, pos);
            assertTrue(shift.isIdentity(), twins.name() + ": a group in the right copy was moved");
            assertSame(pos, shift.apply(pos), twins.name() + ": an identity shift rebuilt its argument");
        }
    }

    private static <T> void assertTwins(List<ToroidalShape.Oriented<T>> expected, List<Oriented<T>> actual,
            String at) {
        assertEquals(expected.size(), actual.size(), at + ": the number of values");
        for (int index = 0; index < expected.size(); index++) {
            assertTwin(expected.get(index), actual.get(index), at);
        }
    }

    private static <T> void assertTwin(ToroidalShape.Oriented<T> expected, Oriented<T> actual, String at) {
        assertEquals(expected.value(), actual.value(), at);
        assertEquals(expected.isIdentity(), actual.isIdentity(), at);
        assertTwin(expected.orientation(), actual.orientation(), at);
    }

    private static void assertTwin(com.exoticworlds.api.v1.SeamShift expected, SeamShift actual, Vec3 member,
            String at) {
        BlockPos block = BlockPos.containing(member);
        AABB box = new AABB(member, member.add(3.0, 2.0, 5.0));
        assertEquals(expected.isIdentity(), actual.isIdentity(), at);
        assertTwin(expected.orientation(), actual.orientation(), at);
        assertEquals(expected.apply(member), actual.apply(member), at);
        assertEquals(expected.apply(block), actual.apply(block), at);
        assertEquals(expected.apply(ChunkPos.containing(block)), actual.apply(ChunkPos.containing(block)), at);
        assertEquals(expected.apply(box), actual.apply(box), at);
    }

    private static void assertTwin(ToroidalShape.Orientation expected, Orientation actual, String at) {
        assertEquals(expected.isIdentity(), actual.isIdentity(), at);
        assertEquals(expected.preservesHandedness(), actual.preservesHandedness(), at);
        assertEquals(expected.applyToDelta(CARRIED_DELTA), actual.applyToDelta(CARRIED_DELTA), at);
    }

    private static Vec3 randomPosition(Random random) {
        return new Vec3((random.nextDouble() - 0.5) * LAPS_SAMPLED * WIDTH, random.nextDouble() * 128,
                (random.nextDouble() - 0.5) * LAPS_SAMPLED * WIDTH);
    }
}
