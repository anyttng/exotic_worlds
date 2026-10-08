package com.exoticworlds.compat.lithium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions")
public class LithiumEntityCollisionsMixin {
    @WrapOperation(
            method = "appendEntityCollisions",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private static AABB toroidal$collisionBoxTowardMover(Entity other, Operation<AABB> original,
            @Local(argsOnly = true) AABB box) {
        return FoldedBoxQuery.toward(((TransformerSource) other).toroidal$wrappedTransformer(), box.getCenter(),
                original.call(other));
    }
}
