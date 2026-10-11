package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.engine.net.RecordPayloadFold.Role;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

class RecordPayloadRowsTest {
    private static final BlockPos CLIENT_INSIDE_BLOCK = new BlockPos(509, 64, -505);
    private static final BlockPos SERVER_PAST_MIN_BLOCK = new BlockPos(-515, 64, -505);
    private static final BlockPos SERVER_FAR_MEMBER = new BlockPos(90, 64, -505);
    private static final BlockPos CLIENT_FAR_MEMBER = new BlockPos(1114, 64, -505);

    private static final Map<String, Role> PAIR_ROW = Map.of("from", Role.GROUP, "to", Role.GROUP);
    private static final Map<String, Role> SIZED_ROW = Map.of("origin", Role.WORLD, "size", Role.UNTOUCHED);

    record PairProbePayload(BlockPos from, BlockPos to) implements CustomPacketPayload {
        static final Type<PairProbePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, "pair_probe"));

        @Override
        public Type<PairProbePayload> type() {
            return TYPE;
        }
    }

    record SizedProbePayload(BlockPos origin, BlockPos size, BlockPos marker) implements CustomPacketPayload {
        static final Type<SizedProbePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, "sized_probe"));

        @Override
        public Type<SizedProbePayload> type() {
            return TYPE;
        }
    }

    record HeldPairProbePayload(Optional<BlockPos> first, BlockPos anchor, BlockPos last)
            implements CustomPacketPayload {
        static final Type<HeldPairProbePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, "held_pair_probe"));

        @Override
        public Type<HeldPairProbePayload> type() {
            return TYPE;
        }
    }

    private static CustomPacketPayload toServer(CustomPacketPayload payload) {
        return ((ServerboundCustomPayloadPacket) PacketTranslator.toServer(
                new ServerboundCustomPayloadPacket(payload), context())).payload();
    }

    private static CustomPacketPayload toClient(CustomPacketPayload payload) {
        return ((ClientboundCustomPayloadPacket) PacketTranslator.toClient(
                new ClientboundCustomPayloadPacket(payload), context())).payload();
    }

    @AfterEach
    void clearTheRows() {
        RecordPayloadFold.declare(Set.of(), Map.of());
    }

    @Test
    void serverboundAGroupStraddlingTheSeamMovesByItsFirstMember() {
        RecordPayloadFold.declare(Set.of(), Map.of(PairProbePayload.TYPE.id(), PAIR_ROW));

        assertEquals(new PairProbePayload(SERVER_BLOCK, SERVER_PAST_MIN_BLOCK),
                toServer(new PairProbePayload(CLIENT_BLOCK, CLIENT_INSIDE_BLOCK)));
    }

    @Test
    void serverboundAPairWithNoRowFoldsEachMemberApart() {
        assertEquals(new PairProbePayload(SERVER_BLOCK, CLIENT_INSIDE_BLOCK),
                toServer(new PairProbePayload(CLIENT_BLOCK, CLIENT_INSIDE_BLOCK)));
    }

    @Test
    void clientboundAGroupWiderThanHalfTheWorldMovesByItsFirstMember() {
        RecordPayloadFold.declare(Set.of(), Map.of(PairProbePayload.TYPE.id(), PAIR_ROW));

        assertEquals(new PairProbePayload(CLIENT_BLOCK, CLIENT_FAR_MEMBER),
                toClient(new PairProbePayload(SERVER_BLOCK, SERVER_FAR_MEMBER)));
    }

    @Test
    void clientboundAPairWithNoRowSeatsEachMemberApart() {
        assertEquals(new PairProbePayload(CLIENT_BLOCK, SERVER_FAR_MEMBER),
                toClient(new PairProbePayload(SERVER_BLOCK, SERVER_FAR_MEMBER)));
    }

    @Test
    void anUntouchedComponentCrossesAsSentAndAnUnnamedOneFoldsBothWays() {
        RecordPayloadFold.declare(Set.of(), Map.of(SizedProbePayload.TYPE.id(), SIZED_ROW));

        assertEquals(new SizedProbePayload(SERVER_BLOCK, CLIENT_BLOCK, SERVER_BLOCK),
                toServer(new SizedProbePayload(CLIENT_BLOCK, CLIENT_BLOCK, CLIENT_BLOCK)));
        assertEquals(new SizedProbePayload(CLIENT_BLOCK, SERVER_BLOCK, CLIENT_BLOCK),
                toClient(new SizedProbePayload(SERVER_BLOCK, SERVER_BLOCK, SERVER_BLOCK)));
    }

    @Test
    void aGroupWhoseFirstMemberIsEmptyAnchorsOnTheNext() {
        RecordPayloadFold.declare(Set.of(), Map.of(HeldPairProbePayload.TYPE.id(),
                Map.of("first", Role.GROUP, "anchor", Role.GROUP, "last", Role.GROUP)));

        assertEquals(new HeldPairProbePayload(Optional.empty(), SERVER_BLOCK, SERVER_PAST_MIN_BLOCK),
                toServer(new HeldPairProbePayload(Optional.empty(), CLIENT_BLOCK, CLIENT_INSIDE_BLOCK)));
    }

    @Test
    void aNameTheRecordLacksIsSkippedAndTheRestKept() {
        RecordPayloadFold.declare(Set.of(), Map.of(PairProbePayload.TYPE.id(),
                Map.of("from", Role.GROUP, "to", Role.GROUP, "middle", Role.UNTOUCHED)));

        assertEquals(new PairProbePayload(SERVER_BLOCK, SERVER_PAST_MIN_BLOCK),
                toServer(new PairProbePayload(CLIENT_BLOCK, CLIENT_INSIDE_BLOCK)));
    }

    @Test
    void theDenyListWinsOverARow() {
        RecordPayloadFold.declare(Set.of(PairProbePayload.TYPE.id()), Map.of(PairProbePayload.TYPE.id(), PAIR_ROW));
        ServerboundCustomPayloadPacket serverbound =
                new ServerboundCustomPayloadPacket(new PairProbePayload(CLIENT_BLOCK, CLIENT_INSIDE_BLOCK));
        ClientboundCustomPayloadPacket clientbound =
                new ClientboundCustomPayloadPacket(new PairProbePayload(SERVER_BLOCK, SERVER_FAR_MEMBER));

        assertSame(serverbound, PacketTranslator.toServer(serverbound, context()));
        assertSame(clientbound, PacketTranslator.toClient(clientbound, context()));
    }
}
