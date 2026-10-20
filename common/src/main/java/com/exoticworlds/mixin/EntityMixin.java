package com.exoticworlds.mixin;

import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.ClientPosition;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamRange;
import com.exoticworlds.engine.seam.SeamSteering;
import com.exoticworlds.engine.seam.VehicleDismountResync;
import com.exoticworlds.engine.seam.circumnavigation.CircumnavigationTracker;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public class EntityMixin implements TransformerSource {
    @Unique
    private static final String PUSH = "push(Lnet/minecraft/world/entity/Entity;)V";

    @WrapMethod(method = "distanceTo")
    private float exotic_worlds$distanceThroughSeam(Entity other, Operation<Float> original) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return original.call(other);
        }

        return (float) Math.sqrt(SeamRange.sqr(transformer, ((Entity) (Object) this).position(), other.position()));
    }

    @WrapMethod(method = "distanceToSqr(DDD)D")
    private double exotic_worlds$distanceToSqrThroughSeam(double x, double y, double z, Operation<Double> original) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return original.call(x, y, z);
        }

        Entity self = (Entity) (Object) this;
        return SeamRange.sqr(self, self.position(), x, y, z);
    }

    @WrapMethod(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D")
    private double exotic_worlds$distanceToSqrVecThroughSeam(Vec3 pos, Operation<Double> original) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return original.call(pos);
        }

        return SeamRange.sqr(transformer, ((Entity) (Object) this).position(), pos);
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;D)Z")
    private boolean exotic_worlds$closerThanThroughSeam(Entity other, double distance, Operation<Boolean> original) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return original.call(other, distance);
        }

        Entity self = (Entity) (Object) this;
        return SeamRange.closerThan(self, self.position(), other.position(), distance);
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;DD)Z")
    private boolean exotic_worlds$closerThanXZThroughSeam(Entity other, double distanceXZ, double distanceY,
            Operation<Boolean> original) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        if (transformer == null) {
            return original.call(other, distanceXZ, distanceY);
        }

        return SeamRange.closerThan(transformer, ((Entity) (Object) this).position(), other.position(), distanceXZ,
                distanceY);
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/commands/arguments/EntityAnchorArgument$Anchor;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), argsOnly = true)
    private Vec3 exotic_worlds$lookAtNearestCopy(Vec3 pos) {
        return SeamSteering.nearestCopy((Entity) (Object) this, pos);
    }

    @WrapOperation(method = PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X, ordinal = 0))
    private double exotic_worlds$pusherNearX(Entity other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.X, original.call(other));
    }

    @WrapOperation(method = PUSH, at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z, ordinal = 0))
    private double exotic_worlds$pusherNearZ(Entity other, Operation<Double> original) {
        return SeamAim.nearestCoord((Entity) (Object) this, other, Direction.Axis.Z, original.call(other));
    }

    @ModifyArg(
            method = "applyEffectsFromBlocks()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity$Movement;<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private Vec3 exotic_worlds$foldBridgeThroughSeam(Vec3 from) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        Vec3 position = ((Entity) (Object) this).position();
        return NearestCopy.toward(transformer, position, from);
    }

    @ModifyArg(
            method = "getRelativePortalPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/portal/PortalShape;getRelativePosition(Lnet/minecraft/util/BlockUtil$FoundRectangle;Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/EntityDimensions;)Lnet/minecraft/world/phys/Vec3;"),
            index = 2)
    private Vec3 exotic_worlds$portalPositionNearestCorner(Vec3 position, @Local(argsOnly = true) BlockUtil.FoundRectangle portalArea) {
        WorldFold transformer = exotic_worlds$wrappedTransformer();
        return NearestCopy.toward(transformer, Vec3.atLowerCornerOf(portalArea.minCorner), position);
    }

    @WrapOperation(
            method = "checkSupportingBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;findSupportingBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/Optional;"))
    private Optional<BlockPos> exotic_worlds$storeCanonicalSupportingBlock(Level level, Entity source, AABB box,
            Operation<Optional<BlockPos>> original) {
        Optional<BlockPos> found = original.call(level, source, box);
        if (found.isEmpty()) {
            return found;
        }

        BlockPos raw = found.get();
        BlockPos folded = WorldLoopAttachments.transformerOf(level).fold(raw);
        return folded == raw ? found : Optional.of(folded);
    }

    @ModifyReturnValue(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("RETURN"))
    private BlockPos exotic_worlds$canonicalOnPos(BlockPos raw) {
        return WorldLoopAttachments.transformerOf(((Entity) (Object) this).level()).fold(raw);
    }

    @Inject(method = "snapTo(DDDFF)V", at = @At("TAIL"))
    private void exotic_worlds$followPlayerPlacement(double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            ClientPosition.rebase(player);
            CircumnavigationTracker.sample(player);
        }
    }

    @Inject(
            method = "teleportSetPosition(Lnet/minecraft/world/entity/PositionMoveRotation;"
                    + "Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V",
            at = @At("TAIL"))
    private void exotic_worlds$sampleTravelOnTeleport(PositionMoveRotation currentValues, PositionMoveRotation destination,
            Set<Relative> relatives, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            CircumnavigationTracker.sample(player);
        }
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void exotic_worlds$resyncVehicleOnDismount(CallbackInfo ci) {
        VehicleDismountResync.resyncAfterDismount((Entity) (Object) this);
    }

    @Unique
    private @Nullable Level exotic_worlds$transformerLevel;

    @Unique
    private WorldFold exotic_worlds$transformer;

    @Override
    public @Nullable WorldFold exotic_worlds$wrappedTransformer() {
        Level level = ((Entity) (Object) this).level();
        if (level != this.exotic_worlds$transformerLevel) {
            this.exotic_worlds$transformer = WorldLoopAttachments.transformerOf(level);
            this.exotic_worlds$transformerLevel = level;
        }

        WorldFold transformer = this.exotic_worlds$transformer;
        return transformer.isWrapped() ? transformer : null;
    }
}
