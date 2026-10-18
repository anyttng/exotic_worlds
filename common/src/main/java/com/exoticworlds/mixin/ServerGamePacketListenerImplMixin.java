package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.ChunkResender;
import com.exoticworlds.accessors.ClientPositionHolder;
import com.exoticworlds.accessors.TrackedEntityRefresher;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.RelativePosition;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.MirrorWriter;
import com.exoticworlds.engine.seam.SeamSnap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements ClientPositionHolder {
    @Unique
    private static final String exotic_worlds$CLAMP_HORIZONTAL =
            "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D";

    @Unique
    private static final String exotic_worlds$HANDLE_PLAYER_POSITION_CHANGE = "handlePlayerPositionChange(DDDFFZZ)V";

    @Unique
    private static final String exotic_worlds$TELEPORT =
            "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V";

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

    @ModifyVariable(
            method = exotic_worlds$TELEPORT,
            at = @At("HEAD"),
            argsOnly = true)
    private PositionMoveRotation exotic_worlds$wrapTeleportDestination(PositionMoveRotation destination,
            @Local(argsOnly = true) Set<Relative> relatives) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return destination;
        }

        Vec3 position = destination.position();
        Vec3 wrapped = RelativePosition.moved(position, relatives.contains(Relative.X), relatives.contains(Relative.Z),
                this.player.position(), transformer::fold);
        if (wrapped == position) {
            return destination;
        }

        return new PositionMoveRotation(wrapped, destination.deltaMovement(), destination.yRot(), destination.xRot());
    }

    @Inject(method = exotic_worlds$TELEPORT, at = @At("HEAD"))
    private void exotic_worlds$dropChunksBeforeTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci,
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

        List<ChunkPos> flipped = exotic_worlds$flippedChunks(destination, relatives, mirror);
        flippedChunks.set(flipped);
        if (!flipped.isEmpty()) {
            resender.exotic_worlds$dropChunks(this.player, flipped);
        }
    }

    @Inject(method = exotic_worlds$TELEPORT, at = @At("TAIL"))
    private void exotic_worlds$resendChunksAfterTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci,
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
                (TrackedEntityRefresher) (Object) this.player.level().getChunkSource().chunkMap;
        refresher.exotic_worlds$refreshTrackedEntities(this.player);
    }

    @Unique
    private List<ChunkPos> exotic_worlds$flippedChunks(PositionMoveRotation destination, Set<Relative> relatives, ClientPosition mirror) {
        WorldFold transformer = WorldLoopAttachments.transformerOf(this.player.level());
        Vec3 clientDestination = mirror.destinationOf(transformer, destination.position(), relatives);

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

        return (ChunkResender) (Object) this.player.level().getChunkSource().chunkMap;
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
            method = exotic_worlds$HANDLE_PLAYER_POSITION_CHANGE,
            at = @At(
                    value = "INVOKE",
                    target = exotic_worlds$CLAMP_HORIZONTAL,
                    ordinal = 0))
    private double exotic_worlds$continuousX(double requestedX, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 2) double requestedZ,
            @Share("continuous") LocalRef<Vec3> continuous) {
        double clampedX = original.call(requestedX);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clampedX;
        }

        Vec3 seated = exotic_worlds$seatPlayerMove(transformer, clampedX, original.call(requestedZ));
        continuous.set(seated);
        return seated.x;
    }

    @WrapOperation(
            method = exotic_worlds$HANDLE_PLAYER_POSITION_CHANGE,
            at = @At(
                    value = "INVOKE",
                    target = exotic_worlds$CLAMP_HORIZONTAL,
                    ordinal = 1))
    private double exotic_worlds$continuousZ(double requestedZ, Operation<Double> original,
            @Share("continuous") LocalRef<Vec3> continuous) {
        double clampedZ = original.call(requestedZ);
        Vec3 seated = continuous.get();
        return seated == null ? clampedZ : seated.z;
    }

    @Unique
    private Vec3 exotic_worlds$seatPlayerMove(WorldFold transformer, double clampedX, double clampedZ) {
        Vec3 clamped = new Vec3(clampedX, this.player.getY(), clampedZ);
        ClientPosition mirror = this.exotic_worlds$clientPosition;
        if (this.player.isPassenger()) {
            if (exotic_worlds$ridesUncontrolled()) {
                mirror.set(this.player.getX(), this.player.getZ(), MirrorWriter.PASSENGER);
            }
            return clamped;
        }

        mirror.set(clampedX, clampedZ, MirrorWriter.PLAYER_MOVE);
        Vec3 unwrapped = transformer.nearestCopy(this.player.position(), clamped);

        Vec3 firstGood = transformer.nearestCopy(unwrapped, new Vec3(this.firstGoodX, unwrapped.y, this.firstGoodZ));
        Vec3 lastGood = transformer.nearestCopy(unwrapped, new Vec3(this.lastGoodX, unwrapped.y, this.lastGoodZ));
        this.firstGoodX = firstGood.x;
        this.firstGoodZ = firstGood.z;
        this.lastGoodX = lastGood.x;
        this.lastGoodZ = lastGood.z;
        return unwrapped;
    }

    @Inject(method = exotic_worlds$HANDLE_PLAYER_POSITION_CHANGE, at = @At("RETURN"))
    private void exotic_worlds$wrapIntoBounds(CallbackInfo ci) {
        if (!SeamSnap.intoBounds(this.player)) {
            return;
        }

        this.firstGoodX = this.player.getX();
        this.firstGoodZ = this.player.getZ();
        this.lastGoodX = this.player.getX();
        this.lastGoodZ = this.player.getZ();
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = exotic_worlds$CLAMP_HORIZONTAL,
                    ordinal = 0))
    private double exotic_worlds$vehicleContinuousX(double clientX, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMoveVehiclePacket packet,
            @Share("vehicleContinuous") LocalRef<Vec3> continuous) {
        double clampedX = original.call(clientX);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clampedX;
        }

        double clampedZ = original.call(packet.movingTo().position().z());
        this.exotic_worlds$clientPosition.set(clampedX, clampedZ, MirrorWriter.VEHICLE_MOVE);
        Entity vehicle = this.player.getRootVehicle();
        Vec3 seated = transformer.nearestCopy(vehicle.position(), new Vec3(clampedX, vehicle.getY(), clampedZ));
        continuous.set(seated);
        return seated.x;
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = exotic_worlds$CLAMP_HORIZONTAL,
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
