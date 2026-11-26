package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;

class LightStepTest {
    private static final int FACE_WIDTH_BLOCKS = 4 * CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    private static final int Y = 70;
    private static final int LEVEL = 14;

    private static final SeamTransform CLOCKWISE_OVER_THE_EAST_EDGE =
            new SeamTransform(-1, 1, FACE_WIDTH_BLOCKS, -FACE_WIDTH_BLOCKS, true);
    private static final WorldFold TURNED = foldBy(CLOCKWISE_OVER_THE_EAST_EDGE);

    private static final Direction STEP = Direction.EAST;
    private static final BlockPos SOURCE = new BlockPos(FACE_WIDTH_BLOCKS - 1, Y, 5);
    private static final BlockPos STEPPED = SOURCE.relative(STEP);
    private static final BlockPos TARGET = CLOCKWISE_OVER_THE_EAST_EDGE.apply(STEPPED);
    private static final Direction BACK_AT_THE_SOURCE =
            directionBetween(TARGET, CLOCKWISE_OVER_THE_EAST_EDGE.apply(SOURCE));
    private static final FoldOrientation TURN = CLOCKWISE_OVER_THE_EAST_EDGE.getOrientation();

    private static final int TORUS_WIDTH_IN_CHUNKS = 64;
    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_IN_CHUNKS)));
    private static final BlockPos ONE_LAP_AWAY =
            new BlockPos(5 + TORUS_WIDTH_IN_CHUNKS * CoordinateConstants.CHUNK_WIDTH_BLOCKS, Y, 5);

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static WorldFold foldBy(SeamTransform move) {
        return (WorldFold) Proxy.newProxyInstance(
                WorldFold.class.getClassLoader(),
                new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isPreservingLocalIndices" -> false;
                    case "foldOriented" -> new WorldFold.Folded<>(
                            move.apply((BlockPos) arguments[0]), move.getOrientation());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static WorldFold unreadFold() {
        return (WorldFold) Proxy.newProxyInstance(
                WorldFold.class.getClassLoader(),
                new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> {
                    throw new AssertionError("the fold was asked for " + method.getName());
                });
    }

    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (Direction direction : Direction.values()) {
            if (from.relative(direction).equals(to)) {
                return direction;
            }
        }

        throw new IllegalArgumentException(from + " and " + to + " are not neighbors");
    }

    private static BlockState stairsFacing(Direction face) {
        return Blocks.STONE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, face);
    }

    private static Direction skipDirection() {
        return LightStep.inTargetFrame(TURN, STEP.getOpposite());
    }

    private static boolean occludes(BlockState targetState) {
        return Shapes.faceShapeOccludes(
                LightEngine.getOcclusionShape(AIR, STEP),
                LightEngine.getOcclusionShape(LightStep.inSourceFrame(TURN, targetState), STEP.getOpposite()));
    }

    private static void assertSpreadsEverywhereBut(long entry, Direction skipped, String what) {
        for (Direction direction : Direction.values()) {
            assertEquals(direction != skipped, LightEngine.QueueEntry.shouldPropagateInDirection(entry, direction),
                    what + " got the " + direction + " side wrong");
        }
    }

    @Nested
    class TheOrientationOfAStep {
        @Test
        void aStepOverATurnedEdgeTakesTheTurnOfItsFold() {
            assertEquals(TURN, LightStep.orientationOf(TURNED, STEPPED.asLong(), TARGET.asLong()),
                    "a step over a turned edge kept the frame it left");
        }

        @Test
        void aStepThatStaysWhereItLandedNeverReadsTheFold() {
            assertSame(FoldOrientation.IDENTITY,
                    LightStep.orientationOf(unreadFold(), SOURCE.asLong(), SOURCE.asLong()),
                    "a step inside the bounds was turned");
        }

        @Test
        void aStepOverATorusSeamIsUpright() {
            assertSame(FoldOrientation.IDENTITY,
                    LightStep.orientationOf(TORUS, ONE_LAP_AWAY.asLong(), TORUS.foldBlockNode(ONE_LAP_AWAY.asLong())),
                    "a torus step was turned");
        }
    }

    @Nested
    class TheQueueEntryAcrossATurnedEdge {
        @Test
        void anIncreaseSkipsTheSideTheLightCameFrom() {
            long entry = LightEngine.QueueEntry.increaseSkipOneDirection(LEVEL, false, skipDirection());

            assertSpreadsEverywhereBut(entry, BACK_AT_THE_SOURCE, "the increase");
        }

        @Test
        void aDecreaseSkipsTheSideTheLightCameFrom() {
            long entry = LightEngine.QueueEntry.decreaseSkipOneDirection(LEVEL, skipDirection());

            assertSpreadsEverywhereBut(entry, BACK_AT_THE_SOURCE, "the decrease");
        }

        @Test
        void aBrighterNeighborRelightsOnlyTheSideTheDecreaseCameFrom() {
            long entry = LightEngine.QueueEntry.increaseOnlyOneDirection(LEVEL, false, skipDirection());

            for (Direction direction : Direction.values()) {
                assertEquals(direction == BACK_AT_THE_SOURCE,
                        LightEngine.QueueEntry.shouldPropagateInDirection(entry, direction),
                        "the relight got the " + direction + " side wrong");
            }
        }

        @Test
        void aVerticalStepKeepsItsDirectionUnderEveryOrientation() {
            for (FoldOrientation orientation : FoldOrientation.values()) {
                assertEquals(Direction.DOWN, LightStep.inTargetFrame(orientation, Direction.DOWN),
                        orientation + " turned a step down");
            }
        }
    }

    @Nested
    class OcclusionAcrossATurnedEdge {
        @Test
        void aFullFaceWhereTheLightEntersBlocksIt() {
            assertTrue(occludes(stairsFacing(BACK_AT_THE_SOURCE)),
                    "stairs with their back to the light let it through");
        }

        @Test
        void aFullFaceOnTheSideTheStepLeftBlocksNothing() {
            assertFalse(occludes(stairsFacing(STEP.getOpposite())),
                    "stairs were judged on the face of the frame the step left");
        }

        @Test
        void anUprightStepReadsTheStateItWasHanded() {
            BlockState state = stairsFacing(Direction.WEST);

            assertSame(state, LightStep.inSourceFrame(FoldOrientation.IDENTITY, state), "an upright step rebuilt its state");
        }
    }
}
