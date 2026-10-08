package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Mixin(value = LogisticalTransporterBase.class, remap = false)
public abstract class LogisticalTransporterBaseMixin {
    @WrapOperation(
            method = "insert(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/core/BlockPos;"
                    + "Lmekanism/common/lib/inventory/TransitRequest;Lmekanism/api/text/EnumColor;IZ"
                    + "Lmekanism/common/content/network/transmitter/LogisticalTransporterBase$PathCalculator;)"
                    + "Lmekanism/common/lib/inventory/TransitRequest$TransitResponse;",
            at = @At(value = "INVOKE", target = "Lmekanism/common/util/WorldUtils;sideDifference("
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Direction;"))
    private Direction toroidal$outputterSide(BlockPos pos, BlockPos outputter, Operation<Direction> original) {
        return original.call(pos, MekanismSeam.nearestCopy(this.toroidal$self().getLevel(), pos, outputter));
    }

    @WrapOperation(method = "onUpdateServer",
            at = @At(value = "INVOKE",
                    target = "Lmekanism/common/content/transporter/TransporterStack;getSide(JJ)"
                            + "Lnet/minecraft/core/Direction;"))
    private Direction toroidal$nextSide(TransporterStack stack, long pos, long target,
            Operation<Direction> original) {
        return original.call(stack, pos, MekanismSeam.nearestCopy(this.toroidal$self().getLevel(), pos, target));
    }

    @Unique
    private LogisticalTransporterBase toroidal$self() {
        return (LogisticalTransporterBase) (Object) this;
    }
}
