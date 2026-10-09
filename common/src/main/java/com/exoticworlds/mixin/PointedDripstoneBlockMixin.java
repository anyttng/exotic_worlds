package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.BlockOffsetSeed;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {
    @WrapOperation(
            method = "spawnDripParticle(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_STATE_GET_OFFSET))
    private static Vec3 toroidal$seedDripOffset(BlockState state, BlockPos pos, Operation<Vec3> original,
            @Local(argsOnly = true) Level level) {
        try (BlockOffsetSeed _ = BlockOffsetSeed.seededBy(level)) {
            return original.call(state, pos);
        }
    }
}
