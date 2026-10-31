package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.FlatShape;
import com.exoticworlds.core.ShapeFixtures;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopBounds;
import com.exoticworlds.core.WorldLoopBounds.AxisBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

class NearestCopyTest {
    private static final int MIN_CHUNK = -8;
    private static final int MAX_CHUNK = 8;
    private static final int WIDTH_IN_CHUNKS = MAX_CHUNK - MIN_CHUNK;
    private static final int UPPER = MAX_CHUNK * CoordinateConstants.CHUNK_WIDTH;
    private static final int WIDTH = WIDTH_IN_CHUNKS * CoordinateConstants.CHUNK_WIDTH;

    private static final int BEFORE_SEAM = 8;
    private static final int HEIGHT = 64;
    private static final int DEPTH = 4;

    private static final WorldLoopBounds LOOPS_ON_X =
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold CYLINDER = WorldFolds.of(ShapeFixtures.cylinder(LOOPS_ON_X));

    private static final Vec3 TARGET = new Vec3(UPPER - BEFORE_SEAM, HEIGHT, DEPTH);
    private static final Vec3 ANCHOR_BESIDE_THE_TARGET = new Vec3(UPPER - BEFORE_SEAM / 2.0, HEIGHT, DEPTH);
    private static final Vec3 ANCHOR_ACROSS_THE_SEAM = new Vec3(BEFORE_SEAM - UPPER, HEIGHT, DEPTH);

    private static final BlockPos TARGET_BLOCK = BlockPos.containing(TARGET);
    private static final BlockPos ANCHOR_BLOCK_BESIDE_THE_TARGET = BlockPos.containing(ANCHOR_BESIDE_THE_TARGET);
    private static final BlockPos ANCHOR_BLOCK_ACROSS_THE_SEAM = BlockPos.containing(ANCHOR_ACROSS_THE_SEAM);

    private static final WorldLoopBounds TORUS_BOUNDS =
            ShapeFixtures.loopedBounds(MIN_CHUNK, MAX_CHUNK, MIN_CHUNK, MAX_CHUNK);
    private static final int SKEW_CHUNKS = 3;
    private static final List<WorldFold> TORI = List.of(
            WorldFolds.of(ShapeFixtures.torus(TORUS_BOUNDS)),
            WorldFolds.of(FlatShape.latticeTorus(TORUS_BOUNDS, SKEW_CHUNKS)));
    private static final double TORUS_SPAN = 3.0 * WIDTH;
    private static final long SEED = 0x1352L;
    private static final int SAMPLES = 2000;
    private static final int ROUNDING_ULPS = 2;

    private static final ChunkPos TARGET_CHUNK = new ChunkPos(MAX_CHUNK - 1, 0);
    private static final ChunkPos ANCHOR_CHUNK_BESIDE_THE_TARGET = new ChunkPos(MAX_CHUNK - 2, 0);
    private static final ChunkPos ANCHOR_CHUNK_ACROSS_THE_SEAM = new ChunkPos(MIN_CHUNK + 1, 0);

