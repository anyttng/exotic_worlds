package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.engine.net.RecordPayloadFold.ComponentRole;
import com.exoticworlds.engine.net.RecordPayloadFold.Role;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

class RecordPayloadRowsTest {
    private static final BlockPos CLIENT_INSIDE_BLOCK = new BlockPos(509, 64, -505);
    private static final BlockPos SERVER_PAST_MIN_BLOCK = new BlockPos(-515, 64, -505);
    private static final BlockPos SERVER_FAR_MEMBER = new BlockPos(90, 64, -505);
    private static final BlockPos CLIENT_FAR_MEMBER = new BlockPos(1114, 64, -505);

    private static final Vec3 CLIENT_DIRECTION = Vec3.atLowerCornerOf(CLIENT_BLOCK);

    private static final Map<String, ComponentRole> PAIR_ROW = Map.of("from", direct(Role.GROUP), "to",
            direct(Role.GROUP));
    private static final Map<String, ComponentRole> SIZED_ROW = Map.of("origin", direct(Role.WORLD), "size",
            direct(Role.UNTOUCHED));
    private static final Map<String, ComponentRole> UNTOUCHED_TANGENT_ROW =
            Map.of("curve", nested(Map.of("tangent", direct(Role.UNTOUCHED))));

    record Curve(BlockPos start, Vec3 tangent) {
    }

