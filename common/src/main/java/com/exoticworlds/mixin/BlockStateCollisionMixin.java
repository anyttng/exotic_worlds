package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCollisionMixin {
    @ModifyVariable(
            method = InjectionTargets.BLOCK_STATE_BASE_GET_COLLISION_SHAPE,
            at = @At("HEAD"),
            argsOnly = true)
    private BlockPos toroidal$canonicalCollisionPos(BlockPos pos, @Local(argsOnly = true) BlockGetter getter) {
        return getter instanceof Level level ? WorldLoopAttachments.transformerOf(level).fold(pos) : pos;
    }
}
