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
    private WorldFold toroidal$transformer = WorldFolds.NOOP;

    @Override
    public WorldFold toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldFold transformer) {
        this.toroidal$transformer = transformer;
    }

    @WrapOperation(
            method = "getPotentialEnergyChange",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/PotentialCalculator$PointCharge;getPotentialChange(Lnet/minecraft/core/BlockPos;)D"))
    private double toroidal$measureChargeThroughSeam(PotentialCalculator.PointCharge charge, BlockPos candidate,
            Operation<Double> original) {
        return original.call(charge, this.toroidal$transformer.nearestCopy(charge.pos, candidate));
    }
}
