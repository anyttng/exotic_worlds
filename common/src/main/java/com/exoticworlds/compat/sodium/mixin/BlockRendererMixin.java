package com.exoticworlds.compat.sodium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.BlockOffsetSeed;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(BlockRenderer.class)
public abstract class BlockRendererMixin extends AbstractBlockRenderContext {
    @WrapOperation(
            method = "renderModel",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_STATE_GET_OFFSET))
    private Vec3 toroidal$seedModelOffset(BlockState state, BlockPos pos, Operation<Vec3> original) {
        try (BlockOffsetSeed ignored = BlockOffsetSeed.seededBy(this.level)) {
            return original.call(state, pos);
        }
    }
}
