package com.exoticworlds.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.sable.SeamFrame;
import com.exoticworlds.core.ForeignFrames;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(value = PropellerActorBehaviour.class, remap = false)
public abstract class PropellerActorBehaviourMixin {
    @Unique
    private static final String PROPELLER_CENTER =
            "Ldev/ryanhcode/sable/companion/math/Pose3d;transformPosition(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;";
    @Unique
    private static final String AIRFLOW_RAY =
            "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;";

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = PROPELLER_CENTER))
    private Vector3d exotic_worlds$centerInTheEntityFrame(Pose3d pose, Vector3d center, Operation<Vector3d> original,
            @Local Entity entity) {
        return SeamFrame.with(entity.level(), entity, Entity::position, original, pose, center);
    }

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = InjectionTargets.VECTOR3D_SUB))
    private Vector3d exotic_worlds$offsetFromTheNearestCenter(Vector3d entityCenter, Vector3dc center,
            Operation<Vector3d> original, @Local Entity entity, @Local @Nullable SubLevel subLevel) {
        if (subLevel != null) {
            return original.call(entityCenter, center);
        }

        Level level = entity.level();
        Vec3 raw = JomlVectors.read(center);
        Vec3 seated = WorldLoopAttachments.foldOfReader(level)
                .nearestCopy(ForeignFrames.seatInWorld(level, entity.position()), raw);
        return original.call(entityCenter, seated == raw ? center : JomlVectors.write(seated, new Vector3d()));
    }

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = AIRFLOW_RAY))
    private BlockHitResult exotic_worlds$rayInTheEntityFrame(Level level, ClipContext context,
            Operation<BlockHitResult> original, @Local Entity entity) {
        return SeamFrame.with(level, entity, Entity::position, original, level, context);
    }
}