    record CurveProbePayload(Curve curve, BlockPos marker) implements CustomPacketPayload {
        static final Type<CurveProbePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "curve_probe"));

        @Override
        public Type<CurveProbePayload> type() {
            return TYPE;
        }
    }

    record Motion(Vec3 side, BlockPos home) {
    }

    record Arm(Motion motion) {
    }

    record ArmsProbePayload(List<Arm> arms) implements CustomPacketPayload {
        static final Type<ArmsProbePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "arms_probe"));

        @Override
        public Type<ArmsProbePayload> type() {
            return TYPE;
        }
    }

    record SpanProbePayload(BlockPos from, Optional<Curve> to) implements CustomPacketPayload {
        static final Type<SpanProbePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "span_probe"));

        @Override
        public Type<SpanProbePayload> type() {
            return TYPE;
        }
    }

    record PairProbePayload(BlockPos from, BlockPos to) implements CustomPacketPayload {
        static final Type<PairProbePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "pair_probe"));

        @Override
        public Type<PairProbePayload> type() {
            return TYPE;
        }
    }

    record SizedProbePayload(BlockPos origin, BlockPos size, BlockPos marker) implements CustomPacketPayload {
        static final Type<SizedProbePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "sized_probe"));

        @Override
        public Type<SizedProbePayload> type() {
            return TYPE;
        }
    }

    record HeldPairProbePayload(Optional<BlockPos> first, BlockPos anchor, BlockPos last)
            implements CustomPacketPayload {
        static final Type<HeldPairProbePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "held_pair_probe"));

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

    private static ComponentRole direct(Role role) {
        return new ComponentRole.Direct(role);
    }

    private static ComponentRole nested(Map<String, ComponentRole> components) {
        return new ComponentRole.Nested(components);
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
                Map.of("first", direct(Role.GROUP), "anchor", direct(Role.GROUP), "last", direct(Role.GROUP))));

        assertEquals(new HeldPairProbePayload(Optional.empty(), SERVER_BLOCK, SERVER_PAST_MIN_BLOCK),
                toServer(new HeldPairProbePayload(Optional.empty(), CLIENT_BLOCK, CLIENT_INSIDE_BLOCK)));
    }

    @Test
    void aNameTheRecordLacksIsSkippedAndTheRestKept() {
        RecordPayloadFold.declare(Set.of(), Map.of(PairProbePayload.TYPE.id(),
                Map.of("from", direct(Role.GROUP), "to", direct(Role.GROUP), "middle", direct(Role.UNTOUCHED))));

        assertEquals(new PairProbePayload(SERVER_BLOCK, SERVER_PAST_MIN_BLOCK),
                toServer(new PairProbePayload(CLIENT_BLOCK, CLIENT_INSIDE_BLOCK)));
    }

    @Test
    void aNestedRecordWithNoRowFoldsEveryPositionInIt() {
        CurveProbePayload folded = (CurveProbePayload) toServer(
                new CurveProbePayload(new Curve(CLIENT_BLOCK, CLIENT_DIRECTION), CLIENT_BLOCK));

        assertNotEquals(CLIENT_DIRECTION, folded.curve().tangent());
    }

    @Test
    void aNestedUntouchedComponentCrossesAsSentWhileItsSiblingFoldsBothWays() {
        RecordPayloadFold.declare(Set.of(), Map.of(CurveProbePayload.TYPE.id(), UNTOUCHED_TANGENT_ROW));

        assertEquals(new CurveProbePayload(new Curve(SERVER_BLOCK, CLIENT_DIRECTION), SERVER_BLOCK),
                toServer(new CurveProbePayload(new Curve(CLIENT_BLOCK, CLIENT_DIRECTION), CLIENT_BLOCK)));
        assertEquals(new CurveProbePayload(new Curve(CLIENT_BLOCK, CLIENT_DIRECTION), CLIENT_BLOCK),
                toClient(new CurveProbePayload(new Curve(SERVER_BLOCK, CLIENT_DIRECTION), SERVER_BLOCK)));
    }

    @Test
    void aRowReachesTwoLevelsDownInsideEachRecordOfAList() {
        RecordPayloadFold.declare(Set.of(), Map.of(ArmsProbePayload.TYPE.id(),
                Map.of("arms", nested(Map.of("motion", nested(Map.of("side", direct(Role.UNTOUCHED))))))));

        assertEquals(new ArmsProbePayload(List.of(new Arm(new Motion(CLIENT_DIRECTION, SERVER_BLOCK)),
                        new Arm(new Motion(CLIENT_DIRECTION, SERVER_BLOCK)))),
                toServer(new ArmsProbePayload(List.of(new Arm(new Motion(CLIENT_DIRECTION, CLIENT_BLOCK)),
                        new Arm(new Motion(CLIENT_DIRECTION, CLIENT_BLOCK))))));
    }

    @Test
    void aGroupSpanningTheTopAndANestedRecordMovesByOneShift() {
        RecordPayloadFold.declare(Set.of(), Map.of(SpanProbePayload.TYPE.id(),
                Map.of("from", direct(Role.GROUP), "to", nested(Map.of("start", direct(Role.GROUP))))));

        assertEquals(new SpanProbePayload(SERVER_BLOCK, Optional.of(new Curve(SERVER_PAST_MIN_BLOCK, Vec3.ZERO))),
                toServer(new SpanProbePayload(CLIENT_BLOCK, Optional.of(new Curve(CLIENT_INSIDE_BLOCK, Vec3.ZERO)))));
    }

    @Test
    void aNestedNameTheRecordLacksIsSkippedAndTheRestKept() {
        RecordPayloadFold.declare(Set.of(), Map.of(CurveProbePayload.TYPE.id(),
                Map.of("curve", nested(Map.of("tangent", direct(Role.UNTOUCHED), "bend", direct(Role.UNTOUCHED))))));

        assertEquals(new CurveProbePayload(new Curve(SERVER_BLOCK, CLIENT_DIRECTION), SERVER_BLOCK),
                toServer(new CurveProbePayload(new Curve(CLIENT_BLOCK, CLIENT_DIRECTION), CLIENT_BLOCK)));
    }

    @Test
    void aNestedObjectOnAComponentHoldingNoRecordIsSkippedAndItFoldsByType() {
        RecordPayloadFold.declare(Set.of(), Map.of(CurveProbePayload.TYPE.id(),
                Map.of("marker", nested(Map.of("x", direct(Role.UNTOUCHED))))));

        assertEquals(new CurveProbePayload(new Curve(SERVER_BLOCK, Vec3.atLowerCornerOf(SERVER_BLOCK)), SERVER_BLOCK),
                toServer(new CurveProbePayload(new Curve(CLIENT_BLOCK, CLIENT_DIRECTION), CLIENT_BLOCK)));
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
