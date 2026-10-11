package com.exoticworlds.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.net.MeasuredReach;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SeamSnap;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Unique
    private static final double PARTICLE_RANGE = 32.0;

    @Unique
    private static final double OVERRIDDEN_PARTICLE_RANGE = 512.0;

    @Unique
    private static final double EXPLOSION_RANGE = 64.0;

    @Unique
    private static final String SEND_PARTICLES_TO_PLAYER =
            "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z";

    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickContainers(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ((LevelBindable) level.getBlockTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getFluidTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getRaids()).toroidal$bindLevel(level);
        ((LevelBindable) this.entityManager).toroidal$bindLevel(level);
        ((LevelBindable) level.getPathTypeCache()).toroidal$bindLevel(level);
    }

    @Inject(method = "getWorldBorder", at = @At("RETURN"))
    private void toroidal$bindBorderToLevelShape(CallbackInfoReturnable<WorldBorder> cir) {
        WorldFold transformer = WorldLoopAttachments.transformerOf((ServerLevel) (Object) this);
        TransformerHolder border = (TransformerHolder) cir.getReturnValue();
        if (border.toroidal$transformer() != transformer) {
            border.toroidal$setTransformer(transformer);
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void toroidal$wrapEntityIntoBounds(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player) {
            return;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return;
        }

        DeckTransformation lap = transformer.foldTransformation(entity.position());
        if (!lap.isIdentity()) {
            SeamSnap.withPassengers(entity, lap);
        }
    }

    @WrapOperation(
            method = "waitForEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ChunkPos;rangeClosed(Lnet/minecraft/world/level/ChunkPos;I)Ljava/util/stream/Stream;"))
    private Stream<ChunkPos> toroidal$waitOnPhysicalChunks(
            ChunkPos center, int radius, Operation<Stream<ChunkPos>> original) {
        Stream<ChunkPos> square = original.call(center, radius);
        ServerLevel level = (ServerLevel) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return square;
        }

        return square.map(chunkPos -> transformer.fold(chunkPos)).distinct();
    }

    @WrapOperation(
            method = SEND_PARTICLES_TO_PLAYER,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$particleRangeThroughSeam(BlockPos listener, Position particle, double range,
            Operation<Boolean> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return original.call(listener, particle, range);
        }

        return SeamRange.sqr(transformer, Vec3.atCenterOf(listener), particle) < range * range;
    }

    @WrapOperation(
            method = SEND_PARTICLES_TO_PLAYER,
            at = @At(value = "INVOKE", target = InjectionTargets.SERVER_GAME_PACKET_LISTENER_SEND))
    private void toroidal$measureParticleReach(ServerGamePacketListenerImpl connection, Packet<?> packet,
            Operation<Void> original, @Local(argsOnly = true) boolean overrideLimiter) {
        try (MeasuredReach _ = MeasuredReach.measuring(
                overrideLimiter ? OVERRIDDEN_PARTICLE_RANGE : PARTICLE_RANGE)) {
            original.call(connection, packet);
        }
    }

    @WrapOperation(
            method = "explode",
            at = @At(value = "INVOKE", target = InjectionTargets.SERVER_GAME_PACKET_LISTENER_SEND))
    private void toroidal$measureExplosionReach(ServerGamePacketListenerImpl connection, Packet<?> packet,
            Operation<Void> original) {
        try (MeasuredReach _ = MeasuredReach.measuring(EXPLOSION_RANGE)) {
            original.call(connection, packet);
        }
    }

    @WrapOperation(
            method = "lambda$globalLevelEvent$0",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_AT_CENTER_OF))
    private Vec3 toroidal$globalEventTowardListener(Vec3i pos, Operation<Vec3> original,
            @Local(argsOnly = true) ServerPlayer player) {
        return SeamSteering.nearestCopy(player, original.call(pos));
    }

    @WrapOperation(
            method = "destroyBlockProgress",
            at = @At(value = "INVOKE", target = InjectionTargets.SERVER_PLAYER_GET_X))
    private double toroidal$crackListenerXThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true) BlockPos blockPos) {
        return toroidal$listenerCoord(player, Direction.Axis.X, blockPos, original);
    }

    @WrapOperation(
            method = "destroyBlockProgress",
            at = @At(value = "INVOKE", target = InjectionTargets.SERVER_PLAYER_GET_Z))
    private double toroidal$crackListenerZThroughSeam(ServerPlayer player, Operation<Double> original,
            @Local(argsOnly = true) BlockPos blockPos) {
        return toroidal$listenerCoord(player, Direction.Axis.Z, blockPos, original);
    }

    @Unique
    private static double toroidal$listenerCoord(ServerPlayer player, Direction.Axis axis, BlockPos blockPos,
            Operation<Double> original) {
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return transformer == null
                ? original.call(player)
                : NearestCopy.toward(transformer, axis, Vec3.atLowerCornerOf(blockPos), player.position());
    }

    @WrapMethod(method = "tickPrecipitation")
    private void toroidal$bindPrecipitationTransformer(BlockPos pos, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        GenerationTransformerContext.runWithTransformer(
                WorldLoopAttachments.transformerOf(level), () -> original.call(pos));
    }
}
