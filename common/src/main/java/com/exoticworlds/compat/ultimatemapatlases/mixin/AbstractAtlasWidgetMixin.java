package com.exoticworlds.compat.ultimatemapatlases.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.ultimatemapatlases.AtlasSeamLines;
import com.exoticworlds.compat.ultimatemapatlases.AtlasTileCopies;
import com.exoticworlds.compat.ultimatemapatlases.BrowsedDimension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.nanaky.ultimate_map_atlases.client.AbstractAtlasWidget;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;

@Mixin(AbstractAtlasWidget.class)
public abstract class AbstractAtlasWidgetMixin {
    @Shadow
    protected int mapBlocksSize;

    @Unique
    private @Nullable ToroidalShape exotic_worlds$copyShape;

    @Unique
    private @Nullable ToroidalShape exotic_worlds$seamShape;

    @Unique
    private int exotic_worlds$requestX;

    @Unique
    private int exotic_worlds$requestZ;

    @Inject(method = "drawAtlas", at = @At("HEAD"))
    private void exotic_worlds$resolveShape(CallbackInfo ci, @Local(argsOnly = true) Player player) {
        if ((Object) this instanceof BrowsedDimension browsed) {
            ToroidalShape shape = ClientShapes.of(browsed.exotic_worlds$browsedDimension());
            MapCopies copies = MapCopies.current();
            this.exotic_worlds$copyShape = copies == MapCopies.SINGLE ? null : shape;
            this.exotic_worlds$seamShape = copies.drawsSeamLines() ? shape : null;
        } else {
            this.exotic_worlds$copyShape = ClientShapes.of(player.level());
            this.exotic_worlds$seamShape = null;
        }
    }

    @WrapOperation(
            method = "getAndDrawMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/client/AbstractAtlasWidget;getMapWithCenter(II)Lnet/nanaky/ultimate_map_atlases/utils/MapDataHolder;"))
    private @Nullable MapDataHolder exotic_worlds$mapAtAnyCopy(AbstractAtlasWidget widget, int centerX, int centerZ,
            Operation<MapDataHolder> original) {
        this.exotic_worlds$requestX = centerX;
        this.exotic_worlds$requestZ = centerZ;
        for (AtlasTileCopies.TileCenter center : AtlasTileCopies.of(this.exotic_worlds$copyShape, centerX, centerZ,
                this.mapBlocksSize)) {
            MapDataHolder found = original.call(widget, center.x(), center.z());
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    @Inject(
            method = "drawMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;map(Lnet/minecraft/client/renderer/state/MapRenderState;)V",
                    shift = At.Shift.AFTER))
    private void exotic_worlds$drawSeamLines(CallbackInfo ci, @Local(argsOnly = true) GuiGraphicsExtractor graphics) {
        if (this.exotic_worlds$seamShape != null) {
            AtlasSeamLines.draw(graphics, AtlasSeamLines.of(this.exotic_worlds$seamShape, this.exotic_worlds$requestX,
                    this.exotic_worlds$requestZ, this.mapBlocksSize));
        }
    }
}
