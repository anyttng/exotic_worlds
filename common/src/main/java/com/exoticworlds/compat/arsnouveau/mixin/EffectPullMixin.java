package com.exoticworlds.compat.arsnouveau.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamAim;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectPull;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = EffectPull.class, remap = false)
public abstract class EffectPullMixin {
    @WrapOperation(method = "onResolveEntity", at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 exotic_worlds$pullDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true) LivingEntity shooter) {
        return SeamAim.foldDelta(shooter, original.call(x, y, z));
    }
}
