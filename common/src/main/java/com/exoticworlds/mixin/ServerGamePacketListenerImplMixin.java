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
import com.exoticworlds.core.DeckTransformation;
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
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
    @Shadow
    public ServerPlayer player;

    // Keep this the last field with an initialiser: mixin splices declaration initialisers by line-number range.
    @Unique
    private final ClientPosition toroidal$clientPosition = new ClientPosition();

    @Override
    public ClientPosition toroidal$clientPosition() {
        return this.toroidal$clientPosition;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$seedMirror(MinecraftServer server, Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci) {
        ClientPosition.rebase(player);
    }

    @ModifyVariable(
            method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private PositionMoveRotation toroidal$wrapTeleportDestination(PositionMoveRotation destination,
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

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
    private void toroidal$dropChunksBeforeTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci,
            @Share("stormWholeView") LocalBooleanRef stormWholeView,
            @Share("flippedChunks") LocalRef<List<ChunkPos>> flippedChunks) {
        ChunkResender resender = toroidal$chunkResender();
        if (resender == null) {
            return;
        }

        ClientPosition mirror = ClientPosition.of(this.player);
        if (!mirror.describes(this.player.level().dimension())) {
            stormWholeView.set(true);
            resender.toroidal$dropTrackedChunks(this.player);
            return;
        }

        List<ChunkPos> flipped = toroidal$flippedChunks(destination, relatives, mirror);
        flippedChunks.set(flipped);
        if (!flipped.isEmpty()) {
            resender.toroidal$dropChunks(this.player, flipped);
        }
    }

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("TAIL"))
    private void toroidal$resendChunksAfterTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci,
            @Share("stormWholeView") LocalBooleanRef stormWholeView,
            @Share("flippedChunks") LocalRef<List<ChunkPos>> flippedChunks) {
        ChunkResender resender = toroidal$chunkResender();
        if (resender == null) {
            return;
        }

        if (stormWholeView.get()) {
            resender.toroidal$resendTrackedChunks(this.player);
        } else {
            List<ChunkPos> flipped = flippedChunks.get();
            if (flipped != null && !flipped.isEmpty()) {
                resender.toroidal$resendChunks(this.player, flipped);
            }
        }

        toroidal$refreshTrackedEntities();
    }

    @Unique
    private void toroidal$refreshTrackedEntities() {
        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) this.player.level().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(this.player);
    }

    @Unique
    private List<ChunkPos> toroidal$flippedChunks(PositionMoveRotation destination, Set<Relative> relatives, ClientPosition mirror) {
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
    private @Nullable ChunkResender toroidal$chunkResender() {
        if (WorldLoopAttachments.wrappedTransformerOf(this.player.level()) == null) {
            return null;
        }

        return (ChunkResender) (Object) this.player.level().getChunkSource().chunkMap;
    }

    @Unique
    private boolean toroidal$ridesUncontrolled() {
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
    private double toroidal$continuousX(double requestedX, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMovePlayerPacket packet,
            @Share("continuous") LocalRef<Vec3> continuous) {
        double clampedX = original.call(requestedX);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !packet.hasPosition()) {
            return clampedX;
        }

        Vec3 seated = toroidal$seatPlayerMove(transformer, clampedX,
                original.call(packet.getZ(this.player.getZ())));
        continuous.set(seated);
        return seated.x;
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 1))
    private double toroidal$continuousZ(double requestedZ, Operation<Double> original,
            @Share("continuous") LocalRef<Vec3> continuous) {
        double clampedZ = original.call(requestedZ);
        Vec3 seated = continuous.get();
        return seated == null ? clampedZ : seated.z;
    }

    @Unique
    private Vec3 toroidal$seatPlayerMove(WorldFold transformer, double clampedX, double clampedZ) {
        Vec3 clamped = new Vec3(clampedX, this.player.getY(), clampedZ);
        ClientPosition mirror = this.toroidal$clientPosition;
        if (this.player.isPassenger()) {
            if (toroidal$ridesUncontrolled()) {
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

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void toroidal$wrapIntoBounds(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return;
        }

        DeckTransformation lap = transformer.foldTransformation(this.player.position());
        if (lap.isIdentity()) {
            return;
        }

        Vec3 wrapped = lap.apply(this.player.position());
        SeamSnap.withPassengers(this.player, lap);
        this.firstGoodX = wrapped.x;
        this.firstGoodZ = wrapped.z;
        this.lastGoodX = wrapped.x;
        this.lastGoodZ = wrapped.z;
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 0))
    private double toroidal$vehicleContinuousX(double clientX, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMoveVehiclePacket packet,
            @Share("vehicleContinuous") LocalRef<Vec3> continuous) {
        double clampedX = original.call(clientX);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clampedX;
        }

        double clampedZ = original.call(packet.position().z());
        this.toroidal$clientPosition.set(clampedX, clampedZ, MirrorWriter.VEHICLE_MOVE);
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
    private double toroidal$vehicleContinuousZ(double clientZ, Operation<Double> original,
            @Share("vehicleContinuous") LocalRef<Vec3> continuous) {
        double clampedZ = original.call(clientZ);
        Vec3 seated = continuous.get();
        return seated == null ? clampedZ : seated.z;
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void toroidal$wrapVehicleIntoBounds(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return;
        }

        Entity vehicle = this.player.getRootVehicle();
        if (vehicle == this.player) {
            return;
        }

        DeckTransformation lap = transformer.foldTransformation(vehicle.position());
        if (lap.isIdentity()) {
            return;
        }

        SeamSnap.withPassengers(vehicle, lap);

        this.vehicleFirstGoodX = vehicle.getX();
        this.vehicleFirstGoodZ = vehicle.getZ();
        this.vehicleLastGoodX = vehicle.getX();
        this.vehicleLastGoodZ = vehicle.getZ();
    }
}
