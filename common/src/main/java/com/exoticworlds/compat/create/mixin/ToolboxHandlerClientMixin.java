package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.exoticworlds.compat.create.CatnipInjectionTargets;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = ToolboxHandlerClient.class, remap = false)
public class ToolboxHandlerClientMixin {
    @ModifyExpressionValue(method = "onKeyInput",
            at = @At(value = "INVOKE", target = CatnipInjectionTargets.NBT_HELPER_READ_BLOCK_POS))
    private static BlockPos toroidal$foldMenuToolbox(BlockPos canonical) {
        return CreateClientFrame.inViewerFrame(canonical);
    }

    @ModifyExpressionValue(method = "renderOverlay",
            at = @At(value = "INVOKE", target = CatnipInjectionTargets.NBT_HELPER_READ_BLOCK_POS))
    private static BlockPos toroidal$foldHudToolbox(BlockPos canonical) {
        return CreateClientFrame.inViewerFrame(canonical);
    }
}
