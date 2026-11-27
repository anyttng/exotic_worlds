package com.exoticworlds.engine.seam.circumnavigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.Codec;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidFace;
import com.exoticworlds.core.fold.cuboid.CuboidFixtures;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.world.WorldLoopBounds;
import com.exoticworlds.engine.seam.circumnavigation.BoxBands.Band;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class SeamTravelBoxTest {
    private static final CuboidAtlas ATLAS =
            CuboidAtlas.of(3, 5, 2, CuboidFixtures.MIN_CHUNK_X, CuboidFixtures.MIN_CHUNK_Z);
    private static final WorldFold FOLD = CuboidFixtures.cuboidFold(ATLAS);
    private static final int LAP_AROUND_A_BLOCKS = 2 * (80 + 32);
    private static final int LAP_AROUND_B_BLOCKS = 2 * (48 + 32);
    private static final int LAP_AROUND_C_BLOCKS = 2 * (48 + 80);
    private static final double BLOCK_Y = CuboidFixtures.BLOCK_Y;
    private static final double CELL_CENTER = 0.5;
    private static final double VERTEX_RADIUS_BLOCKS = 8.0;
    private static final double VERTEX_ARC_STEP_DEGREES = 1.0;
    private static final double VERTEX_ARC_FIRST_DEGREES = -89.5;
    private static final double VERTEX_ARC_DEGREES = 270.0;
    private static final int VERTEX_LOOPS = 50;
    private static final int PARTIAL_BAND_BLOCKS = 20;
    private static final int NORTH_ONTO_TOP_BLOCKS = 45;
    private static final double TOLERANCE = 1.0E-9;
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 WEST = new Vec3(-1.0, 0.0, 0.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 NORTH = new Vec3(0.0, 0.0, -1.0);

    private static final class Walker {
        private final SeamTravel travel = new SeamTravel();

        private Vec3 position;

        private Vec3 heading;

        Walker(Vec3 start, Vec3 heading) {
            this.position = start;
            this.heading = heading;
            this.travel.advance(FOLD, Level.OVERWORLD, start);
        }

        SeamTravel.Step step() {
            WorldFold.Folded<Vec3> next = FOLD.foldOriented(this.position.add(this.heading));
            this.heading = next.orientation().applyToDelta(this.heading);
            this.position = next.value();
            return this.travel.advance(FOLD, Level.OVERWORLD, this.position);
        }

        void turn(Vec3 heading) {
            this.heading = heading;
        }
    }

    static Stream<Arguments> bands() {
        return Stream.of(
                Arguments.of(CuboidFace.Side.WEST, EAST, Band.AROUND_B, LAP_AROUND_B_BLOCKS),
                Arguments.of(CuboidFace.Side.WEST, WEST, Band.AROUND_B, LAP_AROUND_B_BLOCKS),
                Arguments.of(CuboidFace.Side.FRONT, SOUTH, Band.AROUND_A, LAP_AROUND_A_BLOCKS),
                Arguments.of(CuboidFace.Side.FRONT, NORTH, Band.AROUND_A, LAP_AROUND_A_BLOCKS),
                Arguments.of(CuboidFace.Side.WEST, SOUTH, Band.AROUND_C, LAP_AROUND_C_BLOCKS),
                Arguments.of(CuboidFace.Side.WEST, NORTH, Band.AROUND_C, LAP_AROUND_C_BLOCKS));
    }

    @ParameterizedTest
    @MethodSource("bands")
    void aStraightWalkAroundOneBandClosesOneLapOnItsLength(CuboidFace.Side start, Vec3 heading, Band band,
            int lapBlocks) {
        Walker walker = new Walker(centerOf(start), heading);

        for (int block = 1; block < lapBlocks; block++) {
            assertFalse(walker.step().isLapClosed(), "closed after " + block + " of " + lapBlocks + " blocks");
        }

        SeamTravel.Step closing = walker.step();

        assertEquals(Set.of(band), closing.closedBands());
        assertEquals(Set.of(), closing.closed());
        assertEquals(0.0, walker.travel.getBandsIn(Level.OVERWORLD).on(band), TOLERANCE);
    }

    @Test
    void loopsAroundOneVertexCloseNoLap() {
        CuboidFace front = CuboidFixtures.face(ATLAS, CuboidFace.Side.FRONT);
        SeamTravel travel = new SeamTravel();
        int arcSteps = (int) (VERTEX_ARC_DEGREES / VERTEX_ARC_STEP_DEGREES);

        for (int loop = 0; loop < VERTEX_LOOPS; loop++) {
            for (int arcStep = 0; arcStep < arcSteps; arcStep++) {
                double radians = Math.toRadians(VERTEX_ARC_FIRST_DEGREES + arcStep * VERTEX_ARC_STEP_DEGREES);
                Vec3 position = new Vec3(
                        front.minBlockX() + VERTEX_RADIUS_BLOCKS * Math.cos(radians),
                        BLOCK_Y,
                        front.minBlockZ() + VERTEX_RADIUS_BLOCKS * Math.sin(radians));
                assertFalse(travel.advance(FOLD, Level.OVERWORLD, position).isLapClosed(),
                        "closed on loop " + loop + " at " + position);
            }
        }
    }

    @Test
    void steppingOntoABandsCapStartsItsLapOver() {
        Walker walker = new Walker(centerOf(CuboidFace.Side.WEST), EAST);
        for (int block = 0; block < PARTIAL_BAND_BLOCKS; block++) {
            walker.step();
        }
        double beforeCap = walker.travel.getBandsIn(Level.OVERWORLD).aroundB();

        walker.turn(NORTH);
        for (int block = 0; block < NORTH_ONTO_TOP_BLOCKS; block++) {
            walker.step();
        }
        walker.turn(SOUTH);
        for (int block = 0; block < NORTH_ONTO_TOP_BLOCKS; block++) {
            walker.step();
        }

        assertEquals(PARTIAL_BAND_BLOCKS, beforeCap, TOLERANCE);
        assertEquals(0.0, walker.travel.getBandsIn(Level.OVERWORLD).aroundB(), TOLERANCE);
    }

    @Test
    void theCodecCarriesABoxLedgerBesideATorusOne() {
        Walker walker = new Walker(centerOf(CuboidFace.Side.FRONT), SOUTH);
        for (int block = 0; block < PARTIAL_BAND_BLOCKS; block++) {
            walker.step();
        }
        WorldFold torus = WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(32)));
        walker.travel.advance(torus, Level.NETHER, new Vec3(0.0, BLOCK_Y, 0.0));
        walker.travel.advance(torus, Level.NETHER, new Vec3(40.0, BLOCK_Y, 0.0));

        Codec<SeamTravel> codec = SeamTravel.CODEC;
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, walker.travel).getOrThrow();
        SeamTravel decoded = codec.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(-PARTIAL_BAND_BLOCKS, decoded.getBandsIn(Level.OVERWORLD).aroundA(), TOLERANCE);
        assertEquals(40.0, decoded.getProgressIn(Level.NETHER).x(), TOLERANCE);
    }

    private static Vec3 centerOf(CuboidFace.Side side) {
        CuboidFace face = CuboidFixtures.face(ATLAS, side);
        return new Vec3(
                face.minBlockX() + face.widthBlocks() / 2 + CELL_CENTER,
                BLOCK_Y,
                face.minBlockZ() + face.depthBlocks() / 2 + CELL_CENTER);
    }
}
