package com.exoticworlds.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.exoticworlds.compat.create.client.CreateMenuFrame;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;

@Mixin(targets = "net.minecraft.core.component.DataComponentPatch$1")
public class DataComponentPatchCodecMixin {
    @WrapMethod(method = "decode(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lnet/minecraft/core/component/DataComponentPatch;")
    private DataComponentPatch exotic_worlds$decodeOutsideTheMenuFrame(RegistryFriendlyByteBuf buffer,
            Operation<DataComponentPatch> original) {
        CreateMenuFrame.Frame suspended = CreateMenuFrame.suspend();
        try {
            return original.call(buffer);
        } finally {
            CreateMenuFrame.resume(suspended);
        }
    }
}
