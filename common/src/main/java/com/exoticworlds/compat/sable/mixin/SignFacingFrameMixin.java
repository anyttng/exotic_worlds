package com.exoticworlds.compat.sable.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

// Sable overwrites isFacingFrontText at 1000 and measures the player from the sign's centre out of its sub-level; the
// vanilla SignFacingMixin has nothing to wrap in that body, so this one takes the player to the copy nearest that centre.
@Mixin(value = SignBlockEntity.class, priority = 1100)
public abstract class SignFacingFrameMixin {
    @WrapOperation(
            method = "isFacingFrontText",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3d;sub(Lorg/joml/Vector3dc;)Lorg/joml/Vector3d;"))
    private Vector3d toroidal$playerDeltaThroughSeam(Vector3d playerPosition, Vector3dc centre,
            Operation<Vector3d> original) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(((BlockEntity) (Object) this).getLevel());
        if (fold == null) {
            return original.call(playerPosition, centre);
        }

        return original.call(JomlVectors.seat(fold, JomlVectors.read(centre), playerPosition), centre);
    }
}
