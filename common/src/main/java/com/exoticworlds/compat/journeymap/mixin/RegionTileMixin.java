package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.common.Context.UI;
import journeymap.client.model.map.MapType;
import journeymap.client.model.region.RegionCoord;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    @Unique
    private static boolean exotic_worlds$drawingCopies;

    @WrapOperation(method = "setPosition",
            at = @At(value = "INVOKE", target = "Ljava/awt/geom/Rectangle2D$Double;contains(DD)Z"))
    private boolean exotic_worlds$showTilesWhoseCopyMeetsTheGrid(Rectangle2D.Double regionBounds, double regionX,
            double regionZ, Operation<Boolean> original) {
        return original.call(regionBounds, regionX, regionZ)
                || JourneyMapFold.active() && JourneyMapFold.regionInView(regionBounds, (int) regionX, (int) regionZ);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void exotic_worlds$renderWrappedCopies(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha, MapType mapType, RenderPipeline pipeline,
            CallbackInfo ci) {
        if (exotic_worlds$drawingCopies || context == UI.Webmap) {
            return;
        }

        List<WorldCopies.Copy> copies = exotic_worlds$copiesToDraw(context);
        if (copies == null) {
            return;
        }

        double pixelsPerBlock = JourneyMapFold.pixelsPerBlock(this.zoom);
        exotic_worlds$drawingCopies = true;
        try {
            for (WorldCopies.Copy copy : copies) {
                this.render(graphics, pose, context, pixelOffsetX + copy.dx() * pixelsPerBlock,
                        pixelOffsetZ + copy.dz() * pixelsPerBlock, alpha, mapType, pipeline);
            }
        } finally {
            exotic_worlds$drawingCopies = false;
        }
    }

    @Unique
    private @Nullable List<WorldCopies.Copy> exotic_worlds$copiesToDraw(UI context) {
        JourneyMapFold.View view = JourneyMapFold.viewOf(context);
        if (JourneyMapFold.loopedAxes() == 0 || view == null) {
            return null;
        }

        Window window = Minecraft.getInstance().getWindow();
        int viewportX = exotic_worlds$viewportPixels(context, window.getWidth());
        int viewportZ = exotic_worlds$viewportPixels(context, window.getHeight());
        int[] spanX = JourneyMapFold.viewSpan(view.centerX(), viewportX, this.zoom);
        int[] spanZ = JourneyMapFold.viewSpan(view.centerZ(), viewportZ, this.zoom);
        List<WorldCopies.Copy> drawn = JourneyMapFold.drawnCopies(view.tiles(), spanX, spanZ,
                JourneyMapFold.copiesOf(context));
        JourneyMapFold.recordCopies(context, drawn);
        RegionCoord region = this.getRegionCoord();
        return JourneyMapFold.tileCopies(drawn,
                region.regionX * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS,
                region.regionZ * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS,
                FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS, spanX, spanZ);
    }

    @Unique
    private static int exotic_worlds$viewportPixels(UI context, int windowPixels) {
        if (context != UI.Minimap) {
            return windowPixels;
        }

        DisplayVars displayVars = UIManager.INSTANCE.getMiniMap().getDisplayVars();
        return displayVars == null
                ? windowPixels
                : (int) Math.ceil(Math.hypot(displayVars.getMinimapWidth(), displayVars.getMinimapHeight()));
    }
}
