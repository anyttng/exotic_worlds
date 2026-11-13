package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.redstone.displayLink.ClickToLinkHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(ClickToLinkHandler.class)
public class ClickToLinkHandlerMixin {
    @ModifyExpressionValue(method = "clientTick",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/infrastructure/component/ClickToLinkData;"
                            + "selectedPos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos exotic_worlds$foldOutlineToClientFrame(BlockPos selected) {
        return CreateClientFrame.inViewerFrame(selected);
    }

    @Inject(method = "clientTick", cancellable = true,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_EQUALS))
    private static void exotic_worlds$skipOutlineWhileCopyUnheld(Minecraft minecraft, CallbackInfo callback,
            @Local BlockPos selected) {
        if (CreateClientFrame.heldInViewerFrame(selected) == null) {
            callback.cancel();
        }
    }
}
