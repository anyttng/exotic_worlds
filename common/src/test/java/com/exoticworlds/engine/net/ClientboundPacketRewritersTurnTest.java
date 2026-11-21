package com.exoticworlds.engine.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.SeamTransform;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

class ClientboundPacketRewritersTurnTest {
    private static final int FACE_WIDTH = 64;
    private static final int VIEW_DISTANCE = 12;

    private static final SeamTransform CLOCKWISE_TO_THE_SEAT = new SeamTransform(-1, 1, FACE_WIDTH, -FACE_WIDTH, true);

    private static final BlockPos SERVER_POS = new BlockPos(58, 70, 2);
    private static final BlockPos SEAT_POS = new BlockPos(61, 70, -6);
    private static final ChunkPos SEAT_CHUNK = new ChunkPos(3, -1);
    private static final double ANCHOR_X = 60.0;

    private static final int EAST = 5;
    private static final int SOUTH = 3;
    private static final int WEST = 4;
    private static final int NORTH = 2;
    private static final int PISTON_EXTEND = 0;
    private static final int BLOCK_STATE_ID = 77;

    private static final WorldFold TURNED = (WorldFold) Proxy.newProxyInstance(
            WorldFold.class.getClassLoader(),
            new Class<?>[] {WorldFold.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "preservesLocalIndices" -> false;
                case "nearestCopy" -> SEAT_CHUNK;
                case "deckTransformation" -> new DeckTransformation(CLOCKWISE_TO_THE_SEAT);
                case "reseat" -> CLOCKWISE_TO_THE_SEAT.apply((BlockPos) arguments[0]);
                default -> throw new UnsupportedOperationException(method.getName());
            });

    private static TranslationContext context() {
        ClientPosition clientPosition = new ClientPosition();
        clientPosition.rebase(ANCHOR_X, 0.0, Level.OVERWORLD, TURNED);
        return new TranslationContext(
                TURNED,
                clientPosition,
                size -> {
                    throw new UnsupportedOperationException();
                },
                Level.OVERWORLD,
                VIEW_DISTANCE,
                VIEW_DISTANCE,
                entityId -> null,
                entityId -> null,
                () -> {
                });
    }

    private static BlockState facing(Direction face) {
        return Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, face);
    }

    @Test
    void aBlockUpdateSentToAQuarterTurnSeatCarriesTheStateTurned() {
        ClientboundBlockUpdatePacket seated = ClientboundPacketRewriters.blockUpdate(
                new ClientboundBlockUpdatePacket(SERVER_POS, facing(Direction.NORTH)), context());

        assertEquals(SEAT_POS, seated.getPos());
        assertEquals(facing(Direction.EAST), seated.getBlockState());
    }

    @Test
    void aPistonEventSentToAQuarterTurnSeatCarriesTheDirectionTurned() {
        ClientboundBlockEventPacket seated = ClientboundPacketRewriters.blockEvent(
                new ClientboundBlockEventPacket(SERVER_POS, Blocks.PISTON, PISTON_EXTEND, EAST), context());

        assertEquals(SEAT_POS, seated.getPos());
        assertEquals(PISTON_EXTEND, seated.getB0());
        assertEquals(SOUTH, seated.getB1());
    }

    @Test
    void aCrafterEventSentToAQuarterTurnSeatCarriesTheDirectionTurned() {
        ClientboundLevelEventPacket seated = ClientboundPacketRewriters.levelEvent(
                new ClientboundLevelEventPacket(LevelEvent.PARTICLES_SHOOT_WHITE_SMOKE, SERVER_POS, WEST, false),
                context());

        assertEquals(SEAT_POS, seated.getPos());
        assertEquals(NORTH, seated.getData());
    }

    @Test
    void aLevelEventWithoutADirectionKeepsItsData() {
        ClientboundLevelEventPacket seated = ClientboundPacketRewriters.levelEvent(
                new ClientboundLevelEventPacket(LevelEvent.PARTICLES_DESTROY_BLOCK, SERVER_POS, BLOCK_STATE_ID, false),
                context());

        assertEquals(SEAT_POS, seated.getPos());
        assertEquals(BLOCK_STATE_ID, seated.getData());
    }
}
