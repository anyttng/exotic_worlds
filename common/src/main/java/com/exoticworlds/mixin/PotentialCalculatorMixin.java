package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.PotentialCalculator;

@Mixin(PotentialCalculator.class)
public class PotentialCalculatorMixin implements FoldHolder {
    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(WorldFold fold) {
        this.exotic_worlds$fold = fold;
    }

    @WrapOperation(
            method = "getPotentialEnergyChange",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/PotentialCalculator$PointCharge;getPotentialChange(Lnet/minecraft/core/BlockPos;)D"))
    private double exotic_worlds$measureChargeThroughSeam(PotentialCalculator.PointCharge charge, BlockPos candidate,
            Operation<Double> original) {
        return original.call(charge, this.exotic_worlds$fold.nearestCopy(charge.pos, candidate));
    }
}
