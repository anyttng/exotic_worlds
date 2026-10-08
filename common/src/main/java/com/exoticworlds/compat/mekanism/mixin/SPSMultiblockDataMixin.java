package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.mekanism.MultiblockFrames;

import mekanism.common.content.sps.SPSMultiblockData;

import net.minecraft.core.BlockPos;

@Mixin(value = SPSMultiblockData.class, remap = false)
public class SPSMultiblockDataMixin {
    @WrapOperation(method = "handlesSound", at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private boolean toroidal$casingInFrame(BlockPos casing, Object soundPos, Operation<Boolean> original) {
        return original.call(MultiblockFrames.seatOnto((Object) this, casing), soundPos);
    }
}
