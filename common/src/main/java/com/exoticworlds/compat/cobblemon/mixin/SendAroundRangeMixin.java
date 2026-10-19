package com.exoticworlds.compat.cobblemon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.NearestCopy;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(NetworkPacket.class)
public interface SendAroundRangeMixin {
    @WrapOperation(
            method = "sendToPlayersAround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getX()D"))
    private double exotic_worlds$playerXThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y,
            @Local(argsOnly = true, ordinal = 2) double z) {
        return NearestCopy.toward(((TransformerSource) player).exotic_worlds$wrappedTransformer(), Direction.Axis.X,
                new Vec3(x, y, z), player.position());
    }

    @WrapOperation(
            method = "sendToPlayersAround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getY()D"))
    private double exotic_worlds$playerYThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y,
            @Local(argsOnly = true, ordinal = 2) double z) {
        return NearestCopy.toward(((TransformerSource) player).exotic_worlds$wrappedTransformer(), Direction.Axis.Y,
                new Vec3(x, y, z), player.position());
    }

    @WrapOperation(
            method = "sendToPlayersAround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getZ()D"))
    private double exotic_worlds$playerZThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) double x, @Local(argsOnly = true, ordinal = 1) double y,
            @Local(argsOnly = true, ordinal = 2) double z) {
        return NearestCopy.toward(((TransformerSource) player).exotic_worlds$wrappedTransformer(), Direction.Axis.Z,
                new Vec3(x, y, z), player.position());
    }
}
