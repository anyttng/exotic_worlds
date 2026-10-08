package com.exoticworlds.compat.mekanism.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.mekanism.MekanismSeam;

import mekanism.common.content.transporter.TransporterPathfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = TransporterPathfinder.Pathfinder.class, remap = false)
public class TransporterPathfinderPathfinderMixin {
    @Shadow
    @Final
    private Level world;

    @WrapOperation(method = "find",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos$MutableBlockPos;asLong()J"))
    private long toroidal$foldNode(BlockPos.MutableBlockPos neighbor, Operation<Long> original) {
        return MekanismSeam.fold(this.world, original.call(neighbor));
    }

    @WrapOperation(method = {"find", "isValidDestination"},
            at = @At(value = "INVOKE", target = "Lmekanism/common/util/WorldUtils;distanceBetween("
                    + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)D"),
            require = 4,
            expect = 4)
    private double toroidal$distanceTheShortWayRound(BlockPos start, BlockPos end, Operation<Double> original) {
        return original.call(start, MekanismSeam.nearestCopy(this.world, start, end));
    }

    @WrapOperation(method = "isValidDestination",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private boolean toroidal$sameBlockAcrossTheEdge(BlockPos neighbor, Object finalNode,
            Operation<Boolean> original) {
        Object folded = finalNode instanceof BlockPos pos ? MekanismSeam.fold(this.world, pos) : finalNode;
        return original.call(MekanismSeam.fold(this.world, neighbor), folded);
    }
}
