package com.exoticworlds.compat.mapatlases.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.map.ClientFolds;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.MapWidget;

@Mixin(MapWidget.class)
public class MapWidgetMixin {
    @Shadow
    @Final
    private AtlasOverviewScreen mapScreen;

    @ModifyExpressionValue(
            method = "renderPositionText",
            at = @At(value = "INVOKE", target = "Lpepjebs/mapatlases/client/screen/MapWidget;getHoveredPos(DD)"
                    + "Lnet/minecraft/server/level/ColumnPos;"))
    private ColumnPos exotic_worlds$foldedPositionText(ColumnPos hovered) {
        return this.exotic_worlds$folded(hovered);
    }

    @ModifyExpressionValue(
            method = "mouseClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ColumnPos;x()I"))
    private int exotic_worlds$foldedTeleportX(int x, @Local ColumnPos hovered) {
        return this.exotic_worlds$folded(hovered).x();
    }

    @ModifyExpressionValue(
            method = "mouseClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ColumnPos;z()I"))
    private int exotic_worlds$foldedTeleportZ(int z, @Local ColumnPos hovered) {
        return this.exotic_worlds$folded(hovered).z();
    }

    @Unique
    private ColumnPos exotic_worlds$folded(ColumnPos hovered) {
        BlockPos folded = ClientFolds.fold(ClientFolds.of(this.mapScreen.getSelectedSlice().dimension()),
                new BlockPos(hovered.x(), 0, hovered.z()));
        return new ColumnPos(folded.getX(), folded.getZ());
    }
}
