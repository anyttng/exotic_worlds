package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "io/redspace/ironsspellbooks/entity/spells/ender_chain/EnderChain", remap = false)
public abstract class EnderChainMixin {
    @ModifyExpressionValue(method = {"applyRestraint(Lnet/minecraft/world/entity/Entity;)V", "repositionParts()V"},
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_POSITION))
    private Vec3 exotic_worlds$victimBesideChain(Vec3 victim) {
        return SeamSteering.nearestCopy((Entity) (Object) this, victim);
    }
}
