package com.exoticworlds.engine.net;

import static com.exoticworlds.compat.CompatFoldFixture.PER_AXIS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cobblemon.mod.common.entity.PlatformType;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.npc.NPCPlayerModelType;
import com.cobblemon.mod.common.entity.npc.NPCPlayerTexture;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnExtraDataEntityPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnGenericBedrockPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnNPCPacket;
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokemonPacket;
import com.cobblemon.mod.common.pokemon.Gender;
import com.exoticworlds.compat.cobblemon.CobblemonPackets;
import com.exoticworlds.engine.seam.ClientPosition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class CobblemonSpawnTranslationTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static final int VIEW_DISTANCE = 16;
    private static final int BUFFER_CAPACITY = 256;

    private static final double PLAYER_X = 200.0;
    private static final double PLAYER_Z = 8.0;

    private static final double SERVER_X = -231.5;
    private static final double CLIENT_X = 280.5;
    private static final double INLAND_X = 200.5;
    private static final double ENTITY_Y = 64.0;
    private static final double ENTITY_Z = 8.5;

    private static final int ENTITY_ID = 42;
    private static final UUID ENTITY_UUID = new UUID(1L, 2L);
    private static final UUID POKEMON_UUID = new UUID(3L, 4L);

    private static final String NAMESPACE = "cobblemon";
    private static final ResourceLocation SPECIES_ID = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "bulbasaur");
    private static final ResourceLocation BALL_ID = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "poke_ball");
    private static final ResourceLocation NPC_CLASS_ID = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "standard");
    private static final ResourceLocation CATEGORY_ID = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "category");
    private static final String FORM_NAME = "";
    private static final String NPC_NAME = "npc";

    private static final float UNIT_SCALE = 1.0F;
    private static final int NO_PHASING_TARGET = -1;
    private static final byte NO_BEAM = 0;
    private static final int LABEL_LEVEL = 5;
    private static final int NPC_LEVEL = 1;
    private static final float NPC_HITBOX_WIDTH = 0.6F;
    private static final float NPC_HITBOX_HEIGHT = 1.8F;
    private static final float NPC_EYES_HEIGHT = 1.62F;

    @BeforeAll
    static void registerTheCobblemonRewriters() {
        CobblemonPackets.register();
    }

    @Test
    void everySpawnPayloadCarriesItsEntityOntoTheCopyTheClientHolds() {
        for (SpawnExtraDataEntityPacket<?, ?> payload : payloadsAt(SERVER_X)) {
            String name = payload.getClass().getSimpleName();
            SpawnExtraDataEntityPacket<?, ?> sent = (SpawnExtraDataEntityPacket<?, ?>) seated(payload);
            ClientboundAddEntityPacket spawn = sent.getVanillaSpawnPacket();
            assertEquals(CLIENT_X, spawn.getX(), name);
            assertEquals(ENTITY_Y, spawn.getY(), name);
            assertEquals(ENTITY_Z, spawn.getZ(), name);
            assertEquals(ENTITY_ID, spawn.getId(), name);
            assertArrayEquals(entityData(payload), entityData(sent), name);
            assertEquals(SERVER_X, payload.getVanillaSpawnPacket().getX(), name);
        }
    }

    @Test
    void aSpawnPayloadInThePlayersOwnFrameTravelsUntouched() {
        for (SpawnExtraDataEntityPacket<?, ?> payload : payloadsAt(INLAND_X)) {
            assertSame(payload, seated(payload), payload.getClass().getSimpleName());
        }
    }

    private static List<SpawnExtraDataEntityPacket<?, ?>> payloadsAt(double x) {
        return List.of(
                new SpawnPokemonPacket(null, POKEMON_UUID, null, UNIT_SCALE, SPECIES_ID, Gender.MALE, false,
                        FORM_NAME, Set.of(), null, NO_PHASING_TARGET, NO_BEAM, PlatformType.NONE, null, null,
                        LABEL_LEVEL, PoseType.STAND, false, false, BALL_ID, 0.0F, 0, 0.0F, 0, Map.of(), 0.0F,
                        false, false, List.of(), spawnAt(x)),
                new SpawnNPCPacket(NPC_CLASS_ID, NPC_CLASS_ID, Set.of(), NPC_LEVEL, Set.of(),
                        Component.literal(NPC_NAME), PoseType.STAND,
                        new NPCPlayerTexture(new byte[0], NPCPlayerModelType.NONE), false, NPC_HITBOX_WIDTH,
                        NPC_HITBOX_HEIGHT, NPC_EYES_HEIGHT, UNIT_SCALE, UNIT_SCALE, spawnAt(x)),
                new SpawnGenericBedrockPacket(CATEGORY_ID, Set.of(), PoseType.STAND, UNIT_SCALE, UNIT_SCALE,
                        UNIT_SCALE, 0, spawnAt(x)));
    }

    private static ClientboundAddEntityPacket spawnAt(double x) {
        return new ClientboundAddEntityPacket(ENTITY_ID, ENTITY_UUID, x, ENTITY_Y, ENTITY_Z, 0.0F, 0.0F,
                EntityType.PIG, 0, Vec3.ZERO, 0.0);
    }

    private static CustomPacketPayload seated(CustomPacketPayload payload) {
        ClientboundCustomPayloadPacket sent = new ClientboundCustomPayloadPacket(payload);
        return ((ClientboundCustomPayloadPacket) PacketTranslator.toClient(sent, context())).payload();
    }

    private static TranslationContext context() {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(PLAYER_X, PLAYER_Z, Level.OVERWORLD, null, PER_AXIS);
        return new TranslationContext(PER_AXIS, mirror, CobblemonSpawnTranslationTest::buffer, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, entityId -> false, entityId -> null, entityId -> null, () -> {},
                PacketTranslator.production());
    }

    private static byte[] entityData(SpawnExtraDataEntityPacket<?, ?> payload) {
        RegistryFriendlyByteBuf buffer = buffer(BUFFER_CAPACITY);
        payload.encodeEntityData(buffer);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    private static RegistryFriendlyByteBuf buffer(int capacity) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);
    }
}
