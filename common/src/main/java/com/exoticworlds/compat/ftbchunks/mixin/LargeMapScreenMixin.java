package com.exoticworlds.compat.ftbchunks.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Window;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;

import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import net.minecraft.client.Minecraft;

@Mixin(value = LargeMapScreen.class, remap = false)
public abstract class LargeMapScreenMixin {
    @ModifyExpressionValue(method = {"addZoom", "drawForeground"},
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Ldev/ftb/mods/ftbchunks/client/gui/LargeMapScreen;minZoom:I"))
    private int toroidal$floorZoomToWorld(int original) {
        Window window = Minecraft.getInstance().getWindow();
        return Math.max(original, FtbChunksFold.zoomFloor(((LargeMapScreen) (Object) this).currentDimension(),
                window.getGuiScaledWidth(), window.getGuiScaledHeight()));
    }

    // The zoom the screen opens at, or keeps across a dimension switch, never meets the floor.
    @ModifyReturnValue(method = "getRegionTileSize", at = @At("RETURN"))
    private int toroidal$floorTileToWorld(int original) {
        Window window = Minecraft.getInstance().getWindow();
        return Math.max(original, FtbChunksFold.tileFloor(((LargeMapScreen) (Object) this).currentDimension(),
                window.getGuiScaledWidth(), window.getGuiScaledHeight()));
    }
}
