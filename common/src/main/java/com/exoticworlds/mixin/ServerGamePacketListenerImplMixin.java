package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.ChunkResender;
import com.exoticworlds.accessors.ClientPositionHolder;
import com.exoticworlds.accessors.TrackedEntityRefresher;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.MirrorWriter;
import com.exoticworlds.engine.seam.SeamSnap;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements ClientPositionHolder {
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
    private BlockPos exotic_worlds$wrapAckedNeighbour(BlockPos neighbour) {
        return WorldLoopAttachments.transformerOf(this.player.level()).fold(neighbour);
    }

    @WrapMethod(method = "teleport(DDDFFLjava/util/Set;)V")
    private void exotic_worlds$wrapTeleportDestination(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, Operation<Void> original) {
        Vec3 wrapped = WorldLoopAttachments.transformerOf(this.player.level()).fold(new Vec3(x, y, z));
        original.call(wrapped.x, y, wrapped.z, yRot, xRot, relatives);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void exotic_worlds$dropChunksBeforeTeleport(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, CallbackInfo ci,
            @Share("stormWholeView") LocalBooleanRef stormWholeView,
            @Share("flippedChunks") LocalRef<List<ChunkPos>> flippedChunks) {
        ChunkResender resender = exotic_worlds$chunkResender();
        if (resender == null) {
            return;
        }

        ClientPosition mirror = ClientPosition.of(this.player);
        if (!mirror.describes(this.player.level().dimension())) {
            stormWholeView.set(true);
            resender.exotic_worlds$dropTrackedChunks(this.player);
            return;
        }

        List<ChunkPos> flipped = exotic_worlds$flippedChunks(x, z, mirror);
        flippedChunks.set(flipped);
        if (!flipped.isEmpty()) {
            resender.exotic_worlds$dropChunks(this.player, flipped);
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void exotic_worlds$resendChunksAfterTeleport(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, CallbackInfo ci,
            @Share("stormWholeView") LocalBooleanRef stormWholeView,
            @Share("flippedChunks") LocalRef<List<ChunkPos>> flippedChunks) {
        ChunkResender resender = exotic_worlds$chunkResender();
        if (resender == null) {
            return;
        }

        if (stormWholeView.get()) {
            resender.exotic_worlds$resendTrackedChunks(this.player);
        } else {
            List<ChunkPos> flipped = flippedChunks.get();
            if (flipped != null && !flipped.isEmpty()) {
                resender.exotic_worlds$resendChunks(this.player, flipped);
            }
        }

        exotic_worlds$refreshTrackedEntities();
    }

    @Unique
    private void exotic_worlds$refreshTrackedEntities() {
        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) this.player.serverLevel().getChunkSource().chunkMap;
        refresher.exotic_worlds$refreshTrackedEntities(this.player);
    }

    @Unique
    private List<ChunkPos> exotic_worlds$flippedChunks(double destinationX, double destinationZ, ClientPosition mirror) {
        WorldFold transformer = WorldLoopAttachments.transformerOf(this.player.level());
        Vec3 clientDestination =
                mirror.destinationOf(transformer, new Vec3(destinationX, 0.0, destinationZ), Set.of());

        ChunkPos fromAnchor = mirror.chunk();
        ChunkPos toAnchor = new ChunkPos(
                SectionPos.blockToSectionCoord(clientDestination.x),
                SectionPos.blockToSectionCoord(clientDestination.z));

        List<ChunkPos> flipped = new ArrayList<>();
        this.player.getChunkTrackingView().forEach(viewPos -> {
            ChunkPos physical = transformer.fold(viewPos);
            if (!transformer.nearestCopy(fromAnchor, physical).equals(transformer.nearestCopy(toAnchor, physical))) {
                flipped.add(viewPos);
            }
        });
        return flipped;
    }

    @Unique
    private @Nullable ChunkResender exotic_worlds$chunkResender() {
        if (WorldLoopAttachments.wrappedTransformerOf(this.player.level()) == null) {
            return null;
        }

        return (ChunkResender) (Object) this.player.serverLevel().getChunkSource().chunkMap;
    }

    @Unique
    private boolean exotic_worlds$ridesUncontrolled() {
        return this.player.getRootVehicle().getControllingPassenger() != this.player;
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
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !packet.hasPosition()) {
            return clampedX;
        }

        Vec3 seated = exotic_worlds$seatPlayerMove(transformer, new Vec3(clampedX, packet.getY(this.player.getY()),
                original.call(packet.getZ(this.player.getZ()))));
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

    @Unique
    private Vec3 exotic_worlds$seatPlayerMove(WorldFold transformer, Vec3 clamped) {
        ClientPosition mirror = this.exotic_worlds$clientPosition;
        if (this.player.isPassenger()) {
            if (exotic_worlds$ridesUncontrolled()) {
                mirror.set(this.player.position(), MirrorWriter.PASSENGER);
            }
            return clamped;
        }

        mirror.set(clamped, MirrorWriter.PLAYER_MOVE);
        Vec3 unwrapped = transformer.nearestCopy(this.player.position(), clamped);

        Vec3 firstGood = transformer.nearestCopy(unwrapped, new Vec3(this.firstGoodX, unwrapped.y, this.firstGoodZ));
        Vec3 lastGood = transformer.nearestCopy(unwrapped, new Vec3(this.lastGoodX, unwrapped.y, this.lastGoodZ));
        this.firstGoodX = firstGood.x;
        this.firstGoodZ = firstGood.z;
        this.lastGoodX = lastGood.x;
        this.lastGoodZ = lastGood.z;
        return unwrapped;
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void exotic_worlds$wrapIntoBounds(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!SeamSnap.intoBounds(this.player)) {
            return;
        }

        this.firstGoodX = this.player.getX();
        this.firstGoodZ = this.player.getZ();
        this.lastGoodX = this.player.getX();
        this.lastGoodZ = this.player.getZ();
        this.player.serverLevel().getChunkSource().move(this.player);
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
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clampedX;
        }

        double clampedZ = original.call(packet.getZ());
        this.exotic_worlds$clientPosition.set(new Vec3(clampedX, packet.getY(), clampedZ), MirrorWriter.VEHICLE_MOVE);
        Entity vehicle = this.player.getRootVehicle();
        Vec3 seated = transformer.nearestCopy(vehicle.position(), new Vec3(clampedX, vehicle.getY(), clampedZ));
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
        Entity vehicle = this.player.getRootVehicle();
        if (vehicle == this.player || !SeamSnap.intoBounds(vehicle)) {
            return;
        }

        this.vehicleFirstGoodX = vehicle.getX();
        this.vehicleFirstGoodZ = vehicle.getZ();
        this.vehicleLastGoodX = vehicle.getX();
        this.vehicleLastGoodZ = vehicle.getZ();
    }
}
