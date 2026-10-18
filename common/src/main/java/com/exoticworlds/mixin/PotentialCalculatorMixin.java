package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.TransformerHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.PotentialCalculator;

@Mixin(PotentialCalculator.class)
public class PotentialCalculatorMixin implements TransformerHolder {
    @Unique
    private WorldFold exotic_worlds$transformer = WorldFolds.NOOP;

    @Override
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$transformer;
    }

    @Override
    public void exotic_worlds$setTransformer(WorldFold transformer) {
        this.exotic_worlds$transformer = transformer;
    }

    @WrapOperation(
            method = "getPotentialEnergyChange",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/PotentialCalculator$PointCharge;getPotentialChange(Lnet/minecraft/core/BlockPos;)D"))
    private double exotic_worlds$measureChargeThroughSeam(PotentialCalculator.PointCharge charge, BlockPos candidate,
            Operation<Double> original) {
        return original.call(charge, this.exotic_worlds$transformer.nearestCopy(charge.pos, candidate));
    }
}
