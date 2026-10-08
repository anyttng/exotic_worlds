package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Rectangle2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.common.Context.UI;
import journeymap.client.model.map.MapType;
import journeymap.client.model.region.RegionCoord;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import org.joml.Matrix3x2fStack;

@Mixin(targets = "journeymap.client.render.map.RegionTile", remap = false)
public abstract class RegionTileMixin {
    @Shadow(remap = false)
    public abstract void render(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha, MapType mapType, RenderPipeline pipeline);

    @Shadow(remap = false)
    public abstract RegionCoord getRegionCoord();

    @Shadow(remap = false)
    private int zoom;

    // Render-thread only, like the render call itself.
    @Unique
    private static boolean toroidal$drawingCopies;

    @WrapOperation(method = "setPosition",
            at = @At(value = "INVOKE", target = "Ljava/awt/geom/Rectangle2D$Double;contains(DD)Z"))
    private boolean toroidal$showTilesWhoseCopyMeetsTheGrid(Rectangle2D.Double regionBounds, double regionX,
            double regionZ, Operation<Boolean> original) {
        return original.call(regionBounds, regionX, regionZ)
                || JourneyMapFold.active() && JourneyMapFold.regionInView(regionBounds, (int) regionX, (int) regionZ);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void toroidal$renderWrappedCopies(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha, MapType mapType, RenderPipeline pipeline,
            CallbackInfo ci) {
        if (toroidal$drawingCopies || context == UI.Webmap) {
            return;
        }

        JourneyMapFold.View view = JourneyMapFold.viewOf(context);
        if (JourneyMapFold.loopedAxes() == 0 || view == null) {
            return;
        }

        AxisCopies copiesX = JourneyMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = JourneyMapFold.copies(Direction.Axis.Z);
        Window window = Minecraft.getInstance().getWindow();
        int viewportX = toroidal$viewportPixels(context, window.getWidth());
        int viewportZ = toroidal$viewportPixels(context, window.getHeight());
        int[] spanX = JourneyMapFold.viewSpan(view.centerX(), viewportX, this.zoom);
        int[] spanZ = JourneyMapFold.viewSpan(view.centerZ(), viewportZ, this.zoom);
        int[] ranges = JourneyMapFold.copyRanges(copiesX, copiesZ, view.tiles(), spanX, spanZ,
                JourneyMapFold.copiesOf(context));
        JourneyMapFold.recordCopyRange(context, ranges[0], ranges[1]);
        if (ranges[0] == 0 && ranges[1] == 0) {
            return;
        }

        RegionCoord region = this.getRegionCoord();
        int[] lapsX = JourneyMapFold.tileLaps(copiesX, region.regionX * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS,
                FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS, spanX[0], spanX[1], ranges[0]);
        int[] lapsZ = JourneyMapFold.tileLaps(copiesZ, region.regionZ * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS,
                FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS, spanZ[0], spanZ[1], ranges[1]);
        double periodX = JourneyMapFold.worldPixelPeriod(Direction.Axis.X, this.zoom);
        double periodZ = JourneyMapFold.worldPixelPeriod(Direction.Axis.Z, this.zoom);
        toroidal$drawingCopies = true;
        try {
            for (int lapX : lapsX) {
                for (int lapZ : lapsZ) {
                    if (lapX == 0 && lapZ == 0) {
                        continue;
                    }

                    this.render(graphics, pose, context,
                            pixelOffsetX + lapX * periodX, pixelOffsetZ + lapZ * periodZ, alpha, mapType, pipeline);
                }
            }
        } finally {
            toroidal$drawingCopies = false;
        }
    }

    @Unique
    private static int toroidal$viewportPixels(UI context, int windowPixels) {
        if (context != UI.Minimap) {
            return windowPixels;
        }

        DisplayVars displayVars = UIManager.INSTANCE.getMiniMap().getDisplayVars();
        return displayVars == null
                ? windowPixels
                : (int) Math.ceil(Math.hypot(displayVars.getMinimapWidth(), displayVars.getMinimapHeight()));
    }
}
