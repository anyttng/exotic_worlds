package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.lib.multiblock.IMultiblock;
import mekanism.common.lib.multiblock.IValveHandler;

import net.minecraft.core.BlockPos;

@Mixin(value = IValveHandler.class, remap = false)
public interface IValveHandlerMixin {
    @WrapOperation(method = "triggerValveTransfer", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private static boolean toroidal$valveInFrame(BlockPos pos, Object location, Operation<Boolean> original,
            @Local(argsOnly = true) IMultiblock<?> multiblock) {
        return original.call(location instanceof BlockPos valve
                ? MekanismSeam.nearestCopy(multiblock.getLevel(), valve, pos)
                : pos, location);
    }
}
