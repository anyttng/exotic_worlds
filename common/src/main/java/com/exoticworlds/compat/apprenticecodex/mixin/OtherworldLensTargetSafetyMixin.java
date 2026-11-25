package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "jp.aquafactory.apprenticecodex.spell.otherworldlens.OtherworldLensTargetSafety", remap = false)
public abstract class OtherworldLensTargetSafetyMixin {
    @WrapOperation(method = "validate", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private static boolean exotic_worlds$sameBlockThroughSeam(BlockPos target, Object other,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity caster) {
        return original.call(target,
                other instanceof BlockPos picked ? SeamRange.toward(caster, target, picked) : other);
    }

    @WrapOperation(method = "validate", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private static double exotic_worlds$hitOffsetThroughSeam(Vec3 picked, Vec3 target, Operation<Double> original,
            @Local(argsOnly = true) LivingEntity caster) {
        return original.call(picked, SeamRange.toward(caster, picked, target));
    }
}
