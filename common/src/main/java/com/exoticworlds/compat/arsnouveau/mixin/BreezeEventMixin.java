package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.event.BreezeEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.phys.Vec3;

@Mixin(value = BreezeEvent.class, remap = false)
public abstract class BreezeEventMixin {
    @WrapOperation(method = "tick(Z)V", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/common/entity/EntityProjectileSpell;shoot(DDDFF)V"))
    private void exotic_worlds$returnShotThroughSeam(EntityProjectileSpell shot, double x, double y, double z,
            float velocity, float inaccuracy, Operation<Void> original) {
        Vec3 delta = SeamAim.foldDelta(shot, new Vec3(x, y, z));
        original.call(shot, delta.x, delta.y, delta.z, velocity, inaccuracy);
    }
}
