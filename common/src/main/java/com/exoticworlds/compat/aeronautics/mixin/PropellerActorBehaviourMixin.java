package com.exoticworlds.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.sable.SeamFrame;

import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.ryanhcode.sable.companion.math.Pose3d;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(value = PropellerActorBehaviour.class, remap = false)
public abstract class PropellerActorBehaviourMixin {
    private static final String PROPELLER_CENTRE =
            "Ldev/ryanhcode/sable/companion/math/Pose3d;transformPosition(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;";
    private static final String AIRFLOW_RAY =
            "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;";

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = PROPELLER_CENTRE))
    private Vector3d toroidal$centreInTheEntityFrame(Pose3d pose, Vector3d centre, Operation<Vector3d> original,
            @Local Entity entity) {
        return SeamFrame.with(entity.level(), entity::position, () -> original.call(pose, centre));
    }

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = AIRFLOW_RAY))
    private BlockHitResult toroidal$rayInTheEntityFrame(Level level, ClipContext context,
            Operation<BlockHitResult> original, @Local Entity entity) {
        return SeamFrame.with(level, entity::position, () -> original.call(level, context));
    }
}
