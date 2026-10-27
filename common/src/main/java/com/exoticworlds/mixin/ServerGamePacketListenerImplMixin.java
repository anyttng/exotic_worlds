package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.ClientPositionHolder;
import com.exoticworlds.accessors.MoveCheckpoints;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.SeamMove;
import com.exoticworlds.engine.seam.SeamTeleport;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements ClientPositionHolder, MoveCheckpoints {
    @Shadow
    public ServerPlayer player;

    // Keep this the last field with an initialiser: mixin splices declaration initialisers by line-number range.
    @Unique
    private final ClientPosition exotic_worlds$clientPosition = new ClientPosition();

    @Override
    public ClientPosition exotic_worlds$clientPosition() {
        return this.exotic_worlds$clientPosition;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void exotic_worlds$seedMirror(MinecraftServer server, Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci) {
        ClientPosition.rebase(player);
    }

    @ModifyExpressionValue(
            method = "handleUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos exotic_worlds$wrapAckedNeighbor(BlockPos neighbor) {
        return WorldLoopAttachments.foldOf(this.player.level()).fold(neighbor);
    }

    @WrapMethod(method = "teleport(DDDFFLjava/util/Set;)V")
    private void exotic_worlds$wrapTeleportDestination(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, Operation<Void> original) {
        Vec3 wrapped = SeamTeleport.destination(this.player, x, y, z);
        original.call(wrapped.x, y, wrapped.z, yRot, xRot, relatives);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void exotic_worlds$dropChunksBeforeTeleport(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, CallbackInfo ci,
            @Share("dropped") LocalRef<SeamTeleport.Dropped> dropped) {
        dropped.set(SeamTeleport.dropBefore(this.player, x, z));
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void exotic_worlds$resendChunksAfterTeleport(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, CallbackInfo ci,
            @Share("dropped") LocalRef<SeamTeleport.Dropped> dropped) {
        SeamTeleport.resendAfter(this.player, dropped.get());
    }

    @Shadow
    private double firstGoodX;

    @Shadow
    private double firstGoodZ;

    @Shadow
    private double lastGoodX;

    @Shadow
    private double lastGoodZ;

    @Shadow
    private double vehicleFirstGoodX;

    @Shadow
    private double vehicleFirstGoodZ;

    @Shadow
    private double vehicleLastGoodX;

    @Shadow
    private double vehicleLastGoodZ;

    @Override
    public Vec3 exotic_worlds$firstGood(double y) {
        return new Vec3(this.firstGoodX, y, this.firstGoodZ);
    }

    @Override
    public Vec3 exotic_worlds$lastGood(double y) {
        return new Vec3(this.lastGoodX, y, this.lastGoodZ);
    }

    @Override
    public void exotic_worlds$setGood(Vec3 first, Vec3 last) {
        this.firstGoodX = first.x;
        this.firstGoodZ = first.z;
        this.lastGoodX = last.x;
        this.lastGoodZ = last.z;
    }

    @Override
    public void exotic_worlds$setVehicleGood(Vec3 first, Vec3 last) {
        this.vehicleFirstGoodX = first.x;
        this.vehicleFirstGoodZ = first.z;
        this.vehicleLastGoodX = last.x;
        this.vehicleLastGoodZ = last.z;
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 0))
    private double exotic_worlds$continuousX(double requestedX, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMovePlayerPacket packet,
            @Share("continuous") LocalRef<Vec3> continuous) {
        double clampedX = original.call(requestedX);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(this.player.level());
        if (fold == null) {
            return clampedX;
        }

        Vec3 seated = SeamMove.player(this.player, this, fold, new Vec3(clampedX,
                packet.getY(this.player.getY()), original.call(packet.getZ(this.player.getZ()))));
        continuous.set(seated);
        return seated.x;
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 1))
    private double exotic_worlds$continuousZ(double requestedZ, Operation<Double> original,
            @Share("continuous") LocalRef<Vec3> continuous) {
        double clampedZ = original.call(requestedZ);
        Vec3 seated = continuous.get();
        return seated == null ? clampedZ : seated.z;
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void exotic_worlds$wrapIntoBounds(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        SeamMove.playerIntoBounds(this.player, this);
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 0))
    private double exotic_worlds$vehicleContinuousX(double clientX, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMoveVehiclePacket packet,
            @Share("vehicleContinuous") LocalRef<Vec3> continuous) {
        double clampedX = original.call(clientX);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(this.player.level());
        if (fold == null) {
            return clampedX;
        }

        Vec3 seated = SeamMove.vehicle(this.player, fold, clampedX, packet.getY(),
                original.call(packet.getZ()));
        continuous.set(seated);
        return seated.x;
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 1))
    private double exotic_worlds$vehicleContinuousZ(double clientZ, Operation<Double> original,
            @Share("vehicleContinuous") LocalRef<Vec3> continuous) {
        double clampedZ = original.call(clientZ);
        Vec3 seated = continuous.get();
        return seated == null ? clampedZ : seated.z;
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void exotic_worlds$wrapVehicleIntoBounds(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        SeamMove.vehicleIntoBounds(this.player, this);
    }
}
