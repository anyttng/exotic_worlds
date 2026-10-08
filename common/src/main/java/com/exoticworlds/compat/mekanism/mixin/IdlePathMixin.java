package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MekanismInjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.content.network.InventoryNetwork;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterPathfinder;
import mekanism.common.content.transporter.TransporterStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(value = TransporterPathfinder.IdlePath.class, remap = false)
public class IdlePathMixin {
    @Shadow
    @Final
    private InventoryNetwork network;

    @Shadow
    @Final
    private BlockPos start;

    @WrapOperation(method = "loopSide",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;asLong()J"))
    private long toroidal$foldStoredNode(BlockPos pos, Operation<Long> original) {
        return MekanismSeam.fold(this.network.getWorld(), original.call(pos));
    }

    @WrapOperation(method = "getDestination",
            at = @At(value = "INVOKE", target = MekanismInjectionTargets.WORLD_UTILS_RELATIVE_POS))
    private long toroidal$foldStoredEnd(long pos, Direction side, Operation<Long> original) {
        return MekanismSeam.fold(this.network.getWorld(), original.call(pos, side));
    }

    @WrapOperation(method = "loopSide",
            at = @At(value = "INVOKE", target = "Lmekanism/common/content/transporter/TransporterStack;"
                    + "canInsertToTransporter(Lmekanism/common/content/network/transmitter/LogisticalTransporterBase;"
                    + "Lnet/minecraft/core/Direction;"
                    + "Lmekanism/common/content/network/transmitter/LogisticalTransporterBase;)Z"))
    private boolean toroidal$stopAfterOneLap(TransporterStack stack, LogisticalTransporterBase transmitter,
            Direction from, LogisticalTransporterBase transporterFrom, Operation<Boolean> original,
            @Local BlockPos pos) {
        if (MekanismSeam.fold(this.network.getWorld(), pos).equals(this.start)) {
            return false;
        }

        return original.call(stack, transmitter, from, transporterFrom);
    }
}
