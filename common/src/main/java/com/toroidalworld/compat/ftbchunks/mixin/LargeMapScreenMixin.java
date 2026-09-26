package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Window;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.client.Minecraft;

import dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen;

@Mixin(value = LargeMapScreen.class, remap = false)
public abstract class LargeMapScreenMixin {
    @ModifyReturnValue(method = "determineMinZoom", at = @At("RETURN"))
    private int toroidal$floorZoomToWorld(int original) {
        Window window = Minecraft.getInstance().getWindow();
        return Math.max(original, FtbChunksFold.zoomFloor(window.getGuiScaledWidth(), window.getGuiScaledHeight()));
    }

    // determineMinZoom is read once into a final field, and the zoom the screen opens at never meets it.
    @ModifyReturnValue(method = "getRegionTileSize", at = @At("RETURN"))
    private int toroidal$floorTileToWorld(int original) {
        Window window = Minecraft.getInstance().getWindow();
        return Math.max(original, FtbChunksFold.tileFloor(window.getGuiScaledWidth(), window.getGuiScaledHeight()));
    }
}
