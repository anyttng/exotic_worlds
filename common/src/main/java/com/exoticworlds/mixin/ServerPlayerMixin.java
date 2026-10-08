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
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements SeamTravelHolder {
    @Unique
    private static final String TRAVEL_KEY = "exotic_worlds:travel";

    @Unique
    private static final String PLAYER_TICK = "Lnet/minecraft/world/entity/player/Player;tick()V";

    @Unique
    private SeamTravel toroidal$travel;

    @Override
    public SeamTravel toroidal$travel() {
        if (this.toroidal$travel == null) {
            this.toroidal$travel = new SeamTravel();
        }

        return this.toroidal$travel;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void toroidal$readTravel(CompoundTag tag, CallbackInfo ci) {
        Tag travel = tag.get(TRAVEL_KEY);
        if (travel != null) {
            SeamTravel.CODEC.parse(NbtOps.INSTANCE, travel).result().ifPresent(this.toroidal$travel()::copyFrom);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void toroidal$writeTravel(CompoundTag tag, CallbackInfo ci) {
        SeamTravel.CODEC.encodeStart(NbtOps.INSTANCE, this.toroidal$travel()).result()
                .ifPresent(travel -> tag.put(TRAVEL_KEY, travel));
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void toroidal$carryTravelThroughRespawn(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        this.toroidal$travel().copyFrom(((SeamTravelHolder) oldPlayer).toroidal$travel());
    }

    @ModifyVariable(method = "setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V",
            at = @At("HEAD"), argsOnly = true)
    private @Nullable BlockPos toroidal$storeRespawnInsideBounds(@Nullable BlockPos respawnPos,
            @Local(argsOnly = true) ResourceKey<Level> respawnDimension) {
        if (respawnPos == null) {
            return null;
        }

        MinecraftServer server = ((ServerPlayer) (Object) this).getServer();
        ServerLevel level = server == null ? null : server.getLevel(respawnDimension);
        if (level == null) {
            return respawnPos;
        }

        return SeamRespawnData.insideBounds(level, respawnPos);
    }

    @WrapMethod(method = "indicateDamage")
    private void toroidal$hurtDirThroughSeam(double xd, double zd, Operation<Void> original) {
        Vec3 direction = SeamAim.foldDelta((ServerPlayer) (Object) this, xd, zd);
        original.call(direction.x, direction.z);
    }

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("TAIL"))
    private void toroidal$sendBoundsOnDimensionChange(DimensionTransition transition,
            CallbackInfoReturnable<@Nullable Entity> cir) {
        WorldShapeSync.sendTo((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick", at = @At(value = "INVOKE", target = PLAYER_TICK, shift = At.Shift.AFTER))
    private void toroidal$refreshClientAnchors(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ClientAnchorSync.refresh(player);
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
                (TrackedEntityRefresher) (Object) player.serverLevel().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(player);
    }

    @WrapMethod(method = "isReachableBedBlock")
    private boolean toroidal$bedReachThroughSeam(BlockPos bedBlockPos, Operation<Boolean> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return original.call(bedBlockPos);
        }

        return original.call(transformer.nearestCopy(player.blockPosition(), bedBlockPos));
    }
}
