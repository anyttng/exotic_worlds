package com.exoticworlds.engine.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.core.world.ShapeFixtures;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.world.WorldLoopBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ProtoChunk;

class ChunkFrameTest {
    private static final int CHUNK_WIDTH_BLOCKS = CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    private static final int FACE_WIDTH_BLOCKS = 4 * CHUNK_WIDTH_BLOCKS;
    private static final int Y = 70;

    private static final SeamTransform CLOCKWISE_OVER_THE_EAST_EDGE =
            new SeamTransform(-1, 1, FACE_WIDTH_BLOCKS, -FACE_WIDTH_BLOCKS, true);
    private static final WorldFold TURNED = foldBy(CLOCKWISE_OVER_THE_EAST_EDGE);

    private static final ChunkPos OWN = new ChunkPos(3, 0);
    private static final BlockPos PAST_THE_EDGE = new BlockPos(FACE_WIDTH_BLOCKS + 2, Y, 5);
    private static final BlockPos SEATED = new BlockPos(58, Y, 2);
    private static final BlockPos OWN_BLOCK = new BlockPos(50, Y, 9);
    private static final BlockPos LOCAL_PAIR = new BlockPos(2, Y, 5);

    private static final int TORUS_WIDTH_IN_CHUNKS = 64;
    private static final int TORUS_WIDTH_BLOCKS = TORUS_WIDTH_IN_CHUNKS * CHUNK_WIDTH_BLOCKS;
    private static final WorldFold TORUS =
            WorldFolds.of(ShapeFixtures.torus(WorldLoopBounds.ofWidth(TORUS_WIDTH_IN_CHUNKS)));
    private static final ChunkPos TORUS_OWN = new ChunkPos(0, 0);
    private static final BlockPos ONE_LAP_AWAY = new BlockPos(5 + TORUS_WIDTH_BLOCKS, Y, 5);

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static WorldFold foldBy(SeamTransform move) {
        return (WorldFold) Proxy.newProxyInstance(
                WorldFold.class.getClassLoader(),
                new Class<?>[] {WorldFold.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isPreservingLocalIndices" -> false;
                    case "foldOriented" -> new WorldFold.Folded<>(
                            move.apply((BlockPos) arguments[0]), move.getOrientation());
                    case "foldBlockNode" -> move.apply(BlockPos.of((long) arguments[0])).asLong();
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

    private static BlockState facing(Direction face) {
        return Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, face);
    }

    private static BlockState[] chunkHolding(BlockPos own, BlockState state) {
        BlockState[] cells = new BlockState[CHUNK_WIDTH_BLOCKS * CHUNK_WIDTH_BLOCKS];
        Arrays.fill(cells, AIR);
        cells[cellOf(own.getX(), own.getZ())] = state;
        return cells;
    }

    private static int cellOf(int x, int z) {
        return SectionPos.sectionRelative(z) * CHUNK_WIDTH_BLOCKS + SectionPos.sectionRelative(x);
    }

    private static BlockState read(BlockState[] cells, WorldFold fold, ChunkPos own, BlockPos pos) {
        WorldFold.Folded<BlockPos> seated = ChunkFrame.seat(fold, own, pos);
        if (seated == null) {
            return cells[cellOf(pos.getX(), pos.getZ())];
        }

        BlockState held = cells[cellOf(seated.value().getX(), seated.value().getZ())];
        return ChunkFrame.toReader(held, seated.orientation());
    }

    @Nested
    class AcrossATurnedEdge {
        @Test
        void aBlockReadAnswersTheNeighbouringFaceTurnedToTheReader() {
            BlockState[] cells = chunkHolding(SEATED, facing(Direction.NORTH));

            assertEquals(facing(Direction.WEST), read(cells, TURNED, OWN, PAST_THE_EDGE),
                    "the read past the edge missed the block the neighbouring face holds, or left it unturned");
        }

        @Test
        void thePositionSitsDownAtTheFoldedBlock() {
            WorldFold.Folded<BlockPos> seated = ChunkFrame.seat(TURNED, OWN, PAST_THE_EDGE);

            assertNotNull(seated, "a position past a turned edge kept its raw low bits");
            assertEquals(SEATED, seated.value(), "the position sat down at another block");
        }

        @Test
        void aColumnSitsDownAtTheFoldedColumn() {
            long seated = ChunkFrame.seatColumn(TURNED, OWN, PAST_THE_EDGE.getX(), PAST_THE_EDGE.getZ());

            assertEquals(SEATED.getX(), BlockPos.getX(seated), "the column kept its raw x");
            assertEquals(SEATED.getZ(), BlockPos.getZ(seated), "the column kept its raw z");
        }

        @Test
        void aWrittenPositionSitsDownAtTheFoldedBlock() {
            assertEquals(SEATED, ChunkFrame.seatPos(TURNED, OWN, PAST_THE_EDGE),
                    "a write past a turned edge kept its raw low bits");
        }

        @Test
        void aPostProcessingMarkIsPackedAtTheNeighboursOwnLocalPosition() {
            short packed = ProtoChunk.packOffsetCoordinates(ChunkFrame.seatPos(TURNED, OWN, PAST_THE_EDGE));

            assertEquals(SEATED, ProtoChunk.unpackOffsetCoordinates(packed, SectionPos.blockToSectionCoord(Y), OWN),
                    "the mark was packed at another block of the neighbouring chunk");
        }

        @Test
        void aStateWrittenLandsTurnedIntoTheChunk() {
            assertEquals(facing(Direction.NORTH), ChunkFrame.toChunk(TURNED, PAST_THE_EDGE, facing(Direction.WEST)),
                    "the written state kept the writer's frame");
        }

        @Test
        void aStateWrittenThroughTheMovesOrientationLandsTurnedIntoTheChunk() {
            assertEquals(facing(Direction.NORTH),
                    ChunkFrame.toChunk(facing(Direction.WEST), CLOCKWISE_OVER_THE_EAST_EDGE.getOrientation()),
                    "the written state kept the writer's frame");
        }

        @Test
        void aStateWrittenAndReadBackIsTheStateWritten() {
            BlockState written = facing(Direction.SOUTH);
            BlockState[] cells = chunkHolding(SEATED, ChunkFrame.toChunk(TURNED, PAST_THE_EDGE, written));

            assertEquals(written, read(cells, TURNED, OWN, PAST_THE_EDGE), "the state came back turned");
        }
    }

    @Nested
    class LeftAlone {
        @Test
        void aPositionOfTheChunkItselfNeverReadsTheFold() {
            assertNull(ChunkFrame.seat(unreadFold(), OWN, OWN_BLOCK), "a position of the chunk itself was reseated");

            long column = ChunkFrame.seatColumn(unreadFold(), OWN, OWN_BLOCK.getX(), OWN_BLOCK.getZ());
            assertEquals(OWN_BLOCK.getX(), BlockPos.getX(column), "a column of the chunk itself moved on x");
            assertEquals(OWN_BLOCK.getZ(), BlockPos.getZ(column), "a column of the chunk itself moved on z");

            assertSame(OWN_BLOCK, ChunkFrame.seatPos(unreadFold(), OWN, OWN_BLOCK),
                    "a written position of the chunk itself was reseated");
        }

        @Test
        void aLocalPairWhoseFoldMissesTheChunkStays() {
            assertNull(ChunkFrame.seat(TURNED, OWN, LOCAL_PAIR), "a local pair was folded into another chunk");

            long column = ChunkFrame.seatColumn(TURNED, OWN, LOCAL_PAIR.getX(), LOCAL_PAIR.getZ());
            assertEquals(LOCAL_PAIR.getX(), BlockPos.getX(column), "a local pair moved on x");
            assertEquals(LOCAL_PAIR.getZ(), BlockPos.getZ(column), "a local pair moved on z");
        }
    }

    @Nested
    class OnAFoldThatPreservesLocalIndices {
        @Test
        void noPositionIsReseated() {
            assertNull(ChunkFrame.seat(TORUS, TORUS_OWN, ONE_LAP_AWAY), "a torus read was reseated");
            assertSame(ONE_LAP_AWAY, ChunkFrame.seatPos(TORUS, TORUS_OWN, ONE_LAP_AWAY), "a torus write was reseated");
        }

        @Test
        void aColumnKeepsItsRawCoordinates() {
            long column = ChunkFrame.seatColumn(TORUS, TORUS_OWN, ONE_LAP_AWAY.getX(), ONE_LAP_AWAY.getZ());

            assertEquals(ONE_LAP_AWAY.getX(), BlockPos.getX(column), "a torus column moved on x");
            assertEquals(ONE_LAP_AWAY.getZ(), BlockPos.getZ(column), "a torus column moved on z");
        }

        @Test
        void aWrittenStateIsHandedBack() {
            BlockState state = facing(Direction.WEST);

            assertSame(state, ChunkFrame.toChunk(TORUS, ONE_LAP_AWAY, state), "a torus write turned its state");
        }
    }
}
