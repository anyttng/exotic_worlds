package com.exoticworlds.engine.net;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.net.messages.client.effect.PokeSnackBlockParticlesPacket;
import com.cobblemon.mod.common.net.messages.client.effect.SaccharineLogBlockParticlesPacket;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket;
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket;
import com.exoticworlds.compat.cobblemon.CobblemonPackets;
import com.exoticworlds.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class CobblemonEffectTranslationTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final int VIEW_DISTANCE = 16;

    private static final double PLAYER_X = 200.0;
    private static final double PLAYER_Z = 8.0;

    private static final double SERVER_X = -231.5;
    private static final double CLIENT_X = 280.5;
    private static final double INLAND_X = 200.5;
    private static final double EFFECT_Y = 64.0;
    private static final double EFFECT_Z = 8.5;

    private static final int ENTITY_ID = 42;

    private static final String NAMESPACE = "cobblemon";
    private static final ResourceLocation SOUND_ID = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "cry");
    private static final ResourceLocation EFFECT_ID = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "splash");
    private static final float VOLUME = 0.8F;
    private static final float PITCH = 1.2F;
    private static final Vec3 HIT_DIRECTION = new Vec3(0.6, 0.0, 0.8);

    @BeforeAll
    static void registerTheCobblemonRewriters() {
        CobblemonPackets.register();
    }

    @Test
    void aSoundReachesTheCopyTheClientHolds() {
        UnvalidatedPlaySoundS2CPacket sent = (UnvalidatedPlaySoundS2CPacket) seated(soundAt(SERVER_X));
        assertEquals(CLIENT_X, sent.getX());
        assertEquals(EFFECT_Y, sent.getY());
        assertEquals(EFFECT_Z, sent.getZ());
        assertEquals(SOUND_ID, sent.getSound());
        assertEquals(SoundSource.NEUTRAL, sent.getCategory());
        assertEquals(VOLUME, sent.getVolume());
        assertEquals(PITCH, sent.getPitch());
    }

    @Test
    void aSnowstormParticleReachesTheCopyTheClientHolds() {
        SpawnSnowstormParticlePacket sent = (SpawnSnowstormParticlePacket) seated(
                new SpawnSnowstormParticlePacket(EFFECT_ID, new Vec3(SERVER_X, EFFECT_Y, EFFECT_Z)));
        assertEquals(new Vec3(CLIENT_X, EFFECT_Y, EFFECT_Z), sent.getPosition());
        assertEquals(EFFECT_ID, sent.getEffectId());
    }

    @Test
    void blockParticlesReachTheCopyTheClientHolds() {
        BlockPos server = BlockPos.containing(SERVER_X, EFFECT_Y, EFFECT_Z);
        BlockPos client = BlockPos.containing(CLIENT_X, EFFECT_Y, EFFECT_Z);

        PokeSnackBlockParticlesPacket snack = (PokeSnackBlockParticlesPacket) seated(
                new PokeSnackBlockParticlesPacket(server, server.above()));
        assertEquals(client, snack.getBlockPos());
        assertEquals(client.above(), snack.getEntityPos());

        SaccharineLogBlockParticlesPacket log = (SaccharineLogBlockParticlesPacket) seated(
                new SaccharineLogBlockParticlesPacket(server, null));
        assertEquals(client, log.getBlockPos());
        assertNull(log.getEntityPos());
    }

    @Test
    void anEffectInThePlayersOwnFrameTravelsUntouched() {
        BlockPos inland = BlockPos.containing(INLAND_X, EFFECT_Y, EFFECT_Z);
        for (CustomPacketPayload payload : List.of(
                soundAt(INLAND_X),
                new SpawnSnowstormParticlePacket(EFFECT_ID, new Vec3(INLAND_X, EFFECT_Y, EFFECT_Z)),
                new PokeSnackBlockParticlesPacket(inland, inland.above()),
                new SaccharineLogBlockParticlesPacket(inland, null))) {
            assertSame(payload, seated(payload), payload.getClass().getSimpleName());
        }
    }

    @Test
    void theBallsHitTargetMovesAndItsHitDirectionStays() {
        EntityDataAccessor<Vec3> target = EmptyPokeBallEntity.Companion.getHIT_TARGET_POSITION();
        EntityDataAccessor<Vec3> direction = EmptyPokeBallEntity.Companion.getHIT_VELOCITY();
        Vec3 hit = new Vec3(SERVER_X, EFFECT_Y, EFFECT_Z);
        ClientboundSetEntityDataPacket raw = new ClientboundSetEntityDataPacket(ENTITY_ID, List.of(
                SynchedEntityData.DataValue.create(target, hit),
                SynchedEntityData.DataValue.create(direction, HIT_DIRECTION)));

        TranslationContext context = context(entityId -> hit,
                entityId -> new TagPositions.Subject(EmptyPokeBallEntity.class, null));
        ClientboundSetEntityDataPacket sent = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(raw, context);

        assertEquals(new Vec3(CLIENT_X, EFFECT_Y, EFFECT_Z), sent.packedItems().get(0).value());
        assertSame(raw.packedItems().get(1), sent.packedItems().get(1));
    }

    private static UnvalidatedPlaySoundS2CPacket soundAt(double x) {
        return new UnvalidatedPlaySoundS2CPacket(SOUND_ID, SoundSource.NEUTRAL, x, EFFECT_Y, EFFECT_Z, VOLUME, PITCH);
    }

    private static CustomPacketPayload seated(CustomPacketPayload payload) {
        ClientboundCustomPayloadPacket sent = new ClientboundCustomPayloadPacket(payload);
        return ((ClientboundCustomPayloadPacket) PacketTranslator.toClient(sent,
                context(entityId -> null, entityId -> null))).payload();
    }

    private static TranslationContext context(IntFunction<Vec3> entityPosition,
            IntFunction<TagPositions.Subject> entity) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(PLAYER_X, PLAYER_Z, Level.OVERWORLD, null, PER_AXIS);
        return new TranslationContext(PER_AXIS, mirror, CobblemonEffectTranslationTest::buffer, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityPosition, entity, () -> {},
                PacketTranslator.production());
    }

    private static RegistryFriendlyByteBuf buffer(int capacity) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);
    }
}
