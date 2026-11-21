package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.CuboidFixtures.atlases;
import static com.exoticworlds.core.CuboidFixtures.carried;
import static com.exoticworlds.core.CuboidFixtures.cuboidFold;
import static com.exoticworlds.core.CuboidFixtures.edgeBlocks;
import static com.exoticworlds.core.CuboidFixtures.pointBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.SimplexSlot;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class CuboidSimplexStandInTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 64;
    private static final int ARC_STEPS = 200;
    private static final double ARC_RADIUS_CELLS = 0.3;
    private static final double QUARTER = Math.PI / 2.0;
    private static final double SAME_VALUE = 1.0E-5;
    private static final double SAME_POINT = 1.0E-9;
    private static final double LARGEST_ARC_STEP = 0.03;
    private static final double MIN_SPREAD = 0.1;

    private static final double[] READER_SCALES = {
            NoiseConstants.FROZEN_TEMPERATURE_SCALE,
            NoiseConstants.BIOME_INFO_EDGE_SCALE,
            NoiseConstants.BIOME_INFO_PATCH_SCALE,
            NoiseConstants.HEIGHT_TEMPERATURE_SCALE,
            1.0 / NoiseConstants.BIOME_INFO_THRESHOLD_DIVISOR};

    private record Field(CuboidAtlas atlas, CuboidSimplexStandIn standIn, double scale) {
        static List<Field> all() {
            List<Field> fields = new ArrayList<>();
            for (CuboidAtlas atlas : atlases()) {
                for (double scale : READER_SCALES) {
                    fields.add(new Field(atlas, CuboidSimplexStandIn.of(cuboidFold(atlas), atlas, scale, leaf()),
                            scale));
                }
            }

            return fields;
        }

        CuboidFace face(CuboidFace.Side side) {
            return CuboidFixtures.face(this.atlas, side);
        }

        double cellBlocks(int blocks) {
            return (double) blocks / CuboidLattice.cellsAlong(blocks, this.scale * CuboidSimplexStandIn.RATE);
        }

        double valueAtFlat(CuboidFace face, double[] flat) {
            return value(face, flat[0] - face.minBlockX(), flat[1] - face.minBlockZ());
        }

        double value(CuboidFace face, double localX, double localZ) {
            assertTrue(localX > -SAME_POINT && localX < face.widthBlocks() + SAME_POINT, "x " + localX);
            assertTrue(localZ > -SAME_POINT && localZ < face.depthBlocks() + SAME_POINT, "z " + localZ);
            return this.standIn.sampleOnFace(this.atlas.faces().indexOf(face), localX, localZ);
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

    private static SimplexNoise leaf() {
        return new SimplexNoise(new LegacyRandomSource(SEED), true);
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
                    assertEquals(field.valueAtFlat(first, onEdge),
                            field.valueAtFlat(second, carried(edge.transform(), onEdge)), SAME_VALUE,
                            edge.first() + " to " + edge.second() + " at scale " + field.scale());
                }
            }
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
            double value = field.value(face, localX, localZ);
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
    void theSlotReadsTheStandInAtABlockOfTheNet() {
        Random random = new Random(SEED);
        SimplexNoise leaf = leaf();
        for (CuboidAtlas atlas : atlases()) {
            WorldFold fold = cuboidFold(atlas);
            for (double scale : READER_SCALES) {
                SimplexSlot slot = new SimplexSlot();
                CuboidSimplexStandIn standIn = CuboidSimplexStandIn.of(fold, atlas, scale, leaf);
                for (CuboidFace face : atlas.faces()) {
                    int localX = random.nextInt(face.widthBlocks());
                    int localZ = random.nextInt(face.depthBlocks());
                    assertEquals((float) standIn.sampleOnFace(atlas.faces().indexOf(face), localX, localZ),
                            slot.sample(leaf, fold, scale, face.blockX(localX), face.blockZ(localZ)));
                }
            }
        }
    }

    @Test
    void blockOutsideEveryFaceIsRefused() {
        CuboidAtlas atlas = atlases()[1];
        WorldFold fold = cuboidFold(atlas);
        CuboidFace top = CuboidFixtures.face(atlas, CuboidFace.Side.TOP);
        SimplexSlot slot = new SimplexSlot();
        assertThrows(IllegalArgumentException.class, () -> slot.sample(leaf(), fold,
                NoiseConstants.BIOME_INFO_EDGE_SCALE, top.blockX(top.widthBlocks()), top.blockZ(0)));
    }

    @Test
    void fieldVariesOnEveryFace() {
        CuboidAtlas atlas = atlases()[3];
        Field field = new Field(atlas, CuboidSimplexStandIn.of(cuboidFold(atlas), atlas,
                NoiseConstants.BIOME_INFO_PATCH_SCALE, leaf()), NoiseConstants.BIOME_INFO_PATCH_SCALE);
        Random random = new Random(SEED);
        for (CuboidFace face : atlas.faces()) {
            double low = Double.POSITIVE_INFINITY;
            double high = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < SAMPLES; i++) {
                double value = field.value(face, random.nextDouble() * face.widthBlocks(),
                        random.nextDouble() * face.depthBlocks());
                low = Math.min(low, value);
                high = Math.max(high, value);
            }

            assertTrue(high - low > MIN_SPREAD, "spread " + (high - low) + " on " + face.side());
        }
    }
}
