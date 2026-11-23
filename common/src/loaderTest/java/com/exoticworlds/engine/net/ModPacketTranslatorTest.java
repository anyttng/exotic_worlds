package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.Identifier;

class ModPacketTranslatorTest {
    private static final Map<String, Role> UNTOUCHED_OFFSET_ROW = Map.of("offset", Role.UNTOUCHED);
    private static final double PARTICLE_Y = 64.0;

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

    record GeometryProbePacket(BlockPos pos) implements Packet<ClientGamePacketListener> {
        static final PacketType<GeometryProbePacket> TYPE = new PacketType<>(PacketFlow.CLIENTBOUND,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "geometry_probe"));

        @Override
        public PacketType<GeometryProbePacket> type() {
            return TYPE;
        }

        @Override
        public void handle(ClientGamePacketListener listener) {
        }
    }

    record TwiceRegisteredProbePacket(BlockPos pos) implements Packet<ClientGamePacketListener> {
        static final PacketType<TwiceRegisteredProbePacket> TYPE = new PacketType<>(PacketFlow.CLIENTBOUND,
                Identifier.fromNamespaceAndPath(ExoticWorlds.MODID, "twice_registered_probe"));

        @Override
        public PacketType<TwiceRegisteredProbePacket> type() {
            return TYPE;
        }

        @Override
        public void handle(ClientGamePacketListener listener) {
        }
    }

    record ProbeParticle(BlockPos target) implements ParticleOptions {
        @Override
        public ParticleType<?> getType() {
            return ParticleTypes.FLAME;
        }
    }

    record GeometryProbeParticle(BlockPos target) implements ParticleOptions {
        @Override
        public ParticleType<?> getType() {
            return ParticleTypes.FLAME;
        }
    }

    @BeforeAll
    static void registerTheRewrittenProbeThroughThePublicSeamAlone() {
        PacketRewriters.registerClientboundPacket(RewrittenProbePacket.class,
                (packet, context) -> new RewrittenProbePacket(BlockPos.ZERO));
        com.exoticworlds.api.v2.net.PacketRewritersV2.registerClientboundPacket(GeometryProbePacket.class,
                (packet, context) -> new GeometryProbePacket(context.geometry().fold(context.toClient(packet.pos()))));
        PacketRewriters.registerClientboundPacket(TwiceRegisteredProbePacket.class, (packet, context) -> packet);
        PacketRewriters.registerParticle(ProbeParticle.class,
                (particle, context, clientOrigin) -> new ProbeParticle(context.toClient(particle.target())));
        com.exoticworlds.api.v2.net.PacketRewritersV2.registerParticle(GeometryProbeParticle.class,
                (particle, context, clientOrigin) -> new GeometryProbeParticle(
                        context.geometry().fold(particle.target())));
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

    @Test
    void aV2RewriterRunsAndReadsTheGeometryFromItsContext() {
        assertEquals(new GeometryProbePacket(SERVER_BLOCK),
                PacketTranslator.toClient(new GeometryProbePacket(SERVER_BLOCK), context()));
    }

    @Test
    void aPacketRegisteredThroughV1AndV2IsRefused() {
        assertThrows(IllegalStateException.class, () ->
                com.exoticworlds.api.v2.net.PacketRewritersV2.registerClientboundPacket(
                        TwiceRegisteredProbePacket.class, (packet, context) -> packet));
    }

    @Test
    void aV1ParticleRewriterStillMovesItsTarget() {
        assertEquals(new ProbeParticle(CLIENT_BLOCK), particleOf(new ProbeParticle(SERVER_BLOCK)));
    }

    @Test
    void aV2ParticleRewriterRunsAndReadsTheGeometryFromItsContext() {
        assertEquals(new GeometryProbeParticle(SERVER_BLOCK), particleOf(new GeometryProbeParticle(CLIENT_BLOCK)));
    }

    @Test
    void aParticleRegisteredThroughV1AndV2IsRefused() {
        assertThrows(IllegalStateException.class, () ->
                com.exoticworlds.api.v2.net.PacketRewritersV2.registerParticle(
                        ProbeParticle.class, (particle, context, clientOrigin) -> particle));
    }

    private static ParticleOptions particleOf(ParticleOptions particle) {
        ClientboundLevelParticlesPacket translated = (ClientboundLevelParticlesPacket) PacketTranslator.toClient(
                new ClientboundLevelParticlesPacket(particle, false, false,
                        SERVER_X, PARTICLE_Y, SERVER_Z, 0.0F, 0.0F, 0.0F, 0.0F, 1),
                context());
        return translated.particle();
    }
}
