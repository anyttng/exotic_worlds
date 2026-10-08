package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismInjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.acceptor.NetworkAcceptorCache;

import net.minecraft.core.Direction;

@Mixin(value = NetworkAcceptorCache.class, remap = false)
public class NetworkAcceptorCacheMixin {
    @WrapOperation(method = "updateTransmitterOnSide",
            at = @At(value = "INVOKE", target = MekanismInjectionTargets.WORLD_UTILS_RELATIVE_POS))
    private long toroidal$foldAcceptorKey(long pos, Direction side, Operation<Long> original,
            @Local(argsOnly = true) Transmitter<?, ?, ?> transmitter) {
        return MekanismSeam.fold(transmitter.getLevel(), original.call(pos, side));
    }
}
