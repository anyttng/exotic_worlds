package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.MIN_CHUNK_X;
import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.MIN_CHUNK_Z;
import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.carried;
import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.cuboidFold;
import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.edgeBlocks;
import static com.exoticworlds.core.fold.cuboid.CuboidFixtures.pointBy;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.NOISE_DATA;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.SEED;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.compile;
import static com.exoticworlds.engine.noise.DensityFunctionFixture.sample;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidEdge;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class CuboidShiftedNoiseTest {
    private static final int SAMPLES = 64;
    private static final int Y = 37;
    private static final double BLOCK_CENTER = 0.5;
    private static final double EDGE_STEP_ALLOWANCE = 2.0;
    private static final double MIN_WARPED_SHARE = 0.9;
    private static final float CONSTANT_SHIFT = 0.7F;

    private static final Holder<NormalNoise> SHIFT_NOISE =
            Holder.direct(NormalNoise.createParity(-2, 1.0, 1.0));

    private static final Holder<NormalNoise> OTHER_NOISE =
            Holder.direct(NormalNoise.createParity(-3, 1.0, 1.0));

    private static final DensityFunction SHIFT_A = DensityFunctions.shiftA(SHIFT_NOISE);

    private static final DensityFunction SHIFT_B = DensityFunctions.shiftB(SHIFT_NOISE);

    private static final DensityFunction UNWARPED = warped(DensityFunctions.zero(), DensityFunctions.zero());

    private static final DensityFunction WARPED = warped(SHIFT_A, SHIFT_B);

    private static final DensityFunction CACHED_WARPED =
            warped(DensityFunctions.cache(SHIFT_A), DensityFunctions.cache(SHIFT_B));

    private static final List<DensityFunction> UNTURNABLE = List.of(
            warped(DensityFunctions.constant(CONSTANT_SHIFT), DensityFunctions.constant(CONSTANT_SHIFT)),
            warped(SHIFT_A, SHIFT_A),
            warped(SHIFT_B, SHIFT_A),
            warped(SHIFT_A, DensityFunctions.zero()),
            warped(SHIFT_A, DensityFunctions.shiftB(OTHER_NOISE)),
            warped(DensityFunctions.mul(SHIFT_A, DensityFunctions.constant(CONSTANT_SHIFT)), SHIFT_B));

    private static final CuboidAtlas[] ATLASES = {
            CuboidAtlas.of(3, 5, 2, MIN_CHUNK_X, MIN_CHUNK_Z),
            CuboidAtlas.of(16, 24, 40, MIN_CHUNK_X, MIN_CHUNK_Z)};

    private static DensityFunction warped(DensityFunction shiftX, DensityFunction shiftZ) {
        return DensityFunctions.shiftedNoise2d(shiftX, shiftZ, CLIMATE_XZ_SCALE, NOISE_DATA);
    }

    private static float sampleAt(DensitySampler sampler, double[] flat) {
        return sample(sampler, Mth.floor(flat[0]), Y, Mth.floor(flat[1]));
    }

    @Test
    void warpedNoiseStepsSmoothlyAcrossEveryEdge() {
        assertStepsSmoothly(WARPED);
    }

    @Test
    void shiftBStepsSmoothlyAcrossEveryEdge() {
        assertStepsSmoothly(SHIFT_B);
    }

    private static void assertStepsSmoothly(DensityFunction function) {
        Random random = new Random(SEED);
        for (CuboidAtlas atlas : ATLASES) {
            DensitySampler sampler = compile(function, cuboidFold(atlas));
            double largestStep = EDGE_STEP_ALLOWANCE * largestStepInside(sampler, atlas, random);
            for (CuboidEdge edge : atlas.edges()) {
                assertStepsSmoothly(sampler, atlas, edge, largestStep, random);
            }
        }
    }

    private static double largestStepInside(DensitySampler sampler, CuboidAtlas atlas, Random random) {
        double largest = 0.0;
        for (CuboidFace face : atlas.faces()) {
            for (int i = 0; i < SAMPLES; i++) {
                int x = face.blockX(random.nextInt(face.widthBlocks() - 1));
                int z = face.blockZ(random.nextInt(face.depthBlocks() - 1));
                float value = sample(sampler, x, Y, z);
                largest = Math.max(largest, Math.abs(value - sample(sampler, x + 1, Y, z)));
                largest = Math.max(largest, Math.abs(value - sample(sampler, x, Y, z + 1)));
            }
        }

        return largest;
    }

    private static void assertStepsSmoothly(DensitySampler sampler, CuboidAtlas atlas, CuboidEdge edge,
            double largestStep, Random random) {
        CuboidFace first = CuboidFixtures.face(atlas, edge.first());
        for (int i = 0; i < SAMPLES; i++) {
            double along = random.nextInt(edgeBlocks(first, edge.firstSide())) + BLOCK_CENTER;
            float inside = sampleAt(sampler, pointBy(first, edge.firstSide(), along, -BLOCK_CENTER));
            float across = sampleAt(sampler,
                    carried(edge.transform(), pointBy(first, edge.firstSide(), along, BLOCK_CENTER)));
            assertTrue(Math.abs(inside - across) < largestStep,
                    "step " + Math.abs(inside - across) + " from " + edge.first() + " to " + edge.second());
        }
    }

    @Test
    void theShiftPairOfOneNoiseWarpsTheSample() {
        Random random = new Random(SEED);
        for (CuboidAtlas atlas : ATLASES) {
            DensitySampler unwarped = compile(UNWARPED, cuboidFold(atlas));
            DensitySampler warped = compile(WARPED, cuboidFold(atlas));
            DensitySampler cached = compile(CACHED_WARPED, cuboidFold(atlas));
            int moved = 0;
            for (int i = 0; i < SAMPLES; i++) {
                double[] block = blockOn(atlas, random);
                float value = sampleAt(warped, block);
                assertEquals(value, sampleAt(cached, block));
                if (value != sampleAt(unwarped, block)) {
                    moved++;
                }
            }

            assertTrue(moved > SAMPLES * MIN_WARPED_SHARE, moved + " samples moved");
        }
    }

    @Test
    void aPairThatDoesNotTurnAsAVectorLeavesTheSampleInPlace() {
        Random random = new Random(SEED);
        for (CuboidAtlas atlas : ATLASES) {
            DensitySampler unwarped = compile(UNWARPED, cuboidFold(atlas));
            for (DensityFunction function : UNTURNABLE) {
                DensitySampler sampler = compile(function, cuboidFold(atlas));
                for (int i = 0; i < SAMPLES; i++) {
                    double[] block = blockOn(atlas, random);
                    assertEquals(sampleAt(unwarped, block), sampleAt(sampler, block));
                }
            }
        }
    }

    private static double[] blockOn(CuboidAtlas atlas, Random random) {
        CuboidFace face = atlas.faces().get(random.nextInt(atlas.faces().size()));
        return new double[] {
                face.blockX(random.nextInt(face.widthBlocks())),
                face.blockZ(random.nextInt(face.depthBlocks()))};
    }
}
