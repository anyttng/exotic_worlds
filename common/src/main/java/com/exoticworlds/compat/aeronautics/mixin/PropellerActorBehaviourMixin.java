package com.exoticworlds.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
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
    private static final String PROPELLER_CENTRE =
            "Ldev/ryanhcode/sable/companion/math/Pose3d;transformPosition(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;";
    private static final String AIRFLOW_RAY =
            "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;";

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = PROPELLER_CENTRE))
    private Vector3d toroidal$centreInTheEntityFrame(Pose3d pose, Vector3d centre, Operation<Vector3d> original,
            @Local Entity entity) {
        return SeamFrame.with(entity.level(), entity::position, () -> original.call(pose, centre));
    }

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = InjectionTargets.VECTOR3D_SUB))
    private Vector3d toroidal$offsetFromTheNearestCentre(Vector3d entityCentre, Vector3dc centre,
            Operation<Vector3d> original, @Local Entity entity, @Local @Nullable SubLevel subLevel) {
        if (subLevel != null) {
            return original.call(entityCentre, centre);
        }

        Level level = entity.level();
        Vec3 raw = JomlVectors.read(centre);
        Vec3 seated = WorldLoopAttachments.transformerOfReader(level)
                .nearestCopy(ForeignFrames.seatInWorld(level, entity.position()), raw);
        return original.call(entityCentre, seated == raw ? centre : JomlVectors.write(seated, new Vector3d()));
    }

    @WrapOperation(method = "pushEntities", at = @At(value = "INVOKE", target = AIRFLOW_RAY))
    private BlockHitResult toroidal$rayInTheEntityFrame(Level level, ClipContext context,
            Operation<BlockHitResult> original, @Local Entity entity) {
        return SeamFrame.with(level, entity::position, () -> original.call(level, context));
    }
}
