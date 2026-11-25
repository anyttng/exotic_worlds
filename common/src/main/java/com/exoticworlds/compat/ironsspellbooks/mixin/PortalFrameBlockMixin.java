package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.ironsspellbooks.IronsSpellbooksInjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.block.portal_frame.PortalFrameBlock;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(value = PortalFrameBlock.class, remap = false)
public abstract class PortalFrameBlockMixin {

    @WrapOperation(method = IronsSpellbooksInjectionTargets.BLOCK_ENTITY_INSIDE,
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$touchThroughSeam(AABB entityBox, AABB shapeBox, Operation<Boolean> original,
            @Local(argsOnly = true) Entity entity) {
        return original.call(entityBox, FoldedBoxQuery.toward(entity, entity.position(), shapeBox));
    }
}
