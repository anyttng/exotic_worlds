package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zurrtum.create.client.content.kinetics.belt.item.BeltConnectorHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(BeltConnectorHandler.class)
public class BeltConnectorHandlerMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ITEM_STACK_GET))
    private static Object exotic_worlds$foldStoredPulley(Object stored) {
        if (!(stored instanceof BlockPos storedPulley)) {
            return stored;
        }

        return CreateClientFrame.inViewerFrame(storedPulley);
    }
}
