package com.exoticworlds.engine.net;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.FoldOrientation;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.VecDelta;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

final class EntityPacketTurns {
    private static final double BLOCK_CENTER = 0.5;

    private static final int HEAD_ROTATION_BYTES = 6;

    static Packet<?> moveEntity(ClientboundMoveEntityPacket packet, TranslationContext context) {
        FoldOrientation orientation = EntitySeat.orientationOf(context, packet.entityId);
        if (orientation.isIdentity()) {
            return packet;
        }

        byte yaw = Mth.packDegrees(orientation.applyToYaw(packet.getYRot()));
        byte pitch = Mth.packDegrees(packet.getXRot());
        if (!packet.hasPosition()) {
            return new ClientboundMoveEntityPacket.Rot(packet.entityId, yaw, pitch, packet.isOnGround());
        }

        VecDelta delta = turnedDelta(packet.getPositionDelta(), orientation);
        return packet.hasRotation()
                ? new ClientboundMoveEntityPacket.PosRot(packet.entityId, delta, yaw, pitch, packet.isOnGround())
                : new ClientboundMoveEntityPacket.Pos(packet.entityId, delta, packet.isOnGround());
    }

    static Packet<?> rotateHead(ClientboundRotateHeadPacket packet, TranslationContext context) {
        FoldOrientation orientation = EntitySeat.orientationOf(context, packet.entityId);
        if (orientation.isIdentity()) {
            return packet;
        }

        FriendlyByteBuf buffer = context.bufferFactory().apply(HEAD_ROTATION_BYTES);
        buffer.writeVarInt(packet.entityId);
        buffer.writeByte(Mth.packDegrees(orientation.applyToYaw(packet.getYHeadRot())));
        return new ClientboundRotateHeadPacket(buffer);
    }

    static Packet<?> entityMotion(ClientboundSetEntityMotionPacket packet, TranslationContext context) {
        FoldOrientation orientation = EntitySeat.orientationOf(context, packet.id());
        return orientation.isIdentity()
                ? packet
                : new ClientboundSetEntityMotionPacket(packet.id(), orientation.applyToDelta(packet.movement()));
    }

    static @Nullable Class<?> getTurnedEntityClass(TranslationContext context, int entityId) {
        if (context.fold().isPreservingLocalIndices()) {
            return null;
        }

        TagPositions.Subject subject = context.entity().apply(entityId);
        return subject == null ? null : subject.type();
    }

    static Vec3 spawnPosition(ClientboundAddEntityPacket packet, @Nullable Class<?> turnedEntityClass) {
        Vec3 sent = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        return isA(turnedEntityClass, BlockAttachedEntity.class) ? sent.add(BLOCK_CENTER, 0.0, BLOCK_CENTER) : sent;
    }

    static int spawnData(int data, @Nullable Class<?> turnedEntityClass, FoldOrientation orientation) {
        if (orientation.isIdentity()) {
            return data;
        }

        if (isA(turnedEntityClass, ItemFrame.class) || isA(turnedEntityClass, Painting.class)) {
            return orientation.applyToFace(Direction.from3DDataValue(data)).get3DDataValue();
        }

        return isA(turnedEntityClass, FallingBlockEntity.class)
                ? Block.getId(orientation.applyToState(Block.stateById(data)))
                : data;
    }

    private static boolean isA(@Nullable Class<?> entityClass, Class<?> family) {
        return entityClass != null && family.isAssignableFrom(entityClass);
    }

    private static VecDelta turnedDelta(VecDelta delta, FoldOrientation orientation) {
        return switch (delta) {
            case VecDelta.Linear(short xa, short ya, short za) ->
                    new VecDelta.Linear(turnedX(xa, za, orientation), ya, turnedZ(xa, za, orientation));
            case VecDelta.Stepped(var steps) -> new VecDelta.Stepped(steps.stream()
                    .map(step -> new VecDelta.Stepped.DeltaStep(
                            turnedX(step.xa(), step.za(), orientation), step.ya(),
                            turnedZ(step.xa(), step.za(), orientation), step.ticks()))
                    .toList());
        };
    }

    private static short turnedX(short xa, short za, FoldOrientation orientation) {
        short source = orientation.isSwappingAxes() ? za : xa;
        return orientation.isXFlipped() ? reversed(source) : source;
    }

    private static short turnedZ(short xa, short za, FoldOrientation orientation) {
        short source = orientation.isSwappingAxes() ? xa : za;
        return orientation.isZFlipped() ? reversed(source) : source;
    }

    private static short reversed(short step) {
        return (short) Math.min(-step, Short.MAX_VALUE);
    }

    private EntityPacketTurns() {
    }
}
