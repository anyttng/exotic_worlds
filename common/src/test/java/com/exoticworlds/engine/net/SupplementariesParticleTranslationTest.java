package com.exoticworlds.engine.net;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.exoticworlds.compat.supplementaries.SupplementariesPackets;
import com.exoticworlds.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundParticlePacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class SupplementariesParticleTranslationTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final int VIEW_DISTANCE = 16;

    private static final double PLAYER_X = 200.0;
    private static final double PLAYER_Z = 8.0;

    private static final double SERVER_X = -231.5;
    private static final double CLIENT_X = 280.5;
    private static final double INLAND_X = 200.5;
    private static final double NEARBY_X = 210.5;
    private static final double EFFECT_Y = 64.0;
    private static final double EFFECT_Z = 8.5;

    private static final Vec3 CONFETTI_DIRECTION = new Vec3(0.25, 1.0, -0.5);
    private static final int CONFETTI_COLOR = 0xFF4080;

    @BeforeAll
    static void registerTheSupplementariesRewriters() {
        SupplementariesPackets.register();
    }

    @Test
    void aPearlTrailWhoseStartStaysEndsBesideIt() {
        ClientBoundParticlePacket sent = seated(pearl(INLAND_X, SERVER_X));
        assertEquals(at(INLAND_X), sent.pos);
        assertEquals(at(CLIENT_X), sent.dir);
        assertEquals(ClientBoundParticlePacket.Kind.PEARL_TELEPORT, sent.type);
    }

    @Test
    void aPearlTrailInThePlayersOwnFrameTravelsUntouched() {
        ClientBoundParticlePacket raw = pearl(INLAND_X, NEARBY_X);
        assertSame(raw, seated(raw));
    }

    @Test
    void aDirectionKeepsItsValueWhenItsPositionMoves() {
        ClientBoundParticlePacket sent = seated(new ClientBoundParticlePacket(at(SERVER_X),
                ClientBoundParticlePacket.Kind.CONFETTI, CONFETTI_DIRECTION, CONFETTI_COLOR));
        assertEquals(at(CLIENT_X), sent.pos);
        assertEquals(CONFETTI_DIRECTION, sent.dir);
        assertEquals(List.of(CONFETTI_COLOR), sent.getExtraData());
    }

    private static ClientBoundParticlePacket pearl(double fromX, double toX) {
        return new ClientBoundParticlePacket(at(fromX), ClientBoundParticlePacket.Kind.PEARL_TELEPORT, at(toX));
    }

    private static Vec3 at(double x) {
        return new Vec3(x, EFFECT_Y, EFFECT_Z);
    }

    private static ClientBoundParticlePacket seated(CustomPacketPayload payload) {
        ClientboundCustomPayloadPacket sent = new ClientboundCustomPayloadPacket(payload);
        return (ClientBoundParticlePacket) ((ClientboundCustomPayloadPacket) PacketTranslator.toClient(sent,
                context())).payload();
    }

    private static TranslationContext context() {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(PLAYER_X, PLAYER_Z, Level.OVERWORLD, null, PER_AXIS);
        return new TranslationContext(PER_AXIS, mirror, SupplementariesParticleTranslationTest::buffer,
                Level.OVERWORLD, VIEW_DISTANCE, VIEW_DISTANCE, entityId -> null, entityId -> null, () -> {});
    }

    private static RegistryFriendlyByteBuf buffer(int capacity) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);
    }
}
