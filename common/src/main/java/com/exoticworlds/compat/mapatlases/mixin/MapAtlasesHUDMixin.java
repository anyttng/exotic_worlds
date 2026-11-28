package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.map.ClientFolds;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;

@Mixin(MapAtlasesHUD.class)
public class MapAtlasesHUDMixin {
    @ModifyExpressionValue(method = "renderText", at = @At(value = "NEW", target = "net/minecraft/core/BlockPos"))
    private BlockPos exotic_worlds$foldedReadout(BlockPos position) {
        return ClientFolds.fold(ClientFolds.getCurrent(), position);
    }
}