    @Test
    void towardANullFoldHandsTheArgumentPointBack() {
        assertSame(TARGET, NearestCopy.toward(null, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertSame(TARGET_BLOCK, NearestCopy.toward(null, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
        assertSame(TARGET_CHUNK, NearestCopy.toward(null, ANCHOR_CHUNK_ACROSS_THE_SEAM, TARGET_CHUNK));
    }

    @Test
    void towardAFoldThatWrapsNothingHandsTheArgumentPointBack() {
        assertSame(TARGET, NearestCopy.toward(WorldFolds.NOOP, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertSame(TARGET_BLOCK, NearestCopy.toward(WorldFolds.NOOP, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
        assertSame(TARGET_CHUNK, NearestCopy.toward(WorldFolds.NOOP, ANCHOR_CHUNK_ACROSS_THE_SEAM, TARGET_CHUNK));
    }

    @Test
    void towardAnAnchorBesideTheTargetHandsTheArgumentPointBack() {
        assertSame(TARGET, NearestCopy.toward(CYLINDER, ANCHOR_BESIDE_THE_TARGET, TARGET));
        assertSame(TARGET_BLOCK, NearestCopy.toward(CYLINDER, ANCHOR_BLOCK_BESIDE_THE_TARGET, TARGET_BLOCK));
        assertSame(TARGET_CHUNK, NearestCopy.toward(CYLINDER, ANCHOR_CHUNK_BESIDE_THE_TARGET, TARGET_CHUNK));
    }

    @Test
    void towardAnAnchorAcrossTheSeamSeatsThePointInTheLappedCopy() {
        assertEquals(TARGET.subtract(WIDTH, 0.0, 0.0),
                NearestCopy.toward(CYLINDER, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertEquals(TARGET_BLOCK.offset(-WIDTH, 0, 0),
                NearestCopy.toward(CYLINDER, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
        assertEquals(new ChunkPos(TARGET_CHUNK.x() - WIDTH_IN_CHUNKS, TARGET_CHUNK.z()),
                NearestCopy.toward(CYLINDER, ANCHOR_CHUNK_ACROSS_THE_SEAM, TARGET_CHUNK));
    }

    @Test
    void theTransformationTowardANullFoldIsTheIdentityItself() {
        assertSame(DeckTransformation.IDENTITY,
                NearestCopy.transformationToward(null, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertSame(DeckTransformation.IDENTITY,
                NearestCopy.transformationToward(null, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
    }

    @Test
    void theTransformationTowardAFoldThatWrapsNothingIsTheIdentityItself() {
        assertSame(DeckTransformation.IDENTITY,
                NearestCopy.transformationToward(WorldFolds.NOOP, ANCHOR_ACROSS_THE_SEAM, TARGET));
        assertSame(DeckTransformation.IDENTITY,
                NearestCopy.transformationToward(WorldFolds.NOOP, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK));
    }

    @Test
    void theTransformationTowardAnAnchorBesideTheTargetIsTheIdentityItself() {
        assertSame(DeckTransformation.IDENTITY,
                NearestCopy.transformationToward(CYLINDER, ANCHOR_BESIDE_THE_TARGET, TARGET));
        assertSame(DeckTransformation.IDENTITY,
                NearestCopy.transformationToward(CYLINDER, ANCHOR_BLOCK_BESIDE_THE_TARGET, TARGET_BLOCK));
    }

    @Test
    void theTransformationTowardAnAnchorAcrossTheSeamSeatsThePointInTheLappedCopy() {
        assertEquals(TARGET.subtract(WIDTH, 0.0, 0.0),
                NearestCopy.transformationToward(CYLINDER, ANCHOR_ACROSS_THE_SEAM, TARGET).apply(TARGET));
        assertEquals(TARGET_BLOCK.offset(-WIDTH, 0, 0),
                NearestCopy.transformationToward(CYLINDER, ANCHOR_BLOCK_ACROSS_THE_SEAM, TARGET_BLOCK)
                        .apply(TARGET_BLOCK));
    }

    @Test
    void towardANullFoldHandsTheCoordinateBack() {
        assertEquals(TARGET.x, NearestCopy.toward(null, Direction.Axis.X, ANCHOR_ACROSS_THE_SEAM, TARGET));
    }

    @Test
    void towardAFoldThatWrapsNothingHandsTheCoordinateBack() {
        assertEquals(TARGET.x,
                NearestCopy.toward(WorldFolds.NOOP, Direction.Axis.X, ANCHOR_ACROSS_THE_SEAM, TARGET));
    }

    @Test
    void towardAnAnchorAcrossTheSeamSeatsTheCoordinateInTheLappedCopy() {
        assertEquals(TARGET.x - WIDTH,
                NearestCopy.toward(CYLINDER, Direction.Axis.X, ANCHOR_ACROSS_THE_SEAM, TARGET));
    }

    @Test
    void theOperandFormsTowardANullFoldHandTheArgumentBack() {
        Vec3i target = new Vec3i(TARGET_BLOCK.getX(), TARGET_BLOCK.getY(), TARGET_BLOCK.getZ());
        Position position = TARGET;
        assertSame(target, NearestCopy.toward(null, ANCHOR_BLOCK_ACROSS_THE_SEAM, target));
        assertSame(position, NearestCopy.toward(null, ANCHOR_ACROSS_THE_SEAM, position));
        assertSame(position, NearestCopy.towardCenterOf(null, ANCHOR_BLOCK_ACROSS_THE_SEAM, position));
    }

    @Test
    void theOperandFormsTowardAnAnchorBesideTheTargetHandTheArgumentBack() {
        Vec3i target = TARGET_BLOCK;
        Position position = TARGET;
        assertSame(target, NearestCopy.toward(CYLINDER, ANCHOR_BLOCK_BESIDE_THE_TARGET, target));
        assertSame(position, NearestCopy.toward(CYLINDER, ANCHOR_BESIDE_THE_TARGET, position));
        assertSame(position, NearestCopy.towardCenterOf(CYLINDER, ANCHOR_BLOCK_BESIDE_THE_TARGET, position));
    }

    @Test
    void vanillaDistanceOnTheSeatedOperandIsTheDistanceThroughTheSeam() {
        Random random = new Random(SEED);
        for (WorldFold fold : TORI) {
            for (int sample = 0; sample < SAMPLES; sample++) {
                Vec3 from = randomPoint(random);
                Vec3 to = randomPoint(random);
                BlockPos fromBlock = BlockPos.containing(from);
                Vec3i toBlock = new Vec3i(Mth.floor(to.x), Mth.floor(to.y), Mth.floor(to.z));
                Vec3 center = Vec3.atCenterOf(fromBlock);

                assertSameDistance(fold.sqrDistance(fromBlock.getX(), fromBlock.getY(), fromBlock.getZ(),
                                toBlock.getX(), toBlock.getY(), toBlock.getZ()),
                        fromBlock.distSqr(NearestCopy.toward(fold, fromBlock, toBlock)));
                assertSameDistance(fold.sqrDistance(center.x, center.y, center.z, to.x, to.y, to.z),
                        fromBlock.distToCenterSqr(NearestCopy.towardCenterOf(fold, fromBlock, (Position) to)));
                Position seated = NearestCopy.toward(fold, from, (Position) to);
                assertSameDistance(fold.sqrDistance(from.x, from.y, from.z, to.x, to.y, to.z),
                        from.distanceToSqr(seated.x(), seated.y(), seated.z()));
            }
        }
    }

    private static void assertSameDistance(double expected, double actual) {
        assertEquals(expected, actual, ROUNDING_ULPS * Math.ulp(expected));
    }

    private static Vec3 randomPoint(Random random) {
        return new Vec3(TORUS_SPAN * (random.nextDouble() - 0.5), HEIGHT * random.nextDouble(),
                TORUS_SPAN * (random.nextDouble() - 0.5));
    }
}
