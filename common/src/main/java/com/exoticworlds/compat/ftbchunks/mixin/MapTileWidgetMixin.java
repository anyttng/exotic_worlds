package com.exoticworlds.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold;
import com.exoticworlds.compat.ftbchunks.FtbChunksFold.TileBlit;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import dev.ftb.mods.ftbchunks.client.gui.map.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;

@Mixin(value = MapTileWidget.class, remap = false)
public abstract class MapTileWidgetMixin {
    @Shadow
    @Final
    public MapRegion region;

    // The tile paints only its part inside the world: the copies already lie beside it, and its black margin would
    // paint over them.
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit("
                            + "Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;"
                            + "IIFFIIIIIII)V"))
    private void toroidal$blitWorldPartOnly(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture,
            int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth,
            int textureHeight, int color, Operation<Void> original) {
        TileBlit blit = FtbChunksFold.worldPartOf(this.region.pos.x(), this.region.pos.z(), x, y, width, height);
        if (blit == null) {
            return;
        }

        original.call(graphics, pipeline, texture, blit.x(), blit.y(), (float) blit.u(), (float) blit.v(),
                blit.width(), blit.height(), blit.srcWidth(), blit.srcHeight(), FtbChunksFold.REGION_BLOCKS,
                FtbChunksFold.REGION_BLOCKS, color);
    }
}
