package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

@Mixin(value = WispEntity.class, remap = false)
public abstract class WispEntityMixin {

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean exotic_worlds$touchThroughSeam(AABB wispBox, AABB targetBox, Operation<Boolean> original) {
        Entity wisp = (Entity) (Object) this;
        return original.call(wispBox, FoldedBoxQuery.toward(wisp, wisp.position(), targetBox));
    }
}
