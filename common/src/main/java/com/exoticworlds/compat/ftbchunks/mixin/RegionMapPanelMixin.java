package com.exoticworlds.compat.ftbchunks.mixin;

import java.util.List;

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
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.SeamLine;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold.SeamView;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold.TileBlit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.map.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.gui.map.RegionMapPanel;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.icon.Color4I;

@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class RegionMapPanelMixin {
    @Unique
    private static final int SEAM_LINE_WIDTH = 1;

    @Shadow
    @Final
    LargeMapScreen largeMapScreen;

    @Shadow
    int regionMinX;

    @Shadow
    int regionMinZ;

    @Shadow
    int blockX;

    @Unique
    private @Nullable SeamView toroidal$seamView;

    @Inject(method = "draw", at = @At("HEAD"))
    private void toroidal$stopScrollAtTheEdge(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h,
            CallbackInfo ci) {
        if (MapCopies.current() != MapCopies.SINGLE) {
            return;
        }

        RegionMapPanel panel = (RegionMapPanel) (Object) this;
        ResourceKey<Level> dimension = this.largeMapScreen.currentDimension();
        int tilePixels = this.largeMapScreen.getRegionTileSize();
        panel.setScrollX(FtbChunksFold.clampScroll(dimension, Direction.Axis.X, panel.getScrollX(), this.regionMinX,
                tilePixels, panel.width));
        panel.setScrollY(FtbChunksFold.clampScroll(dimension, Direction.Axis.Z, panel.getScrollY(), this.regionMinZ,
                tilePixels, panel.height));
    }

    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/client/gui/widget/Panel;draw("
                            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Ldev/ftb/mods/ftblibrary/client/gui/theme/Theme;IIII)V"))
    private void toroidal$drawWrappedCopies(RegionMapPanel panel, GuiGraphicsExtractor graphics, Theme theme, int x,
            int y, int w, int h, Operation<Void> original) {
        ResourceKey<Level> dimension = this.largeMapScreen.currentDimension();
        int loopedAxes = FtbChunksFold.loopedAxes(dimension);
        if (loopedAxes == 0) {
            toroidal$seamView = null;
            original.call(panel, graphics, theme, x, y, w, h);
            return;
        }

        int tilePixels = this.largeMapScreen.getRegionTileSize();
        panel.setOffset(true);
        double pixelsPerBlock = tilePixels / (double) FtbChunksFold.REGION_BLOCKS;
        int originX = panel.getX() - this.regionMinX * tilePixels;
        int originY = panel.getY() - this.regionMinZ * tilePixels;
        MapCopies mapCopies = MapCopies.current();
        List<WorldCopies.Copy> copies = FtbChunksFold.drawnCopies(dimension,
                Mth.floor((x - originX) / pixelsPerBlock), Mth.floor((y - originY) / pixelsPerBlock),
                Mth.ceil((x + w - originX) / pixelsPerBlock), Mth.ceil((y + h - originY) / pixelsPerBlock), mapCopies);

        // Copies draw before the canonical pass, which draws the icons, so an icon on the seam stays above them.
        boolean scissor = panel.getOnlyRenderWidgetsInside();
        if (scissor) {
            graphics.enableScissor(x, y, x + w, y + h);
        }

        for (WorldCopies.Copy copy : copies) {
            if (copy.isIdentity()) {
                continue;
            }

            toroidal$blitCopy(panel, graphics, x, y, w, h,
                    (int) Math.round(copy.dx() * pixelsPerBlock), (int) Math.round(copy.dz() * pixelsPerBlock));
        }

        if (scissor) {
            graphics.disableScissor();
        }

        panel.setOffset(false);

        toroidal$seamView = !mapCopies.drawsSeamLines()
                ? null
                : new SeamView(originX, originY, tilePixels, x, y, w, h);
        original.call(panel, graphics, theme, x, y, w, h);
    }

    // Not @Unique: it overrides Panel.drawOffsetBackground, drawn between the BACKGROUND tiles and FOREGROUND icons.
    public void drawOffsetBackground(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
        SeamView view = toroidal$seamView;
        if (view == null) {
            return;
        }

        for (SeamLine line : FtbChunksFold.seamLines(this.largeMapScreen.currentDimension(), view)) {
            line.fill(graphics, SEAM_LINE_WIDTH);
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

            TileBlit blit = FtbChunksFold.worldPartOf(tile.region.dimension.dimension, tile.region.pos.x(),
                    tile.region.pos.z(),
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
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I", ordinal = 1))
    private int toroidal$foldPick(int blockZ) {
        BlockPos folded = FtbChunksFold.foldBlock(this.largeMapScreen.currentDimension(), this.blockX, blockZ);
        this.blockX = folded.getX();
        return folded.getZ();
    }
}
