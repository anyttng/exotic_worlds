package com.exoticworlds.compat.ironsspellbooks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.ironsspellbooks.IronsSpellbooksInjectionTargets;
import com.exoticworlds.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.redspace.ironsspellbooks.block.BrazierBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

@Mixin(value = BrazierBlock.class, remap = false)
public abstract class BrazierBlockMixin {

    @WrapOperation(method = IronsSpellbooksInjectionTargets.BLOCK_ENTITY_INSIDE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I"))
    private int exotic_worlds$blockXBesideEntity(BlockPos pos, Operation<Integer> original,
            @Local(argsOnly = true) Entity entity) {
        return original.call(SeamSteering.nearestCopy(entity, pos));
    }

    @WrapOperation(method = IronsSpellbooksInjectionTargets.BLOCK_ENTITY_INSIDE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getZ()I"))
    private int exotic_worlds$blockZBesideEntity(BlockPos pos, Operation<Integer> original,
            @Local(argsOnly = true) Entity entity) {
        return original.call(SeamSteering.nearestCopy(entity, pos));
    }
}
