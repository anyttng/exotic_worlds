package com.exoticworlds.compat.ftbchunks.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
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
import com.mojang.blaze3d.systems.RenderSystem;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold.SeamLine;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold.SeamView;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold.TileBlit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;

@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class RegionMapPanelMixin {
    // MapTileWidget.draw switches to linear filtering once a tile is narrower than 512 window pixels.
    @Unique
    private static final double LINEAR_FILTER_BELOW_PIXELS = 512.0;
    private static final int SEAM_LINE_ARGB = 0xCCFFFFFF;
    private static final int SEAM_LINE_WIDTH = 1;

    @Shadow
    @Final
    LargeMapScreen largeMap;

    @Shadow
    int regionMinX;

    @Shadow
    int regionMinZ;

    @Shadow
    int blockX;

    @Unique
    private @Nullable SeamView toroidal$seamView;

    @Inject(method = "draw", at = @At("HEAD"))
    private void toroidal$stopScrollAtTheEdge(GuiGraphics graphics, Theme theme, int x, int y, int w, int h,
            CallbackInfo ci) {
        if (MapCopies.current() != MapCopies.SINGLE) {
            return;
        }

        RegionMapPanel panel = (RegionMapPanel) (Object) this;
        ResourceKey<Level> dimension = this.largeMap.currentDimension();
        int tilePixels = this.largeMap.getRegionTileSize();
        panel.setScrollX(FtbChunksFold.clampScroll(dimension, Direction.Axis.X, panel.getScrollX(), this.regionMinX,
                tilePixels, panel.width));
        panel.setScrollY(FtbChunksFold.clampScroll(dimension, Direction.Axis.Z, panel.getScrollY(), this.regionMinZ,
                tilePixels, panel.height));
    }

    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/Panel;draw(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Ldev/ftb/mods/ftblibrary/ui/Theme;IIII)V"))
    private void toroidal$drawWrappedCopies(RegionMapPanel panel, GuiGraphics graphics, Theme theme, int x, int y,
            int w, int h, Operation<Void> original) {
        ResourceKey<Level> dimension = this.largeMap.currentDimension();
        int loopedAxes = FtbChunksFold.loopedAxes(dimension);
        if (loopedAxes == 0) {
            toroidal$seamView = null;
            original.call(panel, graphics, theme, x, y, w, h);
            return;
        }

        int tilePixels = this.largeMap.getRegionTileSize();
        panel.setOffset(true);
        double pixelsPerBlock = tilePixels / (double) FtbChunksFold.REGION_BLOCKS;
        int originX = panel.getX() - this.regionMinX * tilePixels;
        int originY = panel.getY() - this.regionMinZ * tilePixels;
        MapCopies mapCopies = MapCopies.current();
        List<WorldCopies.Copy> copies = FtbChunksFold.drawnCopies(dimension,
                Mth.floor((x - originX) / pixelsPerBlock), Mth.floor((y - originY) / pixelsPerBlock),
                Mth.ceil((x + w - originX) / pixelsPerBlock), Mth.ceil((y + h - originY) / pixelsPerBlock), mapCopies);

        // Copies go under the canonical pass: that pass draws the icons, and an icon straddling the seam has to
        // stay on top of the copy beside it.
        boolean scissor = panel.getOnlyRenderWidgetsInside();
        if (scissor) {
            GuiHelper.pushScissor(panel.getWindow(), x, y, w, h);
        }

        for (WorldCopies.Copy copy : copies) {
            if (copy.isIdentity()) {
                continue;
            }

            toroidal$blitCopy(panel, graphics, x, y, w, h,
                    (int) Math.round(copy.dx() * pixelsPerBlock), (int) Math.round(copy.dz() * pixelsPerBlock));
        }

        if (scissor) {
            GuiHelper.popScissor(panel.getWindow());
        }

        panel.setOffset(false);
        FtbChunksFold.recordLargeMapCopies(copies);

        toroidal$seamView = !mapCopies.drawsSeamLines()
                ? null
                : new SeamView(originX, originY, tilePixels, x, y, w, h);
        original.call(panel, graphics, theme, x, y, w, h);
    }

    // Panel.draw calls this between its BACKGROUND and FOREGROUND layers, under the panel's scissor: the tiles are
    // moved to BACKGROUND (MapTileWidgetMixin), so the lines land over the terrain and under every icon.
    public void drawOffsetBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        SeamView view = toroidal$seamView;
        if (view == null) {
            return;
        }

        for (SeamLine line : FtbChunksFold.seamLines(this.largeMap.currentDimension(), view)) {
            if (line.vertical()) {
                graphics.fill(line.at(), line.from(), line.at() + SEAM_LINE_WIDTH, line.to(), SEAM_LINE_ARGB);
            } else {
                graphics.fill(line.from(), line.at(), line.to(), line.at() + SEAM_LINE_WIDTH, SEAM_LINE_ARGB);
            }
        }
    }

    @Unique
    private static void toroidal$blitCopy(RegionMapPanel panel, GuiGraphics graphics, int viewX, int viewY,
            int viewWidth, int viewHeight, int offsetX, int offsetY) {
        for (Widget widget : panel.getWidgets()) {
            if (!(widget instanceof MapTileWidget tile)) {
                continue;
            }

            // The id call uploads a pending image and sets the loaded flag, so it precedes the check, as in MapTileWidget.draw.
            int id = tile.region.getRenderedMapImageTextureId();
            if (!tile.region.isMapImageLoaded()) {
                continue;
            }

            TileBlit blit = FtbChunksFold.worldPartOf(tile.region.dimension.dimension, tile.region.pos.x(),
                    tile.region.pos.z(),
                    tile.getX() + offsetX, tile.getY() + offsetY, tile.width, tile.height);
            if (blit == null || blit.x() + blit.width() <= viewX || blit.x() >= viewX + viewWidth
                    || blit.y() + blit.height() <= viewY || blit.y() >= viewY + viewHeight) {
                continue;
            }

            RenderSystem.bindTextureForSetup(id);
            int filter = tile.width * Minecraft.getInstance().getWindow().getGuiScale() < LINEAR_FILTER_BELOW_PIXELS
                    ? GL11.GL_LINEAR
                    : GL11.GL_NEAREST;
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
            RenderSystem.setShaderTexture(0, id);
            GuiHelper.drawTexturedRect(graphics, blit.x(), blit.y(), blit.width(), blit.height(), Color4I.WHITE,
                    blit.u0(), blit.v0(), blit.u1(), blit.v1());
        }
    }

    @ModifyExpressionValue(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I", ordinal = 1))
    private int toroidal$foldPick(int blockZ) {
        BlockPos folded = FtbChunksFold.foldBlock(this.largeMap.currentDimension(), this.blockX, blockZ);
        this.blockX = folded.getX();
        return folded.getZ();
    }
}
