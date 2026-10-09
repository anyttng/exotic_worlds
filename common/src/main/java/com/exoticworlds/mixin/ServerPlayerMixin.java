package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.SeamTravelHolder;
import com.exoticworlds.accessors.TrackedEntityRefresher;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.level.SeamRespawnData;
import com.exoticworlds.engine.net.ClientAnchorSync;
import com.exoticworlds.engine.net.WorldShapeSync;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.circumnavigation.CircumnavigationTracker;
import com.exoticworlds.engine.seam.circumnavigation.SeamTravel;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import net.minecraft.core.BlockPos;
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
    private static final double BED_REACH_HORIZONTAL = 3.0;

    @Unique
    private static final double BED_REACH_VERTICAL = 2.0;

    @Unique
    private static final String TRAVEL_KEY = "exotic_worlds:travel";

    @Unique
    private static final String PLAYER_TICK = "Lnet/minecraft/world/entity/player/Player;tick()V";

    @Unique
    private final SeamTravel toroidal$travel = new SeamTravel();

    @Override
    public SeamTravel toroidal$travel() {
        return this.toroidal$travel;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void toroidal$readTravel(ValueInput input, CallbackInfo ci) {
        input.read(TRAVEL_KEY, SeamTravel.CODEC).ifPresent(this.toroidal$travel::copyFrom);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void toroidal$writeTravel(ValueOutput output, CallbackInfo ci) {
        output.store(TRAVEL_KEY, SeamTravel.CODEC, this.toroidal$travel);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void toroidal$carryTravelThroughRespawn(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        this.toroidal$travel.copyFrom(((SeamTravelHolder) oldPlayer).toroidal$travel());
    }

    @ModifyVariable(method = "setRespawnPosition", at = @At("HEAD"), argsOnly = true)
    private ServerPlayer.@Nullable RespawnConfig toroidal$storeRespawnInsideBounds(
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
    private void toroidal$hurtDirThroughSeam(double xd, double zd, Operation<Void> original) {
        Vec3 direction = SeamAim.foldDelta((ServerPlayer) (Object) this, xd, zd);
        original.call(direction.x, direction.z);
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("TAIL"))
    private void toroidal$sendBoundsOnDimensionChange(TeleportTransition transition,
            CallbackInfoReturnable<@Nullable ServerPlayer> cir) {
        WorldShapeSync.sendTo((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick", at = @At(value = "INVOKE", target = PLAYER_TICK, shift = At.Shift.AFTER))
    private void toroidal$refreshClientAnchors(CallbackInfo ci) {
        ClientAnchorSync.refresh((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick", at = @At(value = "INVOKE", target = PLAYER_TICK, shift = At.Shift.AFTER))
    private void toroidal$sampleTravel(CallbackInfo ci) {
        CircumnavigationTracker.sample((ServerPlayer) (Object) this);
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void toroidal$captureRequestedViewDistance(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        oldViewDistance.set(((ServerPlayer) (Object) this).requestedViewDistance());
    }

    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void toroidal$refreshTrackingOnViewChange(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.connection == null || oldViewDistance.get() == player.requestedViewDistance()) {
            return;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return;
        }

        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) player.level().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(player);
    }

    @WrapMethod(method = "isReachableBedBlock")
    private boolean toroidal$bedReachThroughSeam(BlockPos bedBlockPos, Operation<Boolean> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return original.call(bedBlockPos);
        }

        Vec3 bedCenter = Vec3.atBottomCenterOf(bedBlockPos);
        Vec3 delta = transformer.foldDelta(player.position(), bedCenter);
        return Math.abs(delta.x) <= BED_REACH_HORIZONTAL
                && Math.abs(delta.y) <= BED_REACH_VERTICAL
                && Math.abs(delta.z) <= BED_REACH_HORIZONTAL;
    }
}
