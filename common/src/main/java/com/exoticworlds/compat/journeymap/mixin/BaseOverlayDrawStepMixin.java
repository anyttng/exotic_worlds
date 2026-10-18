package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Rectangle2D;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.journeymap.JourneyMapFold;
import com.exoticworlds.compat.journeymap.OverlayCopies;
import com.exoticworlds.compat.journeymap.SingleCopyOverlay;

import net.minecraft.client.gui.GuiGraphics;

import journeymap.api.v2.client.display.Overlay;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.Renderer;

@Mixin(targets = "journeymap.client.render.draw.BaseOverlayDrawStep", remap = false)
public abstract class BaseOverlayDrawStepMixin implements OverlayCopies {
    @Unique
    private static final double[][] EXOTIC_WORLDS$NONE = new double[0][];

    @Unique
    private static final double[][] EXOTIC_WORLDS$BASE_ONLY = {{0.0, 0.0}};

    @Shadow(remap = false)
    @Final
    public Overlay overlay;

    @Unique
    private double[][] exotic_worlds$copies = EXOTIC_WORLDS$NONE;

    @Override
    public double[][] exotic_worlds$copies() {
        return exotic_worlds$copies;
    }

    @WrapOperation(method = "isOnScreen(DDLjourneymap/client/render/map/Renderer;D)Z",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/map/Renderer;isOnScreen(Ljava/awt/geom/Rectangle2D$Double;)Z"))
    private boolean exotic_worlds$onScreenOnAnyCopy(Renderer renderer, Rectangle2D.Double bounds, Operation<Boolean> original) {
        Rectangle2D.Double screen = renderer instanceof MapRendererAccessor canvas ? canvas.exotic_worlds$screenBounds() : null;
        if (screen == null || !JourneyMapFold.active()) {
            boolean onScreen = original.call(renderer, bounds);
            exotic_worlds$copies = onScreen ? EXOTIC_WORLDS$BASE_ONLY : EXOTIC_WORLDS$NONE;
            return onScreen;
        }

        double[][] copies = JourneyMapFold.copyOffsets(renderer.getContext(), renderer.getZoom(), bounds, screen);
        exotic_worlds$copies = ((SingleCopyOverlay) this.overlay).exotic_worlds$drawsOnce()
                ? JourneyMapFold.nearestCopyOffset(copies, bounds, screen)
                : copies;
        return exotic_worlds$copies.length > 0;
    }

    @WrapOperation(method = "drawText",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawLabels(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "[Ljava/lang/String;DDLjourneymap/client/render/draw/DrawUtil$HAlign;"
                            + "Ljourneymap/client/render/draw/DrawUtil$VAlign;Ljava/lang/Integer;FLjava/lang/Integer;FDZD)V",
                    ordinal = 0))
    private void exotic_worlds$labelOnEveryCopy(GuiGraphics graphics, String[] lines, double x, double y,
            DrawUtil.HAlign hAlign, DrawUtil.VAlign vAlign, Integer background, float backgroundOpacity, Integer color,
            float opacity, double scale, boolean shadow, double rotation, Operation<Void> original) {
        for (double[] copy : exotic_worlds$copies) {
            original.call(graphics, lines, x + copy[0], y + copy[1], hAlign, vAlign, background, backgroundOpacity,
                    color, opacity, scale, shadow, rotation);
        }
    }
}
