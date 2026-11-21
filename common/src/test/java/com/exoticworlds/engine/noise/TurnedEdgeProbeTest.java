package com.exoticworlds.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

class TurnedEdgeProbeTest {
    private static final long SEED = 0x0153EL;
    private static final long SECOND_SEED = 0xC0FFEEL;
    private static final int AXES = 3;
    private static final int CORNERS = 8;
    private static final int SAMPLES = 256;
    private static final int ARC_STEPS = 200;
    private static final double ARC_RADIUS = 0.3;
    private static final double QUARTER = Math.PI / 2.0;
    private static final double EXACT = 1.0E-9;
    private static final double SLOPE_STEP = 1.0E-5;
    private static final double SLOPE_TOLERANCE = 1.0E-3;
    private static final double LARGEST_ARC_STEP = 0.02;
    private static final double VISIBLE_DIFFERENCE = 1.0E-3;
    private static final double MIN_SPREAD = 0.1;
    private static final double HEIGHT_SPAN = 8.0;
    private static final int PERMUTATION_MASK = 0xFF;
    private static final int GRADIENT_MASK = 15;

    private static final int[][] SIZES = {{4, 4, 4}, {3, 5, 2}, {7, 1, 2}, {300, 257, 2}, {1, 1, 1}};

    private record Face(int normal, int side, int uAxis, int vAxis) {
        int sign() {
            return this.side == 1 ? 1 : -1;
        }
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record Basis(double[] first, double[] second) {
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record Sample(double scalar, double[] vector) {
    }

    private record Edge(Face owner, Face other, int tangent) {
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record Box(int[] cells, byte[] permutations, byte[] secondPermutations) {
        static Box of(int[] cells) {
            return new Box(cells, permutationsOf(SEED), permutationsOf(SECOND_SEED));
        }

        Box swapped() {
            return new Box(this.cells, this.secondPermutations, this.permutations);
        }

        List<Face> faces() {
            List<Face> faces = new ArrayList<>();
            for (int normal = 0; normal < AXES; normal++) {
                int next = (normal + 1) % AXES;
                int last = (normal + 2) % AXES;
                faces.add(new Face(normal, 0, next, last));
                faces.add(new Face(normal, 1, last, next));
            }

            return faces;
        }

        List<Edge> edges() {
            List<Edge> edges = new ArrayList<>();
            List<Face> faces = faces();
            for (Face owner : faces) {
                for (Face other : faces) {
                    if (owner.normal() < other.normal()) {
                        edges.add(new Edge(owner, other, AXES - owner.normal() - other.normal()));
                    }
                }
            }

            return edges;
        }

        double[] pointOn(Edge edge, double along) {
            double[] point = new double[AXES];
            point[edge.owner().normal()] = edge.owner().side() * this.cells[edge.owner().normal()];
            point[edge.other().normal()] = edge.other().side() * this.cells[edge.other().normal()];
            point[edge.tangent()] = along;
            return point;
        }

        Sample sample(Face face, double[] point, double y) {
            double u = point[face.uAxis()];
            double v = point[face.vAxis()];
            int uCell = cellOf(u, this.cells[face.uAxis()]);
            int vCell = cellOf(v, this.cells[face.vAxis()]);
            int yCell = (int) Math.floor(y);
            double uFrac = u - uCell;
            double vFrac = v - vCell;
            double yFrac = y - yCell;
            double scalar = 0.0;
            double[] vector = new double[AXES];
            for (int corner = 0; corner < CORNERS; corner++) {
                int uStep = corner & 1;
                int yStep = corner >> 1 & 1;
                int vStep = corner >> 2;
                int[] node = new int[AXES];
                node[face.normal()] = face.side() * this.cells[face.normal()];
                node[face.uAxis()] = uCell + uStep;
                node[face.vAxis()] = vCell + vStep;
                double[] offset = new double[AXES];
                offset[face.uAxis()] = uFrac - uStep;
                offset[face.vAxis()] = vFrac - vStep;
                double weight = weight(uFrac, uStep) * weight(yFrac, yStep) * weight(vFrac, vStep);
                scalar += weight * cornerValue(face, node, yCell + yStep, offset, yFrac - yStep, vector, weight);
            }

            return new Sample(scalar, vector);
        }

        private double cornerValue(Face face, int[] node, int yNode, double[] offset, double yOffset,
                double[] vector, double weight) {
            int hash = hash(this.permutations, node, yNode);
            Basis basis = basisOf(face, node);
            if (basis == null) {
                return (hash & 1) == 0 ? yOffset : -yOffset;
            }

            double first = dot(offset, basis.first());
            double second = dot(offset, basis.second());
            double firstRow = row(PeriodicNoiseSampler.GRADIENT[hash & GRADIENT_MASK], first, yOffset, second);
            double secondRow = row(
                    PeriodicNoiseSampler.GRADIENT[hash(this.secondPermutations, node, yNode) & GRADIENT_MASK],
                    first, yOffset, second);
            for (int axis = 0; axis < AXES; axis++) {
                vector[axis] += weight * (firstRow * basis.first()[axis] + secondRow * basis.second()[axis]);
            }

            return firstRow;
        }

        private @Nullable Basis basisOf(Face face, int[] node) {
            int low = -1;
            int high = -1;
            int count = 0;
            for (int axis = 0; axis < AXES; axis++) {
                if (node[axis] == 0 || node[axis] == this.cells[axis]) {
                    low = count == 0 ? axis : low;
                    high = axis;
                    count++;
                }
            }

            if (count == AXES) {
                return null;
            }

            if (count == 1) {
                return new Basis(unit((face.normal() + 1) % AXES, 1), unit((face.normal() + 2) % AXES, 1));
            }

            double[] along = unit(AXES - low - high, 1);
            return face.normal() == low
                    ? new Basis(along, unit(high, outward(node, high)))
                    : new Basis(along, unit(low, -outward(node, low)));
        }

        private static int outward(int[] node, int axis) {
            return node[axis] == 0 ? -1 : 1;
        }
    }

    private static byte[] permutationsOf(long seed) {
        return new PerlinNoise(new LegacyRandomSource(seed)).perms;
    }

    private static int cellOf(double coord, int cells) {
        return Math.min((int) Math.floor(coord), cells - 1);
    }

    private static double weight(double frac, int step) {
        double smooth = frac * frac * frac * (frac * (frac * 6.0 - 15.0) + 10.0);
        return step == 0 ? 1.0 - smooth : smooth;
    }

    private static double row(int[] gradient, double first, double yOffset, double second) {
        return gradient[0] * first + gradient[1] * yOffset + gradient[2] * second;
    }

    private static double dot(double[] left, double[] right) {
        return left[0] * right[0] + left[1] * right[1] + left[2] * right[2];
    }

    private static double[] unit(int axis, int sign) {
        double[] unit = new double[AXES];
        unit[axis] = sign;
        return unit;
    }

    private static int hash(byte[] permutations, int[] node, int yNode) {
        return p(permutations, p(permutations, p(permutations, p(permutations, node[0]) + node[1]) + node[2])
                + yNode);
    }

    private static int p(byte[] permutations, int index) {
        return permutations[index & PERMUTATION_MASK] & PERMUTATION_MASK;
    }

    private static double across(Edge edge, Face face, double[] vector) {
        Face opposite = face == edge.owner() ? edge.other() : edge.owner();
        int direction = face == edge.owner() ? opposite.sign() : -opposite.sign();
        return vector[opposite.normal()] * direction;
    }

    private static double[] steppedInto(Edge edge, Face face, double[] point) {
        Face opposite = face == edge.owner() ? edge.other() : edge.owner();
        double[] stepped = point.clone();
        stepped[opposite.normal()] -= opposite.sign() * SLOPE_STEP;
        return stepped;
    }

    @Test
    void valueAndWarpAgreeAcrossEveryEdge() {
        Random random = new Random(SEED);
        for (int[] size : SIZES) {
            Box box = Box.of(size);
            for (Edge edge : box.edges()) {
                for (int i = 0; i < SAMPLES; i++) {
                    double[] point = box.pointOn(edge, random.nextDouble() * size[edge.tangent()]);
                    double y = random.nextDouble() * HEIGHT_SPAN;
                    Sample owner = box.sample(edge.owner(), point, y);
                    Sample other = box.sample(edge.other(), point, y);
                    assertEquals(owner.scalar(), other.scalar(), EXACT);
                    assertEquals(owner.vector()[edge.tangent()], other.vector()[edge.tangent()], EXACT);
                    assertEquals(across(edge, edge.owner(), owner.vector()),
                            across(edge, edge.other(), other.vector()), EXACT);
                }
            }
        }
    }

    @Test
    void slopeAgreesAcrossEveryEdge() {
        Random random = new Random(SEED);
        for (int[] size : SIZES) {
            Box box = Box.of(size);
            for (Edge edge : box.edges()) {
                for (int i = 0; i < SAMPLES; i++) {
                    double[] point = box.pointOn(edge, random.nextDouble() * size[edge.tangent()]);
                    double y = random.nextDouble() * HEIGHT_SPAN;
                    double onEdge = box.sample(edge.owner(), point, y).scalar();
                    double inOwner = box.sample(edge.owner(), steppedInto(edge, edge.owner(), point), y)
                            .scalar();
                    double inOther = box.sample(edge.other(), steppedInto(edge, edge.other(), point), y)
                            .scalar();
                    assertEquals((onEdge - inOwner) / SLOPE_STEP, (inOther - onEdge) / SLOPE_STEP,
                            SLOPE_TOLERANCE);
                }
            }
        }
    }

    @Test
    void fieldIsContinuousAroundEveryVertex() {
        for (int[] size : SIZES) {
            Box box = Box.of(size);
            for (int vertex = 0; vertex < CORNERS; vertex++) {
                double[] loop = vertexLoop(box, vertex, 0.37);
                for (int i = 0; i < loop.length; i++) {
                    double step = Math.abs(loop[(i + 1) % loop.length] - loop[i]);
                    assertTrue(step < LARGEST_ARC_STEP, "step " + step + " at " + i + " of vertex " + vertex);
                }
            }
        }
    }

    private static double[] vertexLoop(Box box, int vertex, double y) {
        int[] sides = {vertex & 1, vertex >> 1 & 1, vertex >> 2};
        double[] values = new double[AXES * ARC_STEPS];
        for (int normal = 0; normal < AXES; normal++) {
            int from = (normal + 1) % AXES;
            int to = (normal + 2) % AXES;
            Face face = faceOf(box, normal, sides[normal]);
            for (int step = 0; step < ARC_STEPS; step++) {
                double angle = QUARTER * step / ARC_STEPS;
                double[] point = new double[AXES];
                for (int axis = 0; axis < AXES; axis++) {
                    point[axis] = sides[axis] * box.cells()[axis];
                }

                point[from] += inward(sides[from]) * ARC_RADIUS * Math.cos(angle);
                point[to] += inward(sides[to]) * ARC_RADIUS * Math.sin(angle);
                values[normal * ARC_STEPS + step] = box.sample(face, point, y).scalar();
            }
        }

        return values;
    }

    private static int inward(int side) {
        return side == 1 ? -1 : 1;
    }

    private static Face faceOf(Box box, int normal, int side) {
        return box.faces().stream()
                .filter(face -> face.normal() == normal && face.side() == side)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void fieldVariesOnAFace() {
        Box box = Box.of(SIZES[0]);
        Face face = box.faces().getFirst();
        Random random = new Random(SEED);
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < SAMPLES; i++) {
            double[] point = new double[AXES];
            point[face.uAxis()] = random.nextDouble() * box.cells()[face.uAxis()];
            point[face.vAxis()] = random.nextDouble() * box.cells()[face.vAxis()];
            double value = box.sample(face, point, 0.37).scalar();
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        assertTrue(max - min > MIN_SPREAD);
    }

    @Test
    void todaysSamplerDisagreesWithItsTurnedCopy() {
        byte[] permutations = permutationsOf(SEED);
        Random random = new Random(SEED);
        long period = SIZES[0][0];
        int differing = 0;
        for (int i = 0; i < SAMPLES; i++) {
            double x = random.nextDouble() * period;
            double z = random.nextDouble() * period;
            double y = random.nextDouble() * HEIGHT_SPAN;
            double value = PeriodicNoiseSampler.sampleLattice(permutations, x, y, z, period, period);
            double turned = PeriodicNoiseSampler.sampleLattice(permutations, -z, y, x, period, period);
            if (Math.abs(value - turned) > VISIBLE_DIFFERENCE) {
                differing++;
            }
        }

        assertTrue(differing > SAMPLES / 2, "differing " + differing);
    }

    @Test
    void twoScalarsDoNotTurnAsAVector() {
        Box box = Box.of(SIZES[0]);
        Box swapped = box.swapped();
        Random random = new Random(SEED);
        int differing = 0;
        int total = 0;
        for (Edge edge : box.edges()) {
            for (int i = 0; i < SAMPLES; i++) {
                double[] point = box.pointOn(edge, random.nextDouble() * box.cells()[edge.tangent()]);
                double y = random.nextDouble() * HEIGHT_SPAN;
                double[] owner = scalarPair(box, swapped, edge.owner(), point, y);
                double[] other = scalarPair(box, swapped, edge.other(), point, y);
                boolean agrees = Math.abs(owner[edge.tangent()] - other[edge.tangent()]) < VISIBLE_DIFFERENCE
                        && Math.abs(across(edge, edge.owner(), owner) - across(edge, edge.other(), other))
                                < VISIBLE_DIFFERENCE;
                differing += agrees ? 0 : 1;
                total++;
            }
        }

        assertTrue(differing * 2 >= total, "differing " + differing + " of " + total);
    }

    private static double[] scalarPair(Box box, Box swapped, Face face, double[] point, double y) {
        double[] vector = new double[AXES];
        vector[face.uAxis()] = box.sample(face, point, y).scalar();
        vector[face.vAxis()] = swapped.sample(face, point, y).scalar();
        return vector;
    }
}
