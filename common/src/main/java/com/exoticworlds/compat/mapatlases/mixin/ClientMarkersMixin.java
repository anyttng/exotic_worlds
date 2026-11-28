package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.map.ClientFolds;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;
import pepjebs.mapatlases.utils.AtlasMap;

@Mixin(ClientMarkers.class)
public class ClientMarkersMixin {
    @ModifyVariable(method = "placePin", at = @At("HEAD"), argsOnly = true)
    private static ColumnPos exotic_worlds$foldedPin(ColumnPos pos, @Local(argsOnly = true) AtlasMap holder) {
        BlockPos folded = ClientFolds.fold(ClientFolds.of(holder.data.dimension), new BlockPos(pos.x(), 0, pos.z()));
        return new ColumnPos(folded.getX(), folded.getZ());
    }
}
