package com.exoticworlds.mixin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.FoldSource;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;
import com.exoticworlds.engine.seam.SeamAim;
import com.exoticworlds.engine.seam.SeamSteering;
import com.exoticworlds.engine.seam.circumnavigation.CircumnavigationTracker;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public class EntityMixin implements FoldSource {
    @Unique
    private static final String PUSH = "push(Lnet/minecraft/world/entity/Entity;)V";

    @WrapMethod(method = "distanceTo")
    private float exotic_worlds$distanceThroughSeam(Entity other, Operation<Float> original) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(other);
        }

        Vec3 otherPos = other.position();
        Vec3 nearest = exotic_worlds$nearestCopy(fold, otherPos);
        if (nearest == otherPos) {
            return original.call(other);
        }

        return (float) Math.sqrt(((Entity) (Object) this).distanceToSqr(nearest));
    }

    @WrapMethod(method = "distanceToSqr(DDD)D")
    private double exotic_worlds$distanceToSqrThroughSeam(double x, double y, double z, Operation<Double> original) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(x, y, z);
        }

        Vec3 nearest = exotic_worlds$nearestCopy(fold, new Vec3(x, y, z));
        return original.call(nearest.x, nearest.y, nearest.z);
    }

    @WrapMethod(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D")
    private double exotic_worlds$distanceToSqrVecThroughSeam(Vec3 pos, Operation<Double> original) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(pos);
        }

        return original.call(exotic_worlds$nearestCopy(fold, pos));
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;D)Z")
    private boolean exotic_worlds$closerThanThroughSeam(Entity other, double distance, Operation<Boolean> original) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(other, distance);
        }

        Vec3 otherPos = other.position();
        Vec3 nearest = exotic_worlds$nearestCopy(fold, otherPos);
        if (nearest == otherPos) {
            return original.call(other, distance);
        }

        return ((Entity) (Object) this).position().closerThan(nearest, distance);
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;DD)Z")
    private boolean exotic_worlds$closerThanXZThroughSeam(Entity other, double distanceXZ, double distanceY,
            Operation<Boolean> original) {
        WorldFold fold = exotic_worlds$wrappedFold();
        if (fold == null) {
            return original.call(other, distanceXZ, distanceY);
        }

        Vec3 otherPos = other.position();
        Vec3 nearest = exotic_worlds$nearestCopy(fold, otherPos);
        if (nearest == otherPos) {
            return original.call(other, distanceXZ, distanceY);
        }

        Vec3 delta = nearest.subtract(((Entity) (Object) this).position());
        return Mth.lengthSquared(delta.x, delta.z) < Mth.square(distanceXZ)
                && Mth.square(delta.y) < Mth.square(distanceY);
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
        BlockPos folded = WorldLoopAttachments.foldOf(level).fold(raw);
        return folded == raw ? found : Optional.of(folded);
    }

    @ModifyReturnValue(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("RETURN"))
    private BlockPos exotic_worlds$canonicalOnPos(BlockPos raw) {
        return WorldLoopAttachments.foldOf(((Entity) (Object) this).level()).fold(raw);
    }

    @Inject(method = "moveTo(DDDFF)V", at = @At("TAIL"))
    private void exotic_worlds$sampleTravelOnPlacement(double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            CircumnavigationTracker.sample(player);
        }
    }

    @Inject(method = "absMoveTo(DDDFF)V", at = @At("TAIL"))
    private void exotic_worlds$sampleTravelOnTeleport(double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            CircumnavigationTracker.sample(player);
        }
    }

    @Unique
    private @Nullable Level exotic_worlds$foldLevel;

    @Unique
    private WorldFold exotic_worlds$fold;

    @Override
    public @Nullable WorldFold exotic_worlds$wrappedFold() {
        Level level = ((Entity) (Object) this).level();
        if (level != this.exotic_worlds$foldLevel) {
            this.exotic_worlds$fold = WorldLoopAttachments.foldOf(level);
            this.exotic_worlds$foldLevel = level;
        }

        WorldFold fold = this.exotic_worlds$fold;
        return fold.isWrapped() ? fold : null;
    }

    @Unique
    private Vec3 exotic_worlds$nearestCopy(WorldFold fold, Vec3 target) {
        return fold.nearestCopy(((Entity) (Object) this).position(), target);
    }
}
