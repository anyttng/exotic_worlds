package com.exoticworlds.compat.create.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zurrtum.create.client.content.equipment.toolbox.ToolboxHandlerClient;
import com.exoticworlds.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(ToolboxHandlerClient.class)
public class ToolboxHandlerClientMixin {
    @Unique
    private static final String COMPOUND_TAG_READ =
            "Lnet/minecraft/nbt/CompoundTag;read(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Ljava/util/Optional;";

    @ModifyExpressionValue(method = "onKeyInput", at = @At(value = "INVOKE", target = COMPOUND_TAG_READ))
    private static Optional<BlockPos> exotic_worlds$foldMenuToolbox(Optional<BlockPos> canonical) {
        return canonical.map(CreateClientFrame::inViewerFrame);
    }

    @ModifyExpressionValue(method = "renderOverlay", at = @At(value = "INVOKE", target = COMPOUND_TAG_READ))
    private static Optional<BlockPos> exotic_worlds$foldHudToolbox(Optional<BlockPos> canonical) {
        return canonical.map(CreateClientFrame::inViewerFrame);
    }
}
