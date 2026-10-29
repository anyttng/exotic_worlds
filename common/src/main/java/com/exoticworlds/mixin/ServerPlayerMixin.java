package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.SeamTravelHolder;
import com.exoticworlds.accessors.TrackedEntityRefresher;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.SeamRespawnData;
import com.exoticworlds.engine.net.ClientAnchorSync;
import com.exoticworlds.engine.net.WorldShapeSync;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;
import com.exoticworlds.engine.seam.circumnavigation.CircumnavigationTracker;
import com.exoticworlds.engine.seam.circumnavigation.SeamTravel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements SeamTravelHolder {
    @Unique
    private static final String TRAVEL_KEY = ExoticWorlds.MODID + ":travel";

    @Unique
    private static final String PLAYER_TICK = "Lnet/minecraft/world/entity/player/Player;tick()V";

    @Unique
    private final SeamTravel exotic_worlds$travel = new SeamTravel();

    @Override
    public SeamTravel exotic_worlds$travel() {
        return this.exotic_worlds$travel;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void exotic_worlds$readTravel(ValueInput input, CallbackInfo ci) {
        input.read(TRAVEL_KEY, SeamTravel.CODEC).ifPresent(this.exotic_worlds$travel::copyFrom);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void exotic_worlds$writeTravel(ValueOutput output, CallbackInfo ci) {
        output.store(TRAVEL_KEY, SeamTravel.CODEC, this.exotic_worlds$travel);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void exotic_worlds$carryTravelThroughRespawn(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        this.exotic_worlds$travel.copyFrom(((SeamTravelHolder) oldPlayer).exotic_worlds$travel());
    }

    @ModifyVariable(method = "setRespawnPosition", at = @At("HEAD"), argsOnly = true)
    private ServerPlayer.@Nullable RespawnConfig exotic_worlds$storeRespawnInsideBounds(
            ServerPlayer.@Nullable RespawnConfig respawnConfig) {
        if (respawnConfig == null) {
            return null;
        }

        LevelData.RespawnData respawnData = SeamRespawnData.insideBounds(
                ((ServerPlayer) (Object) this).level().getServer(), respawnConfig.respawnData());
        return respawnData == respawnConfig.respawnData()
                ? respawnConfig
                : new ServerPlayer.RespawnConfig(respawnData, respawnConfig.forced());
    }

    @WrapMethod(method = "indicateDamage")
    private void exotic_worlds$hurtDirThroughSeam(double xd, double zd, Operation<Void> original) {
        Vec3 direction = SeamAim.foldDelta((ServerPlayer) (Object) this, xd, zd);
        original.call(direction.x, direction.z);
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("TAIL"))
    private void exotic_worlds$sendBoundsOnDimensionChange(TeleportTransition transition,
            CallbackInfoReturnable<@Nullable ServerPlayer> cir) {
        WorldShapeSync.sendTo((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick", at = @At(value = "INVOKE", target = PLAYER_TICK, shift = At.Shift.AFTER))
    private void exotic_worlds$refreshClientAnchors(CallbackInfo ci) {
        ClientAnchorSync.refresh((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick", at = @At(value = "INVOKE", target = PLAYER_TICK, shift = At.Shift.AFTER))
    private void exotic_worlds$sampleTravel(CallbackInfo ci) {
        CircumnavigationTracker.sample((ServerPlayer) (Object) this);
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void exotic_worlds$captureRequestedViewDistance(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        oldViewDistance.set(((ServerPlayer) (Object) this).requestedViewDistance());
    }

    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void exotic_worlds$refreshTrackingOnViewChange(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.connection == null || oldViewDistance.get() == player.requestedViewDistance()) {
            return;
        }

        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(player.level());
        if (fold == null) {
            return;
        }

        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) player.level().getChunkSource().chunkMap;
        refresher.exotic_worlds$refreshTrackedEntities(player);
    }

    @WrapOperation(
            method = "isReachableBedBlock",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_BOTTOM_CENTER_OF))
    private Vec3 exotic_worlds$bedTowardSleeper(Vec3i bedBlockPos, Operation<Vec3> original) {
        return SeamSteering.nearestCopy((ServerPlayer) (Object) this, original.call(bedBlockPos));
    }
}
