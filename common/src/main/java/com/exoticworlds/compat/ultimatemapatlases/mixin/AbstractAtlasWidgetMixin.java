package com.exoticworlds.compat.ultimatemapatlases.mixin;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3x2fStack;
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
import com.exoticworlds.compat.ultimatemapatlases.AtlasPointSeat;
import com.exoticworlds.compat.ultimatemapatlases.AtlasSeamLines;
import com.exoticworlds.compat.ultimatemapatlases.AtlasTileCopies;
import com.exoticworlds.compat.ultimatemapatlases.BrowsedDimension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.nanaky.ultimate_map_atlases.client.AbstractAtlasWidget;
import net.nanaky.ultimate_map_atlases.utils.MapDataHolder;
import net.nanaky.ultimate_map_atlases.utils.PriorityMarkerInfo;
import net.nanaky.ultimate_map_atlases.utils.Slice;

@Mixin(AbstractAtlasWidget.class)
public abstract class AbstractAtlasWidgetMixin {
    @Shadow
    protected int mapBlocksSize;

    @Shadow
    protected double currentXCenter;

    @Shadow
    protected double currentZCenter;

    @Shadow
    protected boolean drawBigPlayerMarker;

    @Shadow
    private void drawMap(Player player, GuiGraphicsExtractor graphics, List<Pair<Integer, Integer>> normalBorders,
            List<Pair<Integer, Integer>> selectedBorders, int ix, int iy, MapDataHolder state, boolean drawPlayerIcons,
            int light, @Nullable MapItemSavedData selectedData, List<Runnable> pendingDecorations) {
    }

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

    @Inject(
            method = "drawAtlas",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;",
                    ordinal = 1, shift = At.Shift.AFTER))
    private void exotic_worlds$drawShiftedCopies(CallbackInfo ci, @Local(argsOnly = true) GuiGraphicsExtractor graphics,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) float zoomLevelDim,
            @Local(argsOnly = true, ordinal = 4) int light, @Local(argsOnly = true) @Nullable MapItemSavedData selectedKey,
            @Local ColumnPos centerMap, @Local(ordinal = 0) List<Runnable> pendingDecorations) {
        AbstractAtlasWidget widget = (AbstractAtlasWidget) (Object) this;
        List<AtlasTileCopies.Placed<MapDataHolder>> placed = AtlasTileCopies.shifted(this.exotic_worlds$copyShape,
                new AtlasTileCopies.TileCenter(centerMap.x(), centerMap.z()), Mth.ceil(zoomLevelDim),
                this.mapBlocksSize, widget::getMapWithCenter);
        if (placed.isEmpty()) {
            return;
        }

        float blocksPerPixel = (float) this.mapBlocksSize / AtlasTileCopies.MAP_PIXELS;
        Matrix3x2fStack pose = graphics.pose();
        List<Pair<Integer, Integer>> unframed = new ArrayList<>();
        for (AtlasTileCopies.Placed<MapDataHolder> copy : placed) {
            this.exotic_worlds$requestX = copy.at().x();
            this.exotic_worlds$requestZ = copy.at().z();
            MapDataHolder state = copy.map();
            boolean drawPlayerIcons = !this.drawBigPlayerMarker && state.data.dimension.equals(player.level().dimension());
            pose.pushMatrix();
            pose.translate((copy.at().x() - centerMap.x()) / blocksPerPixel,
                    (copy.at().z() - centerMap.z()) / blocksPerPixel);
            this.drawMap(player, graphics, unframed, unframed, 0, 0, state, drawPlayerIcons, light, selectedKey,
                    pendingDecorations);
            pose.popMatrix();
            unframed.clear();
        }

        graphics.nextStratum();
    }

    @WrapOperation(
            method = "getAndDrawMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/nanaky/ultimate_map_atlases/client/AbstractAtlasWidget;getMapWithCenter(II)Lnet/nanaky/ultimate_map_atlases/utils/MapDataHolder;"))
    private @Nullable MapDataHolder exotic_worlds$mapAtAnyCopy(AbstractAtlasWidget widget, int centerX, int centerZ,
            Operation<MapDataHolder> original) {
        this.exotic_worlds$requestX = centerX;
        this.exotic_worlds$requestZ = centerZ;
        return AtlasTileCopies.first(this.exotic_worlds$copyShape, centerX, centerZ, this.mapBlocksSize,
                (x, z) -> original.call(widget, x, z));
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

    @ModifyExpressionValue(
            method = "renderPriorityMarkers",
            at = @At(value = "INVOKE", target = "Lnet/nanaky/ultimate_map_atlases/utils/PriorityMarkerInfo;worldX()D"))
    private double exotic_worlds$markerXNearView(double worldX, @Local PriorityMarkerInfo marker,
            @Local(argsOnly = true) Slice slice) {
        return this.exotic_worlds$markerNearView(marker, slice).x;
    }

    @ModifyExpressionValue(
            method = "renderPriorityMarkers",
            at = @At(value = "INVOKE", target = "Lnet/nanaky/ultimate_map_atlases/utils/PriorityMarkerInfo;worldZ()D"))
    private double exotic_worlds$markerZNearView(double worldZ, @Local PriorityMarkerInfo marker,
            @Local(argsOnly = true) Slice slice) {
        return this.exotic_worlds$markerNearView(marker, slice).z;
    }

    @Unique
    private Vec3 exotic_worlds$markerNearView(PriorityMarkerInfo marker, Slice slice) {
        return AtlasPointSeat.toward(slice.dimension(), new Vec3(this.currentXCenter, 0.0, this.currentZCenter),
                marker.worldX(), marker.worldZ());
    }
}
