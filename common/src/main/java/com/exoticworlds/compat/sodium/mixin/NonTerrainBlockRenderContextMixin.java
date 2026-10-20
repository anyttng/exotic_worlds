package com.exoticworlds.compat.sodium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.BlockOffsetSeed;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(NonTerrainBlockRenderContext.class)
public class NonTerrainBlockRenderContextMixin {
    @WrapOperation(
            method = "tesselateBlock",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_STATE_GET_OFFSET))
    private Vec3 exotic_worlds$seedModelOffset(BlockState state, BlockPos pos, Operation<Vec3> original,
            @Local(argsOnly = true) BlockAndTintGetter level) {
        return BlockOffsetSeed.seeded(level, state, () -> original.call(state, pos));
    }
}
