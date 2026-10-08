package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = BeltConnectorHandler.class, remap = false)
public class BeltConnectorHandlerMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.ITEM_STACK_GET))
    private static Object toroidal$foldStoredPulley(Object stored) {
        if (!(stored instanceof BlockPos storedPulley)) {
            return stored;
        }

        return CreateClientFrame.inViewerFrame(storedPulley);
    }
}
