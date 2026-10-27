package com.exoticworlds.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.net.ListenerCopies;
import com.exoticworlds.engine.net.MeasuredReach;
import com.exoticworlds.engine.net.WorldShapeSync;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Unique
    private static final String BROADCAST_ALL =
            "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V";

    @Shadow
    @Final
    private List<ServerPlayer> players;

    @WrapMethod(method = "broadcast")
    private void exotic_worlds$broadcastThroughSeam(@Nullable Player except, double x, double y, double z, double range,
            ResourceKey<Level> dimension, Packet<?> packet, Operation<Void> original) {
        WorldFold fold = null;
        for (ServerPlayer player : this.players) {
            if (player != except && player.level().dimension() == dimension) {
                fold = WorldLoopAttachments.wrappedFoldOf(player.level());
                break;
            }
        }

        if (fold == null) {
            original.call(except, x, y, z, range, dimension, packet);
            return;
        }

        Vec3 source = new Vec3(x, y, z);
        List<Vec3> copies = ListenerCopies.nearestTo(fold, this.players,
                player -> player != except && player.level().dimension() == dimension, source);
        for (Vec3 copy : copies) {
            try (MeasuredReach ignored = MeasuredReach.measuring(range)) {
                original.call(except, copy.x, copy.y, copy.z, range, dimension, packet);
            }
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void exotic_worlds$sendBoundsOnLogin(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
            CallbackInfo ci) {
        WorldShapeSync.sendAllTo(player);
    }

    @Inject(method = "respawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;moveTo(DDDFF)V",
                    shift = At.Shift.AFTER))
    private void exotic_worlds$rebaseMirrorOnRespawn(ServerPlayer player, boolean keepEverything,
            Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> cir,
            @Local(ordinal = 1) ServerPlayer respawned) {
        ClientPosition.rebase(respawned);
    }

    @WrapOperation(
            method = "placeNewPlayer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;viewDistance:I",
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$loginViewDistance(PlayerList playerList, Operation<Integer> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return WorldShapeSync.distanceFor(player, original.call(playerList));
    }

    @WrapOperation(
            method = "placeNewPlayer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;simulationDistance:I",
                    opcode = Opcodes.GETFIELD))
    private int exotic_worlds$loginSimulationDistance(PlayerList playerList, Operation<Integer> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return WorldShapeSync.distanceFor(player, original.call(playerList));
    }

    @Inject(method = "respawn", at = @At("TAIL"))
    private void exotic_worlds$sendBoundsOnRespawn(ServerPlayer player, boolean keepAllPlayerData,
            Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        WorldShapeSync.sendTo(cir.getReturnValue());
    }

    @WrapOperation(method = "setViewDistance", at = @At(value = "INVOKE", target = BROADCAST_ALL))
    private void exotic_worlds$sendViewDistancePerLevel(PlayerList playerList, Packet<?> packet, Operation<Void> original) {
        playerList.getPlayers().forEach(WorldShapeSync::sendViewDistance);
    }

    @WrapOperation(method = "setSimulationDistance", at = @At(value = "INVOKE", target = BROADCAST_ALL))
    private void exotic_worlds$sendSimulationDistancePerLevel(PlayerList playerList, Packet<?> packet,
            Operation<Void> original) {
        playerList.getPlayers().forEach(WorldShapeSync::sendSimulationDistance);
    }

    @WrapOperation(
            method = "setViewDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;setViewDistance(I)V"))
    private void exotic_worlds$limitViewDistance(ServerChunkCache chunkSource, int viewDistance, Operation<Void> original,
            @Local ServerLevel level) {
        original.call(chunkSource, WorldLoopAttachments.foldOf(level).limitViewDistance(viewDistance));
    }

    @WrapOperation(
            method = "setSimulationDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;setSimulationDistance(I)V"))
    private void exotic_worlds$limitSimulationDistance(ServerChunkCache chunkSource, int simulationDistance, Operation<Void> original,
            @Local ServerLevel level) {
        original.call(chunkSource, WorldLoopAttachments.foldOf(level).limitViewDistance(simulationDistance));
    }
}
