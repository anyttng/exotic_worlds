package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Rectangle2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.journeymap.JourneyMapFold;
import com.exoticworlds.compat.journeymap.OverlayCopies;

import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.Renderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(targets = "journeymap.client.render.draw.BaseOverlayDrawStep", remap = false)
public abstract class BaseOverlayDrawStepMixin implements OverlayCopies {
    @Unique
    private static final double[][] EXOTIC_WORLDS$NONE = new double[0][];

    @Unique
    private static final double[][] EXOTIC_WORLDS$BASE_ONLY = {{0.0, 0.0}};

    @Unique
    private double[][] exotic_worlds$copies = EXOTIC_WORLDS$NONE;

    @Override
    public double[][] exotic_worlds$getCopies() {
        return exotic_worlds$copies;
    }

    @WrapOperation(method = "isOnScreen(DDLjourneymap/client/render/map/Renderer;D)Z",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/map/Renderer;isOnScreen(Ljava/awt/geom/Rectangle2D$Double;)Z"))
    private boolean exotic_worlds$onScreenOnAnyCopy(Renderer renderer, Rectangle2D.Double bounds, Operation<Boolean> original) {
        Rectangle2D.Double screen = renderer instanceof MapRendererAccessor canvas ? canvas.exotic_worlds$getScreenBounds() : null;
        if (screen == null || !JourneyMapFold.isActive()) {
            boolean isOnScreen = original.call(renderer, bounds);
            exotic_worlds$copies = isOnScreen ? EXOTIC_WORLDS$BASE_ONLY : EXOTIC_WORLDS$NONE;
            return isOnScreen;
        }

        exotic_worlds$copies = JourneyMapFold.copyOffsets(renderer.getContext(), renderer.getZoom(), bounds, screen);
        return exotic_worlds$copies.length > 0;
    }

    @WrapOperation(method = "drawText",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "[Ljava/lang/String;DDLjourneymap/client/render/draw/DrawUtil$HAlign;"
                            + "Ljourneymap/client/render/draw/DrawUtil$VAlign;Ljava/lang/Integer;FLjava/lang/Integer;FDZD)V",
                    ordinal = 0))
    private void exotic_worlds$labelOnEveryCopy(GuiGraphicsExtractor graphics, String[] lines, double x, double y,
            DrawUtil.HAlign hAlign, DrawUtil.VAlign vAlign, Integer background, float backgroundOpacity, Integer color,
            float opacity, double scale, boolean hasShadow, double rotation, Operation<Void> original) {
        for (double[] copy : exotic_worlds$copies) {
            original.call(graphics, lines, x + copy[0], y + copy[1], hAlign, vAlign, background, backgroundOpacity,
                    color, opacity, scale, hasShadow, rotation);
        }
    }
}
