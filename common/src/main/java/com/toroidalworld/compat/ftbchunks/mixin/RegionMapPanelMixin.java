package com.toroidalworld.compat.ftbchunks.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.MapCopies;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold.SeamView;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold.TileBlit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.map.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.gui.map.RegionMapPanel;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.icon.Color4I;

@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class RegionMapPanelMixin {
    private static final int SEAM_LINE_ARGB = 0xCCFFFFFF;
    private static final int SEAM_LINE_WIDTH = 1;

    @Shadow
    @Final
    LargeMapScreen largeMapScreen;

    @Shadow
    int regionMinX;

    @Shadow
    int regionMinZ;

    @Unique
    private @Nullable SeamView toroidal$seamView;

    @Inject(method = "draw", at = @At("HEAD"))
    private void toroidal$stopScrollAtTheEdge(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h,
            CallbackInfo ci) {
        if (MapCopies.current() != MapCopies.SINGLE) {
            return;
        }

        RegionMapPanel panel = (RegionMapPanel) (Object) this;
        int tilePixels = this.largeMapScreen.getRegionTileSize();
        panel.setScrollX(FtbChunksFold.clampScroll(Direction.Axis.X, panel.getScrollX(), this.regionMinX, tilePixels,
                panel.width));
        panel.setScrollY(FtbChunksFold.clampScroll(Direction.Axis.Z, panel.getScrollY(), this.regionMinZ, tilePixels,
                panel.height));
    }

    // A copy is the same tile blitted again at a lap's offset — never a second pass through Panel.draw, which drags
    // the icons along with it.
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/client/gui/widget/Panel;draw("
                            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Ldev/ftb/mods/ftblibrary/client/gui/theme/Theme;IIII)V"))
    private void toroidal$drawWrappedCopies(RegionMapPanel panel, GuiGraphicsExtractor graphics, Theme theme, int x,
            int y, int w, int h, Operation<Void> original) {
        int loopedAxes = FtbChunksFold.loopedAxes();
        if (loopedAxes == 0) {
            toroidal$seamView = null;
            original.call(panel, graphics, theme, x, y, w, h);
            return;
        }

        int tilePixels = this.largeMapScreen.getRegionTileSize();
        double periodX = FtbChunksFold.worldPixelPeriod(Direction.Axis.X, tilePixels);
        double periodZ = FtbChunksFold.worldPixelPeriod(Direction.Axis.Z, tilePixels);

        // The laps are the ones whose copy falls inside the view in block terms — the canonical square can sit at
        // the edge of the scrolled canvas or past it, so a fixed reach around it leaves the far side bare.
        panel.setOffset(true);
        double pixelsPerBlock = tilePixels / (double) FtbChunksFold.REGION_BLOCKS;
        int originX = panel.getX() - this.regionMinX * tilePixels;
        int originY = panel.getY() - this.regionMinZ * tilePixels;
        MapCopies mapCopies = MapCopies.current();
        int[] lapsX = FtbChunksFold.copies(Direction.Axis.X).drawnLaps(
                Mth.floor((x - originX) / pixelsPerBlock), Mth.ceil((x + w - originX) / pixelsPerBlock), mapCopies);
        int[] lapsZ = FtbChunksFold.copies(Direction.Axis.Z).drawnLaps(
                Mth.floor((y - originY) / pixelsPerBlock), Mth.ceil((y + h - originY) / pixelsPerBlock), mapCopies);

        // Copies go under the canonical pass: that pass draws the icons, and an icon straddling the seam has to
        // stay on top of the copy beside it.
        boolean scissor = panel.getOnlyRenderWidgetsInside();
        if (scissor) {
            graphics.enableScissor(x, y, x + w, y + h);
        }

        for (int lapX : lapsX) {
            for (int lapZ : lapsZ) {
                if (lapX == 0 && lapZ == 0) {
                    continue;
                }

                toroidal$blitCopy(panel, graphics, x, y, w, h,
                        (int) Math.round(lapX * periodX), (int) Math.round(lapZ * periodZ));
            }
        }

        if (scissor) {
            graphics.disableScissor();
        }

        panel.setOffset(false);

        toroidal$seamView = mapCopies == MapCopies.SINGLE
                ? null
                : new SeamView(originX, originY, tilePixels, x, y, w, h);
        original.call(panel, graphics, theme, x, y, w, h);
    }

    // Panel.draw calls this between its BACKGROUND and FOREGROUND layers, under the panel's scissor: the tiles draw
    // on BACKGROUND, so the lines land over the terrain and under every icon.
    public void drawOffsetBackground(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
        SeamView view = toroidal$seamView;
        if (view == null) {
            return;
        }

        for (int seamX : FtbChunksFold.seamPixels(Direction.Axis.X, view.originX(), view.tilePixels(), view.x(),
                view.x() + view.width())) {
            graphics.fill(seamX, view.y(), seamX + SEAM_LINE_WIDTH, view.y() + view.height(), SEAM_LINE_ARGB);
        }

        for (int seamZ : FtbChunksFold.seamPixels(Direction.Axis.Z, view.originY(), view.tilePixels(), view.y(),
                view.y() + view.height())) {
            graphics.fill(view.x(), seamZ, view.x() + view.width(), seamZ + SEAM_LINE_WIDTH, SEAM_LINE_ARGB);
        }
    }

    @Unique
    private static void toroidal$blitCopy(Panel panel, GuiGraphicsExtractor graphics, int viewX, int viewY,
            int viewWidth, int viewHeight, int offsetX, int offsetY) {
        for (Widget widget : panel.getWidgets()) {
            if (!(widget instanceof MapTileWidget tile)) {
                continue;
            }

            Identifier texture = tile.region.regionTexture().getTextureID();
            if (texture == null) {
                continue;
            }

            TileBlit blit = FtbChunksFold.worldPartOf(tile.region.pos.x(), tile.region.pos.z(),
                    tile.getX() + offsetX, tile.getY() + offsetY, tile.width, tile.height);
            if (blit == null || blit.x() + blit.width() <= viewX || blit.x() >= viewX + viewWidth
                    || blit.y() + blit.height() <= viewY || blit.y() >= viewY + viewHeight) {
                continue;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, blit.x(), blit.y(), blit.u(), blit.v(),
                    blit.width(), blit.height(), blit.srcWidth(), blit.srcHeight(), FtbChunksFold.REGION_BLOCKS,
                    FtbChunksFold.REGION_BLOCKS, Color4I.WHITE.rgba());
        }
    }

    @ModifyExpressionValue(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I", ordinal = 0))
    private int toroidal$foldPickX(int blockX) {
        return FtbChunksFold.foldBlock(Direction.Axis.X, blockX);
    }

    @ModifyExpressionValue(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I", ordinal = 1))
    private int toroidal$foldPickZ(int blockZ) {
        return FtbChunksFold.foldBlock(Direction.Axis.Z, blockZ);
    }
}
