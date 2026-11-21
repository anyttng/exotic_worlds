package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.CuboidFixtures.carried;
import static com.exoticworlds.core.CuboidFixtures.cuboidFold;
import static com.exoticworlds.core.CuboidFixtures.edgeBlocks;
import static com.exoticworlds.core.CuboidFixtures.pointBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.accessors.CoastLiftCache;
import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.core.CuboidFixtures;
import com.exoticworlds.core.CuboidVertex;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.FoldedCompileContext;
import com.exoticworlds.engine.noise.FoldedSamplers;
import com.exoticworlds.engine.noise.NoiseScaleLadder;
import com.exoticworlds.engine.noise.PeriodicEndIslands;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class CuboidEndIslandsTest {
    private static final long SEED = 0x0153EL;
    private static final int ISLAND_NOISE_SKIP = 17292;
    private static final String ONLY_THE_END_ISLAND_RANDOM = "the End islands draw only the End island random";

    private static final int CLEAR_OF_THE_MAIN_ISLAND_CHUNK = 100;
    private static final int TILE_BLOCKS = 8;
    private static final int HALF_TILE_BLOCKS = TILE_BLOCKS / 2;
    private static final int CHUNK_BLOCKS = 16;
    private static final int WINDOW_CHUNKS = 13;
    private static final int LARGEST_RING_TILES = 24;

    private static final float NO_ISLAND_DENSITY = PeriodicEndIslands.density(-100.0F);
    private static final float LARGEST_ISLAND_SIZE = 22.0F;
    private static final float LARGEST_TILE_STEP =
            PeriodicEndIslands.density(LARGEST_ISLAND_SIZE) - PeriodicEndIslands.density(0.0F) + 1.0E-4F;

    private static final CuboidAtlas ATLAS = CuboidAtlas.of(16, 24, 40, CLEAR_OF_THE_MAIN_ISLAND_CHUNK,
            CLEAR_OF_THE_MAIN_ISLAND_CHUNK);
    private static final CuboidAtlas WIDE_ATLAS = CuboidAtlas.of(40, 40, 40, CLEAR_OF_THE_MAIN_ISLAND_CHUNK,
            CLEAR_OF_THE_MAIN_ISLAND_CHUNK);

    private static final DensityFunction.CompileContext SEEDED = new DensityFunction.CompileContext() {
        @Override
        public Noise createNoiseSampler(Holder<NormalNoise> parameters) {
            throw new UnsupportedOperationException(ONLY_THE_END_ISLAND_RANDOM);
        }

        @Override
        public RandomSource createRandom(Identifier seed) {
            throw new UnsupportedOperationException(ONLY_THE_END_ISLAND_RANDOM);
        }

        @Override
        @Deprecated
        public RandomSource createEndIslandRandom() {
            return new LegacyRandomSource(SEED);
        }
    };

    private static final CoastLiftCache NO_LIFT = new CoastLiftCache() {
        @Override
        public double exotic_worlds$coastLift() {
            return 0.0;
        }

        @Override
        public void exotic_worlds$coastLift(double lift) {
        }
    };

    private static DensitySampler islands(CuboidAtlas atlas) {
        return FoldedSamplers.endIslands(
                new FoldedCompileContext(SEEDED, cuboidFold(atlas), NO_LIFT, NoiseScaleLadder.NONE));
    }

    private static float density(DensitySampler islands, double[] flat) {
        return islands.sampleValue(SamplerContext.EMPTY_UNCACHED, Mth.floor(flat[0]), 0, Mth.floor(flat[1]));
    }

    @Test
    void theHeightStepsAcrossEveryEdgeAsItStepsInsideAFace() {
        DensitySampler islands = islands(ATLAS);
        int hits = 0;
        for (CuboidEdge edge : ATLAS.edges()) {
            CuboidFace first = CuboidFixtures.face(ATLAS, edge.first());
            for (int along = HALF_TILE_BLOCKS; along < edgeBlocks(first, edge.firstSide()); along += TILE_BLOCKS) {
                float inside = density(islands, pointBy(first, edge.firstSide(), along, -HALF_TILE_BLOCKS));
                float across = density(islands,
                        carried(edge.transform(), pointBy(first, edge.firstSide(), along, HALF_TILE_BLOCKS)));
                int where = along;
                assertTrue(Math.abs(inside - across) <= LARGEST_TILE_STEP,
                        () -> "step " + Math.abs(inside - across) + " across " + edge.first() + " to "
                                + edge.second() + " at " + where + " blocks along");
                hits += inside > NO_ISLAND_DENSITY ? 1 : 0;
            }
        }

        assertStoodOnIslands(hits, "by an edge");
    }

    private record RingEnd(BoxVector onEdge, float density) {
    }

    @Test
    void theHeightStepsAroundEveryVertexAsItStepsInsideAFace() {
        DensitySampler islands = islands(ATLAS);
        int hits = 0;
        for (CuboidVertex vertex : ATLAS.vertices()) {
            for (int ring = 1; ring <= LARGEST_RING_TILES; ring++) {
                hits += assertRingSteps(islands, vertex, ring);
            }
        }

        assertStoodOnIslands(hits, "around a vertex");
    }

    private static int assertRingSteps(DensitySampler islands, CuboidVertex vertex, int ring) {
        List<RingEnd> ends = new ArrayList<>();
        int hits = 0;
        for (CuboidFace.Side side : vertex.faces()) {
            hits += walkRing(islands, CuboidFixtures.face(ATLAS, side), vertex.point(), ring, ends);
        }

        assertEquals(6, ends.size());
        for (RingEnd end : ends) {
            List<RingEnd> met = ends.stream()
                    .filter(other -> other != end && other.onEdge().equals(end.onEdge()))
                    .toList();
            assertEquals(1, met.size(), "ring ends meeting at one edge point of vertex " + vertex.point());
            float step = Math.abs(end.density() - met.getFirst().density());
            assertTrue(step <= LARGEST_TILE_STEP,
                    () -> "step " + step + " across an edge at vertex " + vertex.point() + ", ring " + ring);
        }

        return hits;
    }

    private static int walkRing(DensitySampler islands, CuboidFace face, BoxVector vertex, int ring,
            List<RingEnd> ends) {
        int cornerX = face.boxPoint(0, 0).equals(vertex) || face.boxPoint(0, face.depthBlocks()).equals(vertex)
                ? 0
                : face.widthBlocks();
        int cornerZ = face.boxPoint(cornerX, 0).equals(vertex) ? 0 : face.depthBlocks();
        assertEquals(vertex, face.boxPoint(cornerX, cornerZ));
        int inwardX = cornerX == 0 ? 1 : -1;
        int inwardZ = cornerZ == 0 ? 1 : -1;
        int far = ring * TILE_BLOCKS - HALF_TILE_BLOCKS;
        int hits = 0;
        float previous = Float.NaN;
        for (int step = 0; step < 2 * ring - 1; step++) {
            int tilesX = Math.min(step, ring - 1);
            int tilesZ = Math.min(2 * ring - 2 - step, ring - 1);
            int localX = cornerX + inwardX * (tilesX * TILE_BLOCKS + HALF_TILE_BLOCKS);
            int localZ = cornerZ + inwardZ * (tilesZ * TILE_BLOCKS + HALF_TILE_BLOCKS);
            float density = density(islands, new double[] {face.blockX(localX), face.blockZ(localZ)});
            if (step > 0) {
                float moved = Math.abs(density - previous);
                assertTrue(moved <= LARGEST_TILE_STEP,
                        () -> "step " + moved + " inside " + face.side() + " at vertex " + vertex + ", ring " + ring);
            }

            previous = density;
            hits += density > NO_ISLAND_DENSITY ? 1 : 0;
        }

        ends.add(new RingEnd(face.boxPoint(cornerX, cornerZ + inwardZ * far),
                density(islands, new double[] {
                        face.blockX(cornerX + inwardX * HALF_TILE_BLOCKS), face.blockZ(cornerZ + inwardZ * far)})));
        ends.add(new RingEnd(face.boxPoint(cornerX + inwardX * far, cornerZ),
                density(islands, new double[] {
                        face.blockX(cornerX + inwardX * far), face.blockZ(cornerZ + inwardZ * HALF_TILE_BLOCKS)})));
        return hits;
    }

    @Test
    void aColumnWhoseWindowStaysInsideItsFaceReadsLikeNoFold() {
        DensitySampler islands = islands(WIDE_ATLAS);
        SimplexNoise islandNoise = islandNoise();
        int hits = 0;
        for (CuboidFace face : WIDE_ATLAS.faces()) {
            int clear = WINDOW_CHUNKS * CHUNK_BLOCKS;
            for (int localX = clear; localX < face.widthBlocks() - clear; localX += TILE_BLOCKS) {
                for (int localZ = clear; localZ < face.depthBlocks() - clear; localZ += TILE_BLOCKS) {
                    int blockX = face.blockX(localX);
                    int blockZ = face.blockZ(localZ);
                    float plain = PeriodicEndIslands.density(
                            PeriodicEndIslands.outerHeightValue(islandNoise, WorldFolds.NOOP, blockX, blockZ));
                    assertEquals(plain, islands.sampleValue(SamplerContext.EMPTY_UNCACHED, blockX, 0, blockZ),
                            () -> "at (" + blockX + ", " + blockZ + ") on " + face.side());
                    hits += plain > NO_ISLAND_DENSITY ? 1 : 0;
                }
            }
        }

        assertStoodOnIslands(hits, "inside a face");
    }

    private static SimplexNoise islandNoise() {
        RandomSource random = new LegacyRandomSource(SEED);
        random.consumeCount(ISLAND_NOISE_SKIP);
        return new SimplexNoise(random, true);
    }

    private static void assertStoodOnIslands(int hits, String where) {
        assertTrue(hits > 0, () -> "no sample " + where + " stood on an island, so every comparison read the void");
    }
}
