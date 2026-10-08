package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.seam.BlockOffsetSeed;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @WrapOperation(
            method = "submitBlockDestroyAnimation",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_STATE_GET_OFFSET))
    private Vec3 toroidal$seedDestroyOffset(BlockState state, BlockPos pos, Operation<Vec3> original) {
        try (BlockOffsetSeed ignored = BlockOffsetSeed.seededBy(Minecraft.getInstance().level)) {
            return original.call(state, pos);
        }
    }
}
