package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.MeasuredReach;
import com.exoticworlds.engine.net.WorldShapeSync;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @WrapOperation(method = "broadcast", at = @At(value = "INVOKE", target = InjectionTargets.SERVER_PLAYER_GET_X))
    private double toroidal$listenerXThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true, name = "x") double x, @Local(argsOnly = true, name = "y") double y,
            @Local(argsOnly = true, name = "z") double z) {
        return toroidal$listenerCoord(player, Direction.Axis.X, new Vec3(x, y, z), original);
    }

    @WrapOperation(method = "broadcast", at = @At(value = "INVOKE", target = InjectionTargets.SERVER_PLAYER_GET_Z))
    private double toroidal$listenerZThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true, name = "x") double x, @Local(argsOnly = true, name = "y") double y,
            @Local(argsOnly = true, name = "z") double z) {
        return toroidal$listenerCoord(player, Direction.Axis.Z, new Vec3(x, y, z), original);
    }

    @WrapOperation(
            method = "broadcast",
            at = @At(value = "INVOKE", target = InjectionTargets.SERVER_GAME_PACKET_LISTENER_SEND))
    private void toroidal$measureBroadcastReach(ServerGamePacketListenerImpl connection, Packet<?> packet,
            Operation<Void> original, @Local(argsOnly = true, name = "range") double range) {
        try (MeasuredReach _ = MeasuredReach.measuring(range)) {
            original.call(connection, packet);
        }
    }

    @Unique
    private static double toroidal$listenerCoord(ServerPlayer player, Direction.Axis axis, Vec3 event,
            Operation<Double> original) {
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return transformer == null
                ? original.call(player)
                : NearestCopy.toward(transformer, axis, event, player.position());
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void toroidal$sendBoundsOnLogin(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
            CallbackInfo ci) {
        WorldShapeSync.sendAllTo(player);
    }

    @Inject(method = "respawn", at = @At("TAIL"))
    private void toroidal$sendBoundsOnRespawn(ServerPlayer player, boolean keepAllPlayerData,
            Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        WorldShapeSync.sendTo(cir.getReturnValue());
    }

    @WrapOperation(
            method = "setViewDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;setViewDistance(I)V"))
    private void toroidal$limitViewDistance(ServerChunkCache chunkSource, int viewDistance, Operation<Void> original,
            @Local ServerLevel level) {
        original.call(chunkSource, WorldLoopAttachments.transformerOf(level).limitViewDistance(viewDistance));
    }

    @WrapOperation(
            method = "setSimulationDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;setSimulationDistance(I)V"))
    private void toroidal$limitSimulationDistance(ServerChunkCache chunkSource, int simulationDistance, Operation<Void> original,
            @Local ServerLevel level) {
        original.call(chunkSource, WorldLoopAttachments.transformerOf(level).limitViewDistance(simulationDistance));
    }
}
