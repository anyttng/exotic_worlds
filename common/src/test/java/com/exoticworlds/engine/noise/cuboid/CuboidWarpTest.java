package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.CuboidFixtures.MIN_CHUNK_X;
import static com.exoticworlds.core.CuboidFixtures.MIN_CHUNK_Z;
import static com.exoticworlds.core.CuboidFixtures.atlases;
import static com.exoticworlds.core.CuboidFixtures.carried;
import static com.exoticworlds.core.CuboidFixtures.edgeBlocks;
import static com.exoticworlds.core.CuboidFixtures.pointBy;
import static com.exoticworlds.core.CuboidFixtures.turned;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.core.CuboidFixtures;
import com.exoticworlds.core.CuboidVertex;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.PerlinMath;
import com.exoticworlds.engine.noise.cuboid.CuboidNoiseSampler.Part;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

class CuboidWarpTest {
    private static final long SEED = 0x0153EL;
    private static final long SHIFT_SEED = 0x5EC0DL;
    private static final int SAMPLES = 64;
    private static final double SAME_VALUE = 1.0E-5;
    private static final double SAME_WARPED_VALUE = 1.0E-4;
    private static final double SAME_POINT = 1.0E-9;
    private static final double HEIGHT_SPAN = 8.0;
    private static final double HEIGHT = 0.37;
    private static final int ARC_STEPS_PER_CELL = 400;
    private static final double LARGEST_RADIUS_SHARE = 0.9;
    private static final double QUARTER = Math.PI / 2.0;
    private static final double LARGEST_ARC_STEP = 0.05;
    private static final double MIN_MOVED_SHARE = 0.9;
    private static final double WARP_DIAGONAL = Math.sqrt(2.0);

    private static final double[] SCALES = {0.01, 0.0625, 0.25, 1.17};

    private static final double[] WARPED_SCALES = {0.0625, 0.25, 1.17};

    private static final double[] ARC_WARP_SHARES = {0.125, 0.25, 0.5, 1.0};

    private static CuboidLattice lattice(CuboidAtlas atlas, double scale) {
        PerlinNoise noise = new PerlinNoise(new LegacyRandomSource(SEED));
        return CuboidLattice.of(noise.perms, noise.offsetY, PerlinMath.NO_FUDGE, atlas, scale);
    }

    private static double[] warpAtFlat(CuboidAtlas atlas, double scale, CuboidFace face, double[] flat, double y) {
        int atlasFace = atlas.faces().indexOf(face);
        double localX = flat[0] - face.minBlockX();
        double localZ = flat[1] - face.minBlockZ();
        CuboidLattice lattice = lattice(atlas, scale);
        return new double[] {
                CuboidNoiseSampler.sample(Part.WARP_X, lattice, atlasFace, localX, y, localZ, y),
                CuboidNoiseSampler.sample(Part.WARP_Z, lattice, atlasFace, localX, y, localZ, y)};
    }

    @Test
    void warpTurnsWithTheFrameOnEveryEdge() {
        Random random = new Random(SEED);
        for (CuboidAtlas atlas : atlases()) {
            for (double scale : SCALES) {
                for (CuboidEdge edge : atlas.edges()) {
                    assertTurnsOn(atlas, scale, edge, random);
                }
            }
        }
    }

    private static void assertTurnsOn(CuboidAtlas atlas, double scale, CuboidEdge edge, Random random) {
        CuboidFace first = CuboidFixtures.face(atlas, edge.first());
        CuboidFace second = CuboidFixtures.face(atlas, edge.second());
        for (int i = 0; i < SAMPLES; i++) {
            double[] onEdge = pointBy(first, edge.firstSide(),
                    random.nextDouble() * edgeBlocks(first, edge.firstSide()), 0.0);
            double y = random.nextDouble() * HEIGHT_SPAN;
            double[] expected = turned(edge.transform(), warpAtFlat(atlas, scale, first, onEdge, y));
            double[] actual = warpAtFlat(atlas, scale, second, carried(edge.transform(), onEdge), y);
            String where = edge.first() + " to " + edge.second();
            assertEquals(expected[0], actual[0], SAME_VALUE, where);
            assertEquals(expected[1], actual[1], SAME_VALUE, where);
        }
    }

