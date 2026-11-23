package com.exoticworlds.api.v2.net;

import java.util.function.BiFunction;

import com.exoticworlds.bridge.v2.net.NetBridge;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

/**
 * Where a mod says how its own packets cross the seam. Every vanilla packet carrying a world position is already
 * rewritten between the server's frame and the client's; a payload, a game packet or a particle type this mod never
 * heard of is not, because nothing but its author knows which of its fields is a position.
 *
 * <p>Register from the mod's initializer. The table closes at {@code MinecraftServer.runServer}, before the levels
 * load, and a later registration throws rather than being silently half-effective. A registered rewriter runs for
 * every payload or packet of that exact class, on a folding level and nowhere else.</p>
 *
 * <p>These registrations share one table with {@link com.exoticworlds.api.v1.net.PacketRewriters}: a class
 * registered through both throws on the second registration.</p>
 *
 * <p>A rewriter is handed a {@link SeamContextV2} for the connection the packet is on, and returns the value to send,
 * or the argument itself where nothing moved, which costs nothing.</p>
 */
public final class PacketRewritersV2 {

    /**
     * How a payload of this mod's own is rewritten on its way to a client. Move every world position it carries
     * with {@link SeamContextV2#toClient}, or the client draws it a world away from where it belongs.
     */
    public static <P extends CustomPacketPayload> void registerClientboundPayload(Class<P> payloadType,
            BiFunction<P, SeamContextV2, CustomPacketPayload> rewriter) {
        NetBridge.registerClientboundPayload(payloadType, rewriter);
    }

    /**
     * How a payload of this mod's own is folded back on its way in from a client. A position the client sent is in
     * whatever copy that client holds; {@link SeamContextV2#toServer} is what makes it the position the server means.
     */
    public static <P extends CustomPacketPayload> void registerServerboundPayload(Class<P> payloadType,
            BiFunction<P, SeamContextV2, CustomPacketPayload> rewriter) {
        NetBridge.registerServerboundPayload(payloadType, rewriter);
    }

    /**
     * How a game packet of this mod's own is rewritten on its way to a client: a packet the mod adds to the game
     * protocol itself rather than sending as a payload. A record packet outside the {@code minecraft} namespace is
     * already moved by the declared types of its components; register only where that moves the wrong ones.
     */
    public static <P extends Packet<ClientGamePacketListener>> void registerClientboundPacket(Class<P> packetType,
            BiFunction<P, SeamContextV2, Packet<ClientGamePacketListener>> rewriter) {
        NetBridge.registerClientboundPacket(packetType, rewriter);
    }

    /** How a game packet of this mod's own is folded back on its way in from a client. */
    public static <P extends Packet<ServerGamePacketListener>> void registerServerboundPacket(Class<P> packetType,
            BiFunction<P, SeamContextV2, Packet<ServerGamePacketListener>> rewriter) {
        NetBridge.registerServerboundPacket(packetType, rewriter);
    }

    /** How a particle type of this mod's own is rewritten on its way to a client. */
    public static <P extends ParticleOptions> void registerParticle(Class<P> particleType,
            ParticleRewriterV2<P> rewriter) {
        NetBridge.registerParticle(particleType, rewriter);
    }

    private PacketRewritersV2() {
    }
}
