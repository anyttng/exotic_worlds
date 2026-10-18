package com.exoticworlds.compat.sable.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

// Mixin refuses to inject into a method a mixin of equal priority overwrote; Sable overwrites isFacingFrontText at 1000.
@Mixin(value = SignBlockEntity.class, priority = 1100)
public abstract class SignFacingFrameMixin {
    @WrapOperation(
            method = "isFacingFrontText",
            at = @At(value = "INVOKE", target = InjectionTargets.VECTOR3D_SUB))
    private Vector3d exotic_worlds$playerDeltaThroughSeam(Vector3d playerPosition, Vector3dc centre,
            Operation<Vector3d> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(((BlockEntity) (Object) this).getLevel());
        if (fold == null) {
            return original.call(playerPosition, centre);
        }

        return original.call(JomlVectors.seat(fold, JomlVectors.read(centre), playerPosition), centre);
    }
}
