package com.exoticworlds.engine.seam;

import com.exoticworlds.accessors.MoveCheckpoints;
import com.exoticworlds.core.WorldFold;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class SeamMove {
    public static Vec3 player(ServerPlayer player, MoveCheckpoints checkpoints, WorldFold fold, Vec3 clamped) {
        ClientPosition mirror = ClientPosition.of(player);
        if (player.isPassenger()) {
            if (player.getRootVehicle().getControllingPassenger() != player) {
                mirror.set(player.position(), MirrorWriter.PASSENGER);
            }
            return clamped;
        }

        mirror.set(clamped, MirrorWriter.PLAYER_MOVE);
        Vec3 unwrapped = fold.nearestCopy(player.position(), clamped);
        checkpoints.exotic_worlds$setGood(
                fold.nearestCopy(unwrapped, checkpoints.exotic_worlds$firstGood(unwrapped.y)),
                fold.nearestCopy(unwrapped, checkpoints.exotic_worlds$lastGood(unwrapped.y)));
        return unwrapped;
    }

    public static Vec3 vehicle(ServerPlayer player, WorldFold fold, double clampedX, double reportedY,
            double clampedZ) {
        ClientPosition.of(player).set(new Vec3(clampedX, reportedY, clampedZ), MirrorWriter.VEHICLE_MOVE);
        Entity vehicle = player.getRootVehicle();
        return fold.nearestCopy(vehicle.position(), new Vec3(clampedX, vehicle.getY(), clampedZ));
    }

    public static void playerIntoBounds(ServerPlayer player, MoveCheckpoints checkpoints) {
        if (SeamSnap.intoBounds(player)) {
            checkpoints.exotic_worlds$setGood(player.position(), player.position());
            player.serverLevel().getChunkSource().move(player);
        }
    }

    public static void vehicleIntoBounds(ServerPlayer player, MoveCheckpoints checkpoints) {
        Entity vehicle = player.getRootVehicle();
        if (vehicle != player && SeamSnap.intoBounds(vehicle)) {
            checkpoints.exotic_worlds$setVehicleGood(vehicle.position(), vehicle.position());
        }
    }

    private SeamMove() {
    }
}
