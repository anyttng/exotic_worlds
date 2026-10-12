package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class PacketTranslatorEntityTest {
    @Test
    void addEntityUnwrapsAroundTheMirror() {
        ClientboundAddEntityPacket translated = (ClientboundAddEntityPacket) PacketTranslator.toClient(
                new ClientboundAddEntityPacket(11, new UUID(1L, 2L), SERVER_X, 70.0, SERVER_Z,
                        0.0F, 0.0F, EntityType.PIG, 0, Vec3.ZERO, 0.0),
                context());

        assertEquals(CLIENT_X, translated.getX());
        assertEquals(70.0, translated.getY());
        assertEquals(CLIENT_Z, translated.getZ());
        assertEquals(11, translated.getId());
    }

    @Test
    void teleportOfTheOwnControlledVehicleIsDropped() {
        ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(
                42, new PositionMoveRotation(new Vec3(SERVER_X, 70.0, SERVER_Z), Vec3.ZERO, 0.0F, 0.0F),
                Set.of(), false);

        assertNull(PacketTranslator.toClient(packet, context(entityId -> entityId == 42, entityId -> null)));
    }

    @Test
    void teleportTranslatesAbsoluteAxesKeepsRelativeOnesRaw() {
        ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(
                42, new PositionMoveRotation(new Vec3(5.0, 70.0, SERVER_Z), Vec3.ZERO, 30.0F, 10.0F),
                Set.of(Relative.X), true);

        ClientboundTeleportEntityPacket translated =
                (ClientboundTeleportEntityPacket) PacketTranslator.toClient(packet, context());

        assertEquals(new Vec3(5.0, 70.0, CLIENT_Z), translated.change().position());
        assertEquals(Set.of(Relative.X), translated.relatives());
        assertTrue(translated.onGround());
    }

    @Test
    void entityPositionSyncTranslatesTheAbsolutePosition() {
        ClientboundEntityPositionSyncPacket packet = new ClientboundEntityPositionSyncPacket(
                7, new PositionMoveRotation(new Vec3(SERVER_X, 70.0, SERVER_Z), Vec3.ZERO, 0.0F, 0.0F), false);

        ClientboundEntityPositionSyncPacket translated =
                (ClientboundEntityPositionSyncPacket) PacketTranslator.toClient(packet, context());

        assertEquals(new Vec3(CLIENT_X, 70.0, CLIENT_Z), translated.values().position());
    }

    @Test
    void moveVehicleTranslatesThePosition() {
        ClientboundMoveVehiclePacket translated = (ClientboundMoveVehiclePacket) PacketTranslator.toClient(
                new ClientboundMoveVehiclePacket(new Vec3(SERVER_X, 70.0, SERVER_Z), 30.0F, 10.0F), context());

        assertEquals(new Vec3(CLIENT_X, 70.0, CLIENT_Z), translated.position());
    }

    @Test
    void synchedBlockPosFollowsTheEntityOtherValuesPassThrough() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.OPTIONAL_BLOCK_POS, Optional.of(SERVER_CARRIED_BLOCK)),
                new SynchedEntityData.DataValue<>(1, EntityDataSerializers.BYTE, (byte) 6),
                new SynchedEntityData.DataValue<>(2, EntityDataSerializers.OPTIONAL_BLOCK_POS, Optional.empty()),
                new SynchedEntityData.DataValue<>(3, EntityDataSerializers.BLOCK_POS, SERVER_CARRIED_BLOCK)));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        assertEquals(Optional.of(CLIENT_CARRIED_BLOCK), translated.packedItems().get(0).value());
        assertEquals((byte) 6, translated.packedItems().get(1).value());
        assertEquals(Optional.empty(), translated.packedItems().get(2).value());
        assertEquals(CLIENT_CARRIED_BLOCK, translated.packedItems().get(3).value());
    }

    @Test
    void synchedValuesWithoutAPositionComeBackAsTheSameItems() {
        List<SynchedEntityData.DataValue<?>> items = List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte) 6),
                new SynchedEntityData.DataValue<>(1, EntityDataSerializers.INT, 300),
                new SynchedEntityData.DataValue<>(2, EntityDataSerializers.FLOAT, 20.0F),
                new SynchedEntityData.DataValue<>(3, EntityDataSerializers.BOOLEAN, true),
                new SynchedEntityData.DataValue<>(4, EntityDataSerializers.STRING, "label"),
                new SynchedEntityData.DataValue<>(5, EntityDataSerializers.BLOCK_POS, SERVER_CARRIED_BLOCK));

        List<SynchedEntityData.DataValue<?>> translated = ((ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                new ClientboundSetEntityDataPacket(3, items),
                context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)))).packedItems();

        for (int index = 0; index < items.size() - 1; index++) {
            assertSame(items.get(index), translated.get(index));
        }

        assertEquals(CLIENT_CARRIED_BLOCK, translated.getLast().value());
    }

    @Test
    void synchedBlockPosWithoutTheEntityFoldsAroundTheMirror() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.OPTIONAL_BLOCK_POS, Optional.of(SERVER_CARRIED_BLOCK)),
                new SynchedEntityData.DataValue<>(1, EntityDataSerializers.BLOCK_POS, SERVER_CARRIED_BLOCK)));

        ClientboundSetEntityDataPacket translated =
                (ClientboundSetEntityDataPacket) PacketTranslator.toClient(packet, context());

        assertEquals(Optional.of(MIRROR_CARRIED_BLOCK), translated.packedItems().get(0).value());
        assertEquals(MIRROR_CARRIED_BLOCK, translated.packedItems().get(1).value());
    }

    @Test
    void synchedGlobalPosWithoutTheEntityFoldsAroundTheMirrorInItsOwnDimension() {
        GlobalPos elsewhere = GlobalPos.of(Level.NETHER, SERVER_CARRIED_BLOCK);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.OPTIONAL_GLOBAL_POS,
                        Optional.of(GlobalPos.of(Level.OVERWORLD, SERVER_CARRIED_BLOCK))),
                new SynchedEntityData.DataValue<>(1, EntityDataSerializers.OPTIONAL_GLOBAL_POS, Optional.of(elsewhere))));

        ClientboundSetEntityDataPacket translated =
                (ClientboundSetEntityDataPacket) PacketTranslator.toClient(packet, context());

        assertEquals(Optional.of(GlobalPos.of(Level.OVERWORLD, MIRROR_CARRIED_BLOCK)),
                translated.packedItems().get(0).value());
        assertEquals(Optional.of(elsewhere), translated.packedItems().get(1).value());
    }

    @Test
    void synchedGlobalPosFollowsTheEntityOnlyInItsOwnDimension() {
        GlobalPos elsewhere = GlobalPos.of(Level.NETHER, SERVER_CARRIED_BLOCK);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.OPTIONAL_GLOBAL_POS,
                        Optional.of(GlobalPos.of(Level.OVERWORLD, SERVER_CARRIED_BLOCK))),
                new SynchedEntityData.DataValue<>(1, EntityDataSerializers.OPTIONAL_GLOBAL_POS, Optional.of(elsewhere))));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        assertEquals(Optional.of(GlobalPos.of(Level.OVERWORLD, CLIENT_CARRIED_BLOCK)),
                translated.packedItems().get(0).value());
        assertEquals(Optional.of(elsewhere), translated.packedItems().get(1).value());
    }

    @Test
    void synchedParticlePayloadFollowsTheEntity() {
        TrailParticleOption particle =
                new TrailParticleOption(new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0), 16545810, 30);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, particle)));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        TrailParticleOption trail = (TrailParticleOption) translated.packedItems().getFirst().value();
        assertEquals(new Vec3(CLIENT_X + 3.0, 71.0, CLIENT_Z + 3.0), trail.target());
        assertEquals(16545810, trail.color());
        assertEquals(30, trail.duration());
    }

    @Test
    void synchedParticleListFollowsTheEntity() {
        TrailParticleOption particle =
                new TrailParticleOption(new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0), 16545810, 30);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLES,
                        List.<ParticleOptions>of(ParticleTypes.FLAME, particle))));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        List<?> particles = (List<?>) translated.packedItems().getFirst().value();
        assertSame(ParticleTypes.FLAME, particles.get(0));
        assertEquals(new Vec3(CLIENT_X + 3.0, 71.0, CLIENT_Z + 3.0), ((TrailParticleOption) particles.get(1)).target());
    }

    @Test
    void synchedParticleWithoutTheEntityFoldsAroundTheMirror() {
        TrailParticleOption particle =
                new TrailParticleOption(new Vec3(SERVER_X + 3.0, 71.0, SERVER_Z + 3.0), 16545810, 30);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, particle)));

        ClientboundSetEntityDataPacket translated =
                (ClientboundSetEntityDataPacket) PacketTranslator.toClient(packet, context());

        TrailParticleOption trail = (TrailParticleOption) translated.packedItems().getFirst().value();
        assertEquals(new Vec3(CLIENT_X + 3.0, 71.0, CLIENT_Z + 3.0), trail.target());
    }

    @Test
    void positionlessValuesReturnTheSamePacket() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte) 6)));

        assertSame(packet, PacketTranslator.toClient(packet, context()));
    }

    @Test
    void positionlessSynchedParticleKeepsItsValue() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, ParticleTypes.FLAME)));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        assertSame(ParticleTypes.FLAME, translated.packedItems().getFirst().value());
    }
}
