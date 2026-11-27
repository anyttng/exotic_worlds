package com.exoticworlds.engine.seam;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.MoveCheckpoints;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamMove {
    public static Vec3 player(ServerPlayer player, MoveCheckpoints checkpoints, WorldFold fold, double clampedX,
            double clampedZ) {
        Vec3 clamped = new Vec3(clampedX, player.getY(), clampedZ);
        ClientPosition mirror = ClientPosition.of(player);
        if (player.isPassenger()) {
            if (player.getRootVehicle().getControllingPassenger() != player) {
                mirror.set(player.getX(), player.getZ(), MirrorWriter.PASSENGER);
            }
            return clamped;
        }

        mirror.set(clampedX, clampedZ, MirrorWriter.PLAYER_MOVE);
        Vec3 unwrapped = fold.nearestCopy(player.position(), clamped);
        reseatCheckpoints(checkpoints, fold, unwrapped);
        return unwrapped;
    }

    private static void reseatCheckpoints(MoveCheckpoints checkpoints, WorldFold fold, Vec3 unwrapped) {
        checkpoints.exotic_worlds$setGood(
                fold.nearestCopy(unwrapped, checkpoints.exotic_worlds$firstGood(unwrapped.y)),
                fold.nearestCopy(unwrapped, checkpoints.exotic_worlds$lastGood(unwrapped.y)));
    }

    public static Vec3 vehicle(ServerPlayer player, WorldFold fold, double clampedX, double clampedZ) {
        ClientPosition.of(player).set(clampedX, clampedZ, MirrorWriter.VEHICLE_MOVE);
        Entity vehicle = player.getRootVehicle();
        return fold.nearestCopy(vehicle.position(), new Vec3(clampedX, vehicle.getY(), clampedZ));
    }

    public static float playerYaw(ServerPlayer player, ServerboundMovePlayerPacket packet, float yaw) {
        if (!packet.hasRotation()) {
            return yaw;
        }

        return yawFrom(player, player.position(), packet.hasPosition() ? clientPointOf(packet) : null, yaw);
    }

    private static Vec3 clientPointOf(ServerboundMovePlayerPacket packet) {
        return new Vec3(packet.getX(0.0), 0.0, packet.getZ(0.0));
    }

    public static float vehicleYaw(ServerPlayer player, Vec3 clientPoint, float yaw) {
        return yawFrom(player, player.getRootVehicle().position(), clientPoint, yaw);
    }

    public static float heldYaw(ServerPlayer player, float yaw) {
        return yawFrom(player, player.position(), null, yaw);
    }

    private static float yawFrom(ServerPlayer player, Vec3 serverPosition, @Nullable Vec3 clientPoint, float yaw) {
        WorldFold fold = WorldLoopAttachments.getFold(player.level());
        ClientPosition mirror = ClientPosition.of(player);
        if (fold.isPreservingLocalIndices() || !mirror.isForDimension(player.level().dimension())) {
            return yaw;
        }

        Vec3 seen = clientPoint == null ? new Vec3(mirror.getX(), 0.0, mirror.getZ()) : clientPoint;
        return serverYaw(fold, serverPosition, seen, yaw);
    }

    static float serverYaw(WorldFold fold, Vec3 serverPosition, Vec3 clientPoint, float yaw) {
        return fold.nearestCopyTransformation(serverPosition, clientPoint).getOrientation().applyToYaw(yaw);
    }

    public static void playerIntoBounds(ServerPlayer player, MoveCheckpoints checkpoints) {
        if (SeamSnap.trySnapIntoBounds(player)) {
            checkpoints.exotic_worlds$setGood(player.position(), player.position());
        }
    }

    public static void vehicleIntoBounds(ServerPlayer player, MoveCheckpoints checkpoints) {
        Entity vehicle = player.getRootVehicle();
        if (vehicle != player && SeamSnap.trySnapIntoBounds(vehicle)) {
            checkpoints.exotic_worlds$setVehicleGood(vehicle.position(), vehicle.position());
        }
    }

    private SeamMove() {
    }
}