    private record Warped(CuboidAtlas atlas, CuboidOctaves field, CuboidWarp warp, double scale,
            double largestBlocks) {
        static Warped of(CuboidAtlas atlas, double scale) {
            NoiseStack shiftStack = stack(SHIFT_SEED);
            double shiftBlocks = NoiseConstants.SHIFT_AMPLITUDE / scale;
            CuboidOctaves shift = CuboidOctaves.compile(atlas, NoiseConstants.SHIFT_SCALE, null, shiftStack);
            double largestBlocks = shiftBlocks * shiftStack.range().max() * WARP_DIAGONAL;
            return new Warped(atlas, CuboidOctaves.compile(atlas, scale, null, stack(SEED)),
                    CuboidWarp.of(atlas, shift, shiftBlocks, largestBlocks), scale, largestBlocks);
        }

        private static NoiseStack stack(long seed) {
            return (NoiseStack) NormalNoise.createParity(-3, 1.0, 1.0).create(new LegacyRandomSource(seed));
        }

        double valueAtFlat(CuboidFace face, double[] flat) {
            return value(face, flat[0] - face.minBlockX(), flat[1] - face.minBlockZ());
        }

        double value(CuboidFace face, double localX, double localZ) {
            return this.warp.sampleFrom(this.field, this.atlas.faces().indexOf(face), localX, HEIGHT, localZ);
        }

        double plainValue(CuboidFace face, double localX, double localZ) {
            return this.field.sampleOnFace(this.atlas.faces().indexOf(face), localX, HEIGHT, localZ);
        }

        double radius(double warpShare) {
            int smallestEdge = Integer.MAX_VALUE;
            for (CuboidFace face : this.atlas.faces()) {
                smallestEdge = Math.min(smallestEdge, Math.min(face.widthBlocks(), face.depthBlocks()));
            }

            return Math.min(warpShare * this.largestBlocks, LARGEST_RADIUS_SHARE * smallestEdge);
        }
    }

    @Test
    void warpedNoiseAgreesOnBothFacesOfEveryEdge() {
        Random random = new Random(SEED);
        for (CuboidAtlas atlas : atlases()) {
            for (double scale : WARPED_SCALES) {
                Warped warped = Warped.of(atlas, scale);
                for (CuboidEdge edge : atlas.edges()) {
                    assertAgreesOn(warped, edge, random);
                }
            }
        }
    }

