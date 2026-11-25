package com.exoticworlds.compat.apprenticecodex.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import jp.aquafactory.apprenticecodex.spell.frostrune.FrostRune;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = FrostRune.class, remap = false)
public abstract class FrostRuneMixin {
    @WrapOperation(method = "resolveVisualNorth", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;getCenter()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$runeCenterThroughSeam(BlockPos place, Operation<Vec3> original,
            @Local(argsOnly = true) LivingEntity entity) {
        return SeamRange.toward(entity, entity.position(), original.call(place));
    }
}
