package com.exoticworlds.engine.net;

import static com.exoticworlds.engine.net.PacketTranslatorFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.exoticworlds.mixin.BlockPositionSourceAccessor;

import io.netty.buffer.Unpooled;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.phys.Vec3;

class PacketTranslatorEntityTest {
    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    private static ClientboundTeleportEntityPacket teleportPacket(int entityId, double x, double z,
            boolean onGround) {
        RegistryFriendlyByteBuf buf = buffer();
        buf.writeVarInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(70.0);
        buf.writeDouble(z);
        buf.writeByte(20);
        buf.writeByte(10);
        buf.writeBoolean(onGround);
        return ClientboundTeleportEntityPacket.STREAM_CODEC.decode(buf);
    }

    private static ClientboundMoveVehiclePacket moveVehiclePacket(double x, double z) {
        RegistryFriendlyByteBuf buf = buffer();
        buf.writeDouble(x);
        buf.writeDouble(70.0);
        buf.writeDouble(z);
        buf.writeFloat(30.0F);
        buf.writeFloat(10.0F);
        return ClientboundMoveVehiclePacket.STREAM_CODEC.decode(buf);
    }

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
    void teleportOfTheOwnControlledVehicleReachesTheClientOnItsCopy() {
        ClientboundTeleportEntityPacket translated = (ClientboundTeleportEntityPacket) PacketTranslator.toClient(
                teleportPacket(42, SERVER_X, SERVER_Z, false), context(entityId -> entityId == 42, entityId -> null));

        assertEquals(42, translated.getId());
        assertEquals(CLIENT_X, translated.getX());
        assertEquals(CLIENT_Z, translated.getZ());
    }

    @Test
    void teleportTranslatesThePositionAndKeepsTheTail() {
        ClientboundTeleportEntityPacket translated = (ClientboundTeleportEntityPacket) PacketTranslator.toClient(
                teleportPacket(42, SERVER_X, SERVER_Z, true), context());

        assertEquals(42, translated.getId());
        assertEquals(CLIENT_X, translated.getX());
        assertEquals(70.0, translated.getY());
        assertEquals(CLIENT_Z, translated.getZ());
        assertEquals((byte) 20, translated.getyRot());
        assertEquals((byte) 10, translated.getxRot());
        assertTrue(translated.isOnGround());
    }

    @Test
    void moveVehicleTranslatesThePositionAndKeepsTheRotation() {
        ClientboundMoveVehiclePacket translated = (ClientboundMoveVehiclePacket) PacketTranslator.toClient(
                moveVehiclePacket(SERVER_X, SERVER_Z), context());

        assertEquals(CLIENT_X, translated.getX());
        assertEquals(70.0, translated.getY());
        assertEquals(CLIENT_Z, translated.getZ());
        assertEquals(30.0F, translated.getYRot());
        assertEquals(10.0F, translated.getXRot());
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
        VibrationParticleOption particle =
                new VibrationParticleOption(new BlockPositionSource(SERVER_BLOCK), 12);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, particle)));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        VibrationParticleOption vibration =
                (VibrationParticleOption) translated.packedItems().getFirst().value();
        BlockPositionSource destination = (BlockPositionSource) vibration.getDestination();
        assertEquals(CLIENT_BLOCK, ((BlockPositionSourceAccessor) destination).exotic_worlds$getPos());
        assertEquals(12, vibration.getArrivalInTicks());
    }

    @Test
    void synchedParticleListFollowsTheEntity() {
        VibrationParticleOption particle =
                new VibrationParticleOption(new BlockPositionSource(SERVER_BLOCK), 12);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLES,
                        List.<ParticleOptions>of(ParticleTypes.FLAME, particle))));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        List<?> particles = (List<?>) translated.packedItems().getFirst().value();
        assertSame(ParticleTypes.FLAME, particles.get(0));
        BlockPositionSource destination =
                (BlockPositionSource) ((VibrationParticleOption) particles.get(1)).getDestination();
        assertEquals(CLIENT_BLOCK, ((BlockPositionSourceAccessor) destination).exotic_worlds$getPos());
    }

    @Test
    void synchedParticleWithoutTheEntityFoldsAroundTheMirror() {
        VibrationParticleOption particle =
                new VibrationParticleOption(new BlockPositionSource(SERVER_BLOCK), 12);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, EntityDataSerializers.PARTICLE, particle)));

        ClientboundSetEntityDataPacket translated =
                (ClientboundSetEntityDataPacket) PacketTranslator.toClient(packet, context());

        VibrationParticleOption vibration =
                (VibrationParticleOption) translated.packedItems().getFirst().value();
        BlockPositionSource destination = (BlockPositionSource) vibration.getDestination();
        assertEquals(CLIENT_BLOCK, ((BlockPositionSourceAccessor) destination).exotic_worlds$getPos());
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

    @Test
    void synchedValueOfARegisteredSerializerIsReSeated() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, FOLDED_POSITION_SERIALIZER, SERVER_CARRIED_POSITION)));

        ClientboundSetEntityDataPacket translated =
                (ClientboundSetEntityDataPacket) PacketTranslator.toClient(packet, context());

        assertEquals(MIRROR_CARRIED_POSITION, translated.packedItems().getFirst().value());
    }

    @Test
    void synchedValueOfARegisteredSerializerFollowsTheEntityWhenItIsKnown() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, FOLDED_POSITION_SERIALIZER, SERVER_CARRIED_POSITION)));

        ClientboundSetEntityDataPacket translated = (ClientboundSetEntityDataPacket) PacketTranslator.toClient(
                packet, context(entityId -> false, entityId -> new Vec3(SERVER_X, 70.0, SERVER_Z)));

        assertEquals(CLIENT_CARRIED_POSITION, translated.packedItems().getFirst().value());
    }

    @Test
    void synchedValueOfAnUnregisteredSerializerPassesThrough() {
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(3, List.of(
                new SynchedEntityData.DataValue<>(0, UNFOLDED_POSITION_SERIALIZER, SERVER_CARRIED_POSITION)));

        assertSame(packet, PacketTranslator.toClient(packet, context()));
    }
}