    private static void assertAgreesOn(Warped warped, CuboidEdge edge, Random random) {
        CuboidFace first = CuboidFixtures.face(warped.atlas(), edge.first());
        CuboidFace second = CuboidFixtures.face(warped.atlas(), edge.second());
        for (int i = 0; i < SAMPLES; i++) {
            double[] onEdge = pointBy(first, edge.firstSide(),
                    random.nextDouble() * edgeBlocks(first, edge.firstSide()), 0.0);
            assertEquals(warped.valueAtFlat(first, onEdge),
                    warped.valueAtFlat(second, carried(edge.transform(), onEdge)), SAME_WARPED_VALUE,
                    edge.first() + " to " + edge.second());
        }
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record ArcEnd(double[] boxPoint, double value) {
        boolean meets(ArcEnd other) {
            return Math.abs(this.boxPoint[0] - other.boxPoint[0]) < SAME_POINT
                    && Math.abs(this.boxPoint[1] - other.boxPoint[1]) < SAME_POINT
                    && Math.abs(this.boxPoint[2] - other.boxPoint[2]) < SAME_POINT;
        }
    }

    @Test
    void warpedNoiseIsContinuousAroundEveryVertex() {
        for (CuboidAtlas atlas : atlases()) {
            for (double scale : WARPED_SCALES) {
                Warped warped = Warped.of(atlas, scale);
                for (double warpShare : ARC_WARP_SHARES) {
                    assertContinuousAroundEveryVertex(warped, warped.radius(warpShare));
                }
            }
        }
    }

    private static void assertContinuousAroundEveryVertex(Warped warped, double radius) {
        for (CuboidVertex vertex : warped.atlas().vertices()) {
            assertContinuousAround(warped, vertex, radius);
        }
    }

    private static void assertContinuousAround(Warped warped, CuboidVertex vertex, double radius) {
        List<ArcEnd> ends = new ArrayList<>();
        for (CuboidFace.Side side : vertex.faces()) {
            walkArc(warped, CuboidFixtures.face(warped.atlas(), side), vertex.point(), radius, ends);
        }

        assertEquals(6, ends.size());
        for (ArcEnd end : ends) {
            List<ArcEnd> met = ends.stream().filter(other -> other != end && other.meets(end)).toList();
            assertEquals(1, met.size(), "arc ends meeting at one point of vertex " + vertex.point());
            assertEquals(end.value(), met.getFirst().value(), SAME_WARPED_VALUE, "vertex " + vertex.point());
        }
    }

    private static void walkArc(Warped warped, CuboidFace face, BoxVector vertex, double radius,
            List<ArcEnd> ends) {
        int cornerX = face.boxPoint(0, 0).equals(vertex) || face.boxPoint(0, face.depthBlocks()).equals(vertex)
                ? 0
                : face.widthBlocks();
        int cornerZ = face.boxPoint(cornerX, 0).equals(vertex) ? 0 : face.depthBlocks();
        assertEquals(vertex, face.boxPoint(cornerX, cornerZ));
        double radiusX = cornerX == 0 ? radius : -radius;
        double radiusZ = cornerZ == 0 ? radius : -radius;
        int steps = (int) Math.ceil(QUARTER * radius * warped.scale() * ARC_STEPS_PER_CELL);
        double previous = Double.NaN;
        for (int step = 0; step <= steps; step++) {
            double angle = QUARTER * step / steps;
            double localX = cornerX + radiusX * Math.cos(angle);
            double localZ = cornerZ + radiusZ * Math.sin(angle);
            double value = warped.value(face, localX, localZ);
            if (step == 0 || step == steps) {
                ends.add(new ArcEnd(boxPoint(face, localX, localZ), value));
            }

            if (step > 0) {
                assertTrue(Math.abs(value - previous) < LARGEST_ARC_STEP,
                        "step " + Math.abs(value - previous) + " on " + face.side() + " at vertex " + vertex);
            }

            previous = value;
        }
    }

    private static double[] boxPoint(CuboidFace face, double localX, double localZ) {
        return new double[] {
                face.origin().a() + localX * face.alongX().a() + localZ * face.alongZ().a(),
                face.origin().b() + localX * face.alongX().b() + localZ * face.alongZ().b(),
                face.origin().c() + localX * face.alongX().c() + localZ * face.alongZ().c()};
    }

    @Test
    void warpMovesTheSampleOnEveryFace() {
        Random random = new Random(SEED);
        CuboidAtlas atlas = CuboidAtlas.of(16, 24, 40, MIN_CHUNK_X, MIN_CHUNK_Z);
        Warped warped = Warped.of(atlas, NoiseConstants.SHIFT_SCALE);
        for (CuboidFace face : atlas.faces()) {
            int moved = 0;
            for (int i = 0; i < SAMPLES; i++) {
                double localX = random.nextDouble() * face.widthBlocks();
                double localZ = random.nextDouble() * face.depthBlocks();
                if (Math.abs(warped.value(face, localX, localZ) - warped.plainValue(face, localX, localZ))
                        > SAME_WARPED_VALUE) {
                    moved++;
                }
            }

            assertTrue(moved > SAMPLES * MIN_MOVED_SHARE, moved + " moved on " + face.side());
        }
    }
}
