package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.engine.seam.BlockOffsetSeed;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateOffsetSeedMixin {
    @WrapOperation(
            method = "getOffset",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$OffsetFunction;evaluate(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$seedFromCanonical(BlockBehaviour.OffsetFunction function, BlockState state, BlockGetter getter,
            BlockPos pos, Operation<Vec3> original) {
        return original.call(function, state, getter, BlockOffsetSeed.seedPosition(getter, pos));
    }
}
