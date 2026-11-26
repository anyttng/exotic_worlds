package com.exoticworlds.bridge.v2.net;

import java.util.function.BiFunction;

import com.exoticworlds.api.v2.net.ParticleRewriterV2;
import com.exoticworlds.api.v2.net.SeamContextV2;
import com.exoticworlds.engine.net.PacketTranslator;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public final class NetBridge {

    public static <P extends CustomPacketPayload> void registerClientboundPayload(Class<P> payloadType,
            BiFunction<P, SeamContextV2, CustomPacketPayload> rewriter) {
        PacketTranslator.registerClientboundPayloadRewriter(payloadType,
                (payload, context) -> rewriter.apply(payload, new SeamContextView(context)));
    }

    public static <P extends CustomPacketPayload> void registerServerboundPayload(Class<P> payloadType,
            BiFunction<P, SeamContextV2, CustomPacketPayload> rewriter) {
        PacketTranslator.registerServerboundPayloadRewriter(payloadType,
                (payload, context) -> rewriter.apply(payload, new SeamContextView(context)));
    }

    public static <P extends Packet<ClientGamePacketListener>> void registerClientboundPacket(Class<P> packetType,
            BiFunction<P, SeamContextV2, Packet<ClientGamePacketListener>> rewriter) {
        PacketTranslator.registerClientboundPacketRewriter(packetType,
                (packet, context) -> rewriter.apply(packet, new SeamContextView(context)));
    }

    public static <P extends Packet<ServerGamePacketListener>> void registerServerboundPacket(Class<P> packetType,
            BiFunction<P, SeamContextV2, Packet<ServerGamePacketListener>> rewriter) {
        PacketTranslator.registerServerboundPacketRewriter(packetType,
                (packet, context) -> rewriter.apply(packet, new SeamContextView(context)));
    }

    public static <P extends ParticleOptions> void registerParticle(Class<P> particleType,
            ParticleRewriterV2<P> rewriter) {
        PacketTranslator.registerParticleRewriter(particleType,
                (particle, context, clientOrigin) -> rewriter.rewrite(particle, new SeamContextView(context),
                        clientOrigin));
    }

    private NetBridge() {
    }
}
