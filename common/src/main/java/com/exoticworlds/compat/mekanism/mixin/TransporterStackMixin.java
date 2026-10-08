package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismInjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;

import net.minecraft.core.Direction;

@Mixin(value = TransporterStack.class, remap = false)
public class TransporterStackMixin {
    @WrapOperation(
            method = "getSide(Lmekanism/common/content/network/transmitter/LogisticalTransporterBase;)"
                    + "Lnet/minecraft/core/Direction;",
            at = @At(value = "INVOKE", target = MekanismInjectionTargets.WORLD_UTILS_SIDE_DIFFERENCE_PACKED),
            require = 2,
            expect = 2)
    private Direction toroidal$sideTheShortWayRound(long pos, long other, Operation<Direction> original,
            @Local(argsOnly = true) LogisticalTransporterBase transporter) {
        return original.call(pos, MekanismSeam.nearestCopy(transporter.getLevel(), pos, other));
    }
}
