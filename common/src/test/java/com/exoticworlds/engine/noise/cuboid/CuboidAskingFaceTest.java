package com.exoticworlds.engine.noise.cuboid;

import static com.exoticworlds.core.CuboidFixtures.atlases;
import static com.exoticworlds.core.CuboidFixtures.carried;
import static com.exoticworlds.core.CuboidFixtures.cuboidFold;
import static com.exoticworlds.core.CuboidFixtures.edgeBlocks;
import static com.exoticworlds.core.CuboidFixtures.face;
import static com.exoticworlds.core.CuboidFixtures.pointBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.CuboidAtlas;
import com.exoticworlds.core.CuboidEdge;
import com.exoticworlds.core.CuboidFace;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.engine.noise.BoxDivisors;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseConstants;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class CuboidAskingFaceTest {
    private static final long SEED = 0x1646L;
    private static final double SCALE = 0.0625;
    private static final double Y = 12.3;
    private static final double INSIDE_CORNER_CHUNK = 3.0;
    private static final double CORNER_COLUMN = 4.0;
    private static final double ONE_BLOCK = 1.0;
    private static final double ON_THE_EDGE = 0.0;
    private static final double SAME_VALUE = 1.0E-5;
    private static final double FAR = 5.0;
    private static final double NEAR = 1.0E-3;
    private static final double CONTINUOUS = 5.0E-3;

    private static final Direction[] X_SIDES = {Direction.WEST, Direction.EAST};
    private static final Direction[] Z_SIDES = {Direction.NORTH, Direction.SOUTH};

    private record Corner(CuboidAtlas atlas, CuboidFace face, Direction side, double along, SeamTransform across) {
        static List<Corner> all() {
            List<Corner> corners = new ArrayList<>();
            for (CuboidAtlas atlas : atlases()) {
                for (CuboidFace face : atlas.faces()) {
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        SeamTransform across = across(atlas, face, side);
                        corners.add(new Corner(atlas, face, side, INSIDE_CORNER_CHUNK, across));
                        corners.add(new Corner(atlas, face, side, edgeBlocks(face, side) - INSIDE_CORNER_CHUNK,
                                across));
                    }
                }
            }

            return corners;
        }

        private static SeamTransform across(CuboidAtlas atlas, CuboidFace face, Direction side) {
            for (CuboidEdge edge : atlas.edges()) {
                if (edge.first() == face.side() && edge.firstSide() == side) {
                    return edge.transform();
                }

                if (edge.second() == face.side() && edge.secondSide() == side) {
                    return edge.transform().inverse();
                }
            }

            throw new AssertionError("No edge on the " + side + " side of " + face.side());
        }

        double[] outside(double along, double outward) {
            return pointBy(this.face, this.side, along, outward);
        }

        ChunkPos chunk() {
            return chunkOf(outside(this.along, -ONE_BLOCK));
        }

        ChunkPos chunkAcross() {
            return chunkOf(carried(this.across, outside(this.along, ONE_BLOCK)));
        }

        CuboidOctaves octaves() {
            NoiseStack stack = (NoiseStack) NormalNoise.createParity(-3, 1.0, 1.0)
                    .create(new LegacyRandomSource(SEED));
            return CuboidOctaves.compile(this.atlas, SCALE, BoxDivisors.UNDIVIDED, null, stack);
        }

        float sampleFrom(ChunkPos asking, CuboidOctaves octaves, double[] point) {
            try (GenerationFoldContext.Context.FoldScope _ =
                    GenerationFoldContext.context().withFold(cuboidFold(this.atlas), asking)) {
                return octaves.sample(point[0], Y, point[1]);
            }
        }

        static ChunkPos chunkOf(double[] point) {
            return new ChunkPos(SectionPos.blockToSectionCoord(Mth.floor(point[0])),
                    SectionPos.blockToSectionCoord(Mth.floor(point[1])));
        }
    }

    @Test
    void aSampleOneBlockPastTheAskingFaceIsTheSampleAtTheCarriedPoint() {
        for (Corner corner : Corner.all()) {
            CuboidOctaves octaves = corner.octaves();
            double[] past = corner.outside(corner.along(), ONE_BLOCK);
            double[] carried = carried(corner.across(), past);
            assertEquals(octaves.sample(carried[0], Y, carried[1]), corner.sampleFrom(corner.chunk(), octaves, past),
                    SAME_VALUE, corner.toString());
        }
    }

    @Test
    void twoChunksEitherSideOfAnEdgeReadOneValueAtTheirSharedCornerColumn() {
        for (Corner corner : Corner.all()) {
            CuboidOctaves octaves = corner.octaves();
            double[] column = corner.outside(CORNER_COLUMN, ON_THE_EDGE);
            assertEquals(corner.sampleFrom(corner.chunk(), octaves, column),
                    corner.sampleFrom(corner.chunkAcross(), octaves, carried(corner.across(), column)),
                    SAME_VALUE, corner.toString());
        }
    }

    @Test
    void aCellPastTheAskingFaceIsTheCellAtTheCarriedBlock() {
        for (Corner corner : Corner.all()) {
            CuboidCellGrid grid = CuboidCellGrid.of(corner.atlas(), NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH);
            double[] past = corner.outside(corner.along(), ONE_BLOCK);
            int blockX = Mth.floor(past[0]);
            int blockZ = Mth.floor(past[1]);
            int carriedX = corner.across().applyCellX(blockX, blockZ);
            int carriedZ = corner.across().applyCellZ(blockX, blockZ);
            try (GenerationFoldContext.Context.FoldScope _ =
                    GenerationFoldContext.context().withFold(cuboidFold(corner.atlas()), corner.chunk())) {
                assertEquals(grid.cellOriginX(carriedX, carriedZ), grid.cellOriginX(blockX, blockZ),
                        corner.toString());
                assertEquals(grid.cellOriginZ(carriedX, carriedZ), grid.cellOriginZ(blockX, blockZ),
                        corner.toString());
            }
        }
    }

    @Test
    void aSamplePastTwoEdgesContinuesTheBandItIsNearer() {
        for (CuboidAtlas atlas : atlases()) {
            for (CuboidFace face : atlas.faces()) {
                for (Direction sideX : X_SIDES) {
                    for (Direction sideZ : Z_SIDES) {
                        assertContinuousIntoTheQuarter(atlas, face, sideX, sideZ);
                    }
                }
            }
        }
    }

    private static void assertContinuousIntoTheQuarter(CuboidAtlas atlas, CuboidFace face, Direction sideX,
            Direction sideZ) {
        if (atlas.faceAt(Mth.floor(pastX(face, sideX, ONE_BLOCK)), Mth.floor(pastZ(face, sideZ, ONE_BLOCK))) != null) {
            return;
        }

        Corner corner = new Corner(atlas, face, sideX, INSIDE_CORNER_CHUNK, SeamTransform.IDENTITY);
        CuboidOctaves octaves = corner.octaves();
        ChunkPos asking = Corner.chunkOf(new double[] {pastX(face, sideX, -ONE_BLOCK), pastZ(face, sideZ, -ONE_BLOCK)});
        String at = face.side() + " " + sideX + " " + sideZ;
        assertEquals(
                corner.sampleFrom(asking, octaves, new double[] {pastX(face, sideX, FAR), pastZ(face, sideZ, -NEAR)}),
                corner.sampleFrom(asking, octaves, new double[] {pastX(face, sideX, FAR), pastZ(face, sideZ, NEAR)}),
                CONTINUOUS, at + " beside the band past " + sideX);
        assertEquals(
                corner.sampleFrom(asking, octaves, new double[] {pastX(face, sideX, -NEAR), pastZ(face, sideZ, FAR)}),
                corner.sampleFrom(asking, octaves, new double[] {pastX(face, sideX, NEAR), pastZ(face, sideZ, FAR)}),
                CONTINUOUS, at + " beside the band past " + sideZ);
    }

    private static double pastX(CuboidFace face, Direction side, double outward) {
        return side == Direction.WEST ? face.minBlockX() - outward : face.minBlockX() + face.widthBlocks() + outward;
    }

    private static double pastZ(CuboidFace face, Direction side, double outward) {
        return side == Direction.NORTH ? face.minBlockZ() - outward : face.minBlockZ() + face.depthBlocks() + outward;
    }

    @Test
    void aSampleOnNoFaceWithNoAskingChunkIsRefused() {
        for (CuboidAtlas atlas : atlases()) {
            CuboidFace top = face(atlas, CuboidFace.Side.TOP);
            Corner corner = new Corner(atlas, top, Direction.EAST, INSIDE_CORNER_CHUNK, SeamTransform.IDENTITY);
            CuboidOctaves octaves = corner.octaves();
            double[] past = corner.outside(corner.along(), ONE_BLOCK);
            assertThrows(IllegalArgumentException.class, () -> octaves.sample(past[0], Y, past[1]));
        }
    }
}
