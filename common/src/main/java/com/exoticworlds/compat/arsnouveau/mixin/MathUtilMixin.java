package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.hollingsworth.arsnouveau.api.util.MathUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = MathUtil.class, remap = false)
public abstract class MathUtilMixin {
    @WrapOperation(method = "traceEntities", at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private static AABB exotic_worlds$candidateBoxThroughSeam(Entity candidate, Operation<AABB> original,
            @Local(argsOnly = true, ordinal = 0) Vec3 startVec) {
        return FoldedBoxQuery.toward(candidate, startVec, original.call(candidate));
    }
}
