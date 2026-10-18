package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import net.minecraft.world.phys.Vec3;

import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.MapIconWidget;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;

@Mixin(value = MapIconWidget.class, remap = false)
public abstract class MapIconWidgetMixin {
    @Shadow
    @Final
    private RegionMapPanel regionMapPanel;

    @ModifyExpressionValue(method = "updatePosition",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbchunks/api/client/icon/MapIcon;getPos(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 exotic_worlds$foldIconPosition(Vec3 position) {
        return FtbChunksFold.foldPosition(((LargeMapScreen) this.regionMapPanel.getGui()).currentDimension(), position);
    }
}
