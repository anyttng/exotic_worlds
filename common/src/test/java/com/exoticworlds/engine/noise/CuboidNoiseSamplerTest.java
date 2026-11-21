package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.CuboidFace.BoxVector;
import com.exoticworlds.core.CuboidVertex;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

class CuboidNoiseSamplerTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 64;
    private static final int ARC_STEPS = 200;
    private static final double ARC_RADIUS_CELLS = 0.3;
    private static final double QUARTER = Math.PI / 2.0;
    private static final double SAME_VALUE = 1.0E-5;
    private static final double SAME_POINT = 1.0E-9;
    private static final double SLOPE_STEP_CELLS = 1.0E-3;
    private static final double SLOPE_TOLERANCE = 1.0E-4;
    private static final double LARGEST_ARC_STEP = 0.02;
    private static final double MIN_SPREAD = 0.1;
    private static final double HEIGHT_SPAN = 8.0;
    private static final double NO_FUDGE = 0.0;
    private static final double COMPILED_SCALE = 0.25;
    private static final int MIN_CHUNK_X = -3;
    private static final int MIN_CHUNK_Z = 5;
    private static final String ATLAS_MEMBER = "cuboidAtlas";

    private static final int[][] CHUNK_SIZES = {{1, 1, 1}, {3, 5, 2}, {7, 1, 2}, {16, 24, 40}};

    private static final double[] SCALES = {0.01, 0.0625, 0.25, 1.17};

    private static final double[] FUDGE_SCALES = {NO_FUDGE, 1.0};

    private record Field(CuboidAtlas atlas, CuboidLattice lattice, double scale) {
        static List<Field> all() {
            List<Field> fields = new ArrayList<>();
            for (int[] size : CHUNK_SIZES) {
                CuboidAtlas atlas = CuboidAtlas.of(size[0], size[1], size[2], MIN_CHUNK_X, MIN_CHUNK_Z);
                for (double scale : SCALES) {
                    for (double fudgeYScale : FUDGE_SCALES) {
                        fields.add(of(atlas, scale, fudgeYScale));
                    }
                }
            }

            return fields;
        }

        static Field of(CuboidAtlas atlas, double scale, double fudgeYScale) {
            PerlinNoise noise = new PerlinNoise(new LegacyRandomSource(SEED));
            return new Field(atlas, CuboidLattice.of(noise.perms, noise.offsetY, fudgeYScale, atlas, scale), scale);
        }

        CuboidFace face(CuboidFace.Side side) {
            return this.atlas.faces().stream().filter(face -> face.side() == side).findFirst().orElseThrow();
        }

        double cellBlocks(int blocks) {
            return (double) blocks / CuboidLattice.cellsAlong(blocks, this.scale);
        }

        double valueAtFlat(CuboidFace face, double[] flat, double y) {
            return value(face, flat[0] - face.minBlockX(), flat[1] - face.minBlockZ(), y);
        }

        double value(CuboidFace face, double localX, double localZ, double y) {
            assertTrue(localX > -SAME_POINT && localX < face.widthBlocks() + SAME_POINT, "x " + localX);
            assertTrue(localZ > -SAME_POINT && localZ < face.depthBlocks() + SAME_POINT, "z " + localZ);
            return CuboidNoiseSampler.sample(this.lattice, this.atlas.faces().indexOf(face), localX, y, localZ, y);
        }
    }

    private static WorldFold foldOver(CuboidAtlas atlas) {
        return (WorldFold) Proxy.newProxyInstance(WorldFold.class.getClassLoader(), new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals(ATLAS_MEMBER)) {
                        return atlas;
                    }

                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static boolean runsAlongZ(Direction side) {
        return side == Direction.WEST || side == Direction.EAST;
    }

    private static int edgeBlocks(CuboidFace face, Direction side) {
        return runsAlongZ(side) ? face.depthBlocks() : face.widthBlocks();
    }

    private static int acrossBlocks(CuboidFace face, Direction side) {
        return runsAlongZ(side) ? face.widthBlocks() : face.depthBlocks();
    }

    private static double[] pointBy(CuboidFace face, Direction side, double along, double outward) {
        return switch (side) {
            case WEST -> new double[] {face.minBlockX() - outward, face.minBlockZ() + along};
            case EAST -> new double[] {face.minBlockX() + face.widthBlocks() + outward, face.minBlockZ() + along};
            case NORTH -> new double[] {face.minBlockX() + along, face.minBlockZ() - outward};
            case SOUTH -> new double[] {face.minBlockX() + along, face.minBlockZ() + face.depthBlocks() + outward};
            default -> throw new IllegalArgumentException("A face has no " + side + " edge");
        };
    }

    private static double[] carried(SeamTransform transform, double[] flat) {
        return new double[] {transform.applyX(flat[0], flat[1]), transform.applyZ(flat[0], flat[1])};
    }

    @Test
    void valueAgreesOnBothFacesOfEveryEdge() {
        Random random = new Random(SEED);
        for (Field field : Field.all()) {
            for (CuboidEdge edge : field.atlas().edges()) {
                CuboidFace first = field.face(edge.first());
                CuboidFace second = field.face(edge.second());
                for (int i = 0; i < SAMPLES; i++) {
                    double[] onEdge = pointBy(first, edge.firstSide(),
                            random.nextDouble() * edgeBlocks(first, edge.firstSide()), 0.0);
                    double y = random.nextDouble() * HEIGHT_SPAN;
                    assertEquals(field.valueAtFlat(first, onEdge, y),
                            field.valueAtFlat(second, carried(edge.transform(), onEdge), y), SAME_VALUE,
                            edge.first() + " to " + edge.second());
                }
            }
        }
    }

    @Test
    void slopeAgreesAcrossEveryEdge() {
        Random random = new Random(SEED);
        for (Field field : Field.all()) {
            for (CuboidEdge edge : field.atlas().edges()) {
                CuboidFace first = field.face(edge.first());
                CuboidFace second = field.face(edge.second());
                double step = SLOPE_STEP_CELLS * Math.min(
                        field.cellBlocks(acrossBlocks(first, edge.firstSide())),
                        field.cellBlocks(acrossBlocks(second, edge.secondSide())));
                for (int i = 0; i < SAMPLES; i++) {
                    double along = random.nextDouble() * edgeBlocks(first, edge.firstSide());
                    double y = random.nextDouble() * HEIGHT_SPAN;
                    double onEdge = field.valueAtFlat(first, pointBy(first, edge.firstSide(), along, 0.0), y);
                    double before = field.valueAtFlat(first, pointBy(first, edge.firstSide(), along, -step), y);
                    double after = field.valueAtFlat(second,
                            carried(edge.transform(), pointBy(first, edge.firstSide(), along, step)), y);
                    assertEquals((onEdge - before) / SLOPE_STEP_CELLS, (after - onEdge) / SLOPE_STEP_CELLS,
                            SLOPE_TOLERANCE / SLOPE_STEP_CELLS, edge.first() + " to " + edge.second());
                }
            }
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
    void fieldIsContinuousAroundEveryVertex() {
        for (Field field : Field.all()) {
            for (CuboidVertex vertex : field.atlas().vertices()) {
                List<ArcEnd> ends = new ArrayList<>();
                for (CuboidFace.Side side : vertex.faces()) {
                    walkArc(field, field.face(side), vertex.point(), ends);
                }

                assertEquals(6, ends.size());
                for (ArcEnd end : ends) {
                    List<ArcEnd> met = ends.stream().filter(other -> other != end && other.meets(end)).toList();
                    assertEquals(1, met.size(), "arc ends meeting at one point of vertex " + vertex.point());
                    assertEquals(end.value(), met.getFirst().value(), SAME_VALUE, "vertex " + vertex.point());
                }
            }
        }
    }

    private static void walkArc(Field field, CuboidFace face, BoxVector vertex, List<ArcEnd> ends) {
        int cornerX = face.boxPoint(0, 0).equals(vertex) || face.boxPoint(0, face.depthBlocks()).equals(vertex)
                ? 0
                : face.widthBlocks();
        int cornerZ = face.boxPoint(cornerX, 0).equals(vertex) ? 0 : face.depthBlocks();
        assertEquals(vertex, face.boxPoint(cornerX, cornerZ));
        double radiusX = (cornerX == 0 ? 1 : -1) * ARC_RADIUS_CELLS * field.cellBlocks(face.widthBlocks());
        double radiusZ = (cornerZ == 0 ? 1 : -1) * ARC_RADIUS_CELLS * field.cellBlocks(face.depthBlocks());
        double previous = Double.NaN;
        for (int step = 0; step <= ARC_STEPS; step++) {
            double angle = QUARTER * step / ARC_STEPS;
            double localX = cornerX + radiusX * Math.cos(angle);
            double localZ = cornerZ + radiusZ * Math.sin(angle);
            double value = field.value(face, localX, localZ, 0.37);
            if (step == 0 || step == ARC_STEPS) {
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
    void compiledOctavesSumTheFaceLattices() {
        CuboidAtlas atlas = CuboidAtlas.of(3, 5, 2, MIN_CHUNK_X, MIN_CHUNK_Z);
        NoiseStack stack = (NoiseStack) NormalNoise.createParity(-3, 1.0, 1.0).create(new LegacyRandomSource(SEED));
        PeriodicOctaves octaves = PeriodicOctaveSampler.compile(foldOver(atlas), NoiseFrame.UNDECLARED,
                COMPILED_SCALE, null, stack);
        Random random = new Random(SEED);
        for (CuboidFace face : atlas.faces()) {
            for (int i = 0; i < SAMPLES; i++) {
                int localX = random.nextInt(face.widthBlocks());
                int localZ = random.nextInt(face.depthBlocks());
                double y = random.nextDouble() * HEIGHT_SPAN;
                assertEquals(layerSum(atlas, stack, face, localX, localZ, y),
                        octaves.sample(face.blockX(localX), y, face.blockZ(localZ)), SAME_VALUE);
            }
        }
    }

    private static double layerSum(CuboidAtlas atlas, NoiseStack stack, CuboidFace face, int localX, int localZ,
            double y) {
        double sum = 0.0;
        for (NoiseStack.Layer layer : stack.layers) {
            PerlinNoise leaf = (PerlinNoise) layer.noise();
            double octaveY = y * layer.frequency();
            CuboidLattice lattice = CuboidLattice.of(leaf.perms, leaf.offsetY, NO_FUDGE, atlas,
                    COMPILED_SCALE * layer.frequency());
            sum += layer.amplitude() * CuboidNoiseSampler.sample(lattice, atlas.faces().indexOf(face), localX,
                    GradientNoise.wrap(octaveY), localZ, octaveY);
        }

        return sum;
    }

    @Test
    void blockOutsideEveryFaceIsRefused() {
        CuboidAtlas atlas = CuboidAtlas.of(3, 5, 2, MIN_CHUNK_X, MIN_CHUNK_Z);
        NoiseStack stack = (NoiseStack) NormalNoise.createParity(-3, 1.0, 1.0).create(new LegacyRandomSource(SEED));
        PeriodicOctaves octaves = PeriodicOctaveSampler.compile(foldOver(atlas), NoiseFrame.UNDECLARED,
                COMPILED_SCALE, null, stack);
        CuboidFace top = atlas.faces().stream().filter(face -> face.side() == CuboidFace.Side.TOP).findFirst()
                .orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> octaves.sample(top.blockX(top.widthBlocks()), 0.0, top.blockZ(0)));
    }

    @Test
    void fieldVariesOnEveryFace() {
        Random random = new Random(SEED);
        CuboidAtlas atlas = CuboidAtlas.of(16, 24, 40, MIN_CHUNK_X, MIN_CHUNK_Z);
        Field field = Field.of(atlas, 0.25, NO_FUDGE);
        for (CuboidFace face : atlas.faces()) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < SAMPLES; i++) {
                double value = field.value(face, random.nextDouble() * face.widthBlocks(),
                        random.nextDouble() * face.depthBlocks(), 0.37);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            assertTrue(max - min > MIN_SPREAD, "spread " + (max - min) + " on " + face.side());
        }
    }
}
