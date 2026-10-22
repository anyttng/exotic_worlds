package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.api.v1.net.PacketRewriters;
import com.exoticworlds.engine.net.RecordPayloadFold.Role;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.Identifier;

class ModPacketTranslatorTest {
    private static final Map<String, Role> UNTOUCHED_OFFSET_ROW = Map.of("offset", Role.UNTOUCHED);

    record ClientboundProbePacket(BlockPos pos, BlockPos offset) implements Packet<ClientGamePacketListener> {
        static final PacketType<ClientboundProbePacket> TYPE = new PacketType<>(PacketFlow.CLIENTBOUND,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "clientbound_probe"));

        @Override
        public PacketType<ClientboundProbePacket> type() {
            return TYPE;
        }

        @Override
        public void handle(ClientGamePacketListener listener) {
        }
    }

    record ServerboundProbePacket(BlockPos pos, BlockPos offset) implements Packet<ServerGamePacketListener> {
        static final PacketType<ServerboundProbePacket> TYPE = new PacketType<>(PacketFlow.SERVERBOUND,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "serverbound_probe"));

        @Override
        public PacketType<ServerboundProbePacket> type() {
            return TYPE;
        }

        @Override
        public void handle(ServerGamePacketListener listener) {
        }
    }

    record VanillaProbePacket(BlockPos pos) implements Packet<ClientGamePacketListener> {
        static final PacketType<VanillaProbePacket> TYPE =
                new PacketType<>(PacketFlow.CLIENTBOUND, Identifier.withDefaultNamespace("vanilla_probe"));

        @Override
        public PacketType<VanillaProbePacket> type() {
            return TYPE;
        }

        @Override
        public void handle(ClientGamePacketListener listener) {
        }
    }

    record RewrittenProbePacket(BlockPos pos) implements Packet<ClientGamePacketListener> {
        static final PacketType<RewrittenProbePacket> TYPE = new PacketType<>(PacketFlow.CLIENTBOUND,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "rewritten_probe"));

        @Override
        public PacketType<RewrittenProbePacket> type() {
            return TYPE;
        }

        @Override
        public void handle(ClientGamePacketListener listener) {
        }
    }

    @BeforeAll
    static void registerTheRewrittenProbeThroughThePublicSeamAlone() {
        PacketRewriters.registerClientboundPacket(RewrittenProbePacket.class,
                (packet, context) -> new RewrittenProbePacket(BlockPos.ZERO));
    }

    @AfterEach
    void clearTheRows() {
        RecordPayloadFold.declare(Set.of(), Map.of());
    }

    @Test
    void clientboundModPacketMovesItsPositionsToTheClientFrame() {
        assertEquals(new ClientboundProbePacket(CLIENT_BLOCK, CLIENT_BLOCK), PacketTranslator.toClient(
                new ClientboundProbePacket(SERVER_BLOCK, SERVER_BLOCK), context()));
    }

    @Test
    void serverboundModPacketReturnsItsPositionsToTheServerFrame() {
        assertEquals(new ServerboundProbePacket(SERVER_BLOCK, SERVER_BLOCK), PacketTranslator.toServer(
                new ServerboundProbePacket(CLIENT_BLOCK, CLIENT_BLOCK), context()));
    }

    @Test
    void modPacketInsideABundleMovesToTheClientFrame() {
        ClientboundBundlePacket translated = (ClientboundBundlePacket) PacketTranslator.toClient(
                new ClientboundBundlePacket(List.of(new ClientboundProbePacket(SERVER_BLOCK, SERVER_BLOCK))),
                context());

        assertEquals(List.of(new ClientboundProbePacket(CLIENT_BLOCK, CLIENT_BLOCK)), translated.subPackets());
    }

    @Test
    void aRowKeyedByThePacketIdLeavesItsUntouchedComponent() {
        RecordPayloadFold.declare(Set.of(), Map.of(ClientboundProbePacket.TYPE.id(), UNTOUCHED_OFFSET_ROW,
                ServerboundProbePacket.TYPE.id(), UNTOUCHED_OFFSET_ROW));

        assertEquals(new ClientboundProbePacket(CLIENT_BLOCK, SERVER_BLOCK), PacketTranslator.toClient(
                new ClientboundProbePacket(SERVER_BLOCK, SERVER_BLOCK), context()));
        assertEquals(new ServerboundProbePacket(SERVER_BLOCK, CLIENT_BLOCK), PacketTranslator.toServer(
                new ServerboundProbePacket(CLIENT_BLOCK, CLIENT_BLOCK), context()));
    }

    @Test
    void aDeniedPacketCrossesUntouchedBothWays() {
        RecordPayloadFold.declare(Set.of(ClientboundProbePacket.TYPE.id(), ServerboundProbePacket.TYPE.id()),
                Map.of());
        ClientboundProbePacket clientbound = new ClientboundProbePacket(SERVER_BLOCK, SERVER_BLOCK);
        ServerboundProbePacket serverbound = new ServerboundProbePacket(CLIENT_BLOCK, CLIENT_BLOCK);

        assertSame(clientbound, PacketTranslator.toClient(clientbound, context()));
        assertSame(serverbound, PacketTranslator.toServer(serverbound, context()));
    }

    @Test
    void aVanillaPacketWithNoRewriterPassesUnchanged() {
        VanillaProbePacket packet = new VanillaProbePacket(SERVER_BLOCK);

        assertSame(packet, PacketTranslator.toClient(packet, context()));
    }

    @Test
    void aRegisteredRewriterTakesPrecedenceOverTheRecordFold() {
        assertEquals(new RewrittenProbePacket(BlockPos.ZERO),
                PacketTranslator.toClient(new RewrittenProbePacket(SERVER_BLOCK), context()));
    }
}
