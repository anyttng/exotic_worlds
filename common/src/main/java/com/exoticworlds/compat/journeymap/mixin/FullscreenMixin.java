package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Point2D;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Window;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import journeymap.client.render.map.MapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

@Mixin(targets = "journeymap.client.ui.fullscreen.Fullscreen", remap = false)
public abstract class FullscreenMixin {
    @Shadow(remap = false)
    @Final
    private static MapRenderer mapRenderer;

    @ModifyVariable(method = "setZoom(I)V", at = @At("HEAD"), argsOnly = true)
    private int toroidal$keepSingleZoomOnTheLevels(int zoom) {
        if (MapCopies.current() != MapCopies.SINGLE) {
            return zoom;
        }

        return Math.max(Integer.highestOneBit(zoom), JourneyMapFold.fullscreenZoomFloor());
    }

    @ModifyReturnValue(method = "getMouseDrag", at = @At("RETURN"))
    private Point2D.Double toroidal$stopDragAtTheEdge(Point2D.Double drag) {
        if (MapCopies.current() != MapCopies.SINGLE || !JourneyMapFold.active()) {
            return drag;
        }

        Window window = Minecraft.getInstance().getWindow();
        int zoom = mapRenderer.getZoom();
        return new Point2D.Double(
                -JourneyMapFold.clampedMove(Direction.Axis.X, mapRenderer.getCenterBlockX(), -drag.getX(), zoom,
                        window.getWidth()),
                -JourneyMapFold.clampedMove(Direction.Axis.Z, mapRenderer.getCenterBlockZ(), -drag.getY(), zoom,
                        window.getHeight()));
    }
}
