package com.exoticworlds.compat.mapatlases.mixin;

import java.util.List;

import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.map.AtlasPointSeat;
import com.exoticworlds.compat.map.AtlasSeamLines;
import com.exoticworlds.compat.map.AtlasTileCopies;
import com.exoticworlds.compat.map.AtlasTileMemo;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.core.fold.WorldFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import pepjebs.mapatlases.client.AbstractAtlasDisplay;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.utils.AtlasMap;

@Mixin(AbstractAtlasDisplay.class)
public abstract class AbstractAtlasDisplayMixin {
    @Shadow
    protected int mapBlocksSize;

    @Shadow
    private static void pushMapTransform(GuiGraphicsExtractor graphics, int ix, int iy) {
    }

    @Shadow
    private void drawMap(GuiGraphicsExtractor graphics, int ix, int iy, AtlasMap map, int tint, boolean hovered) {
    }

    @Shadow
    private void drawDecorations(GuiGraphicsExtractor graphics, MapItemSavedData data, Player player, int tint) {
    }

    @Unique
    private @Nullable WorldFold exotic_worlds$copyFold;

    @Unique
    private @Nullable WorldFold exotic_worlds$seamFold;

    @Unique
    private final AtlasTileMemo exotic_worlds$tileMemo = new AtlasTileMemo();

    @Unique
    private int exotic_worlds$gridCenterX;

    @Unique
    private int exotic_worlds$gridCenterZ;

    @Unique
    private AtlasTileCopies.@Nullable TileCenter exotic_worlds$drawnShiftedCopy;

    @Inject(method = "drawAtlas", at = @At("HEAD"))
    private void exotic_worlds$resolveFold(CallbackInfo ci) {
        if ((Object) this instanceof MapWidgetAccessor widget) {
            WorldFold fold = ClientFolds.of(widget.exotic_worlds$getMapScreen().getSelectedSlice().dimension());
            MapCopies copies = MapCopies.getCurrent();
            this.exotic_worlds$copyFold = copies == MapCopies.SINGLE ? null : fold;
            this.exotic_worlds$seamFold = copies.isDrawingSeamLines() ? fold : null;
        } else {
            this.exotic_worlds$copyFold = ClientFolds.getCurrent();
            this.exotic_worlds$seamFold = null;
        }
    }

    @Inject(
            method = "drawAtlas",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;",
                    ordinal = 1, shift = At.Shift.AFTER))
    private void exotic_worlds$recordGridCenter(CallbackInfo ci, @Local ColumnPos centerMap) {
        this.exotic_worlds$gridCenterX = centerMap.x();
        this.exotic_worlds$gridCenterZ = centerMap.z();
    }

    @Inject(
            method = "drawAtlas",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;",
                    ordinal = 1, shift = At.Shift.AFTER))
    private void exotic_worlds$drawShiftedCopies(CallbackInfo ci, @Local(argsOnly = true) GuiGraphicsExtractor graphics,
            @Local(argsOnly = true) float zoomLevelDim, @Local(argsOnly = true, ordinal = 4) int tint,
            @Local Player player, @Local ColumnPos centerMap) {
        AbstractAtlasDisplay display = (AbstractAtlasDisplay) (Object) this;
        List<AtlasTileCopies.Placed<AtlasMap>> placed = AtlasTileCopies.shifted(this.exotic_worlds$copyFold,
                new AtlasTileCopies.TileCenter(centerMap.x(), centerMap.z()), Mth.ceil(zoomLevelDim),
                this.mapBlocksSize, display::getMapWithCenter);
        if (placed.isEmpty()) {
            return;
        }

        float blocksPerPixel = (float) this.mapBlocksSize / AtlasTileCopies.MAP_PIXELS;
        Matrix3x2fStack pose = graphics.pose();
        try {
            for (AtlasTileCopies.Placed<AtlasMap> copy : placed) {
                this.exotic_worlds$drawnShiftedCopy = copy.at();
                pose.pushMatrix();
                pose.translate((copy.at().x() - centerMap.x()) / blocksPerPixel,
                        (copy.at().z() - centerMap.z()) / blocksPerPixel);
                this.drawMap(graphics, 0, 0, copy.map(), tint, false);
                pose.popMatrix();
            }
        } finally {
            this.exotic_worlds$drawnShiftedCopy = null;
        }

        for (AtlasTileCopies.Placed<AtlasMap> copy : placed) {
            AtlasMap map = copy.map();
            pose.pushMatrix();
            pose.translate((copy.at().x() - centerMap.x()) / blocksPerPixel,
                    (copy.at().z() - centerMap.z()) / blocksPerPixel);
            pushMapTransform(graphics, 0, 0);
            this.drawDecorations(graphics, map.data, player, MapAtlasesClient.debugIsMapUpdated(tint, map.id, map.type));
            pose.popMatrix();
            pose.popMatrix();
        }

        graphics.nextStratum();
    }

    @WrapOperation(
            method = "drawAtlas",
            at = @At(value = "INVOKE",
                    target = "Lpepjebs/mapatlases/client/AbstractAtlasDisplay;getMapWithCenter(II)Lpepjebs/mapatlases/utils/AtlasMap;"))
    private @Nullable AtlasMap exotic_worlds$mapAtAnyCopy(AbstractAtlasDisplay display, int centerX, int centerZ,
            Operation<AtlasMap> original) {
        for (AtlasTileCopies.TileCenter copy : this.exotic_worlds$tileMemo.copies(this.exotic_worlds$copyFold,
                centerX, centerZ, this.mapBlocksSize)) {
            @Nullable AtlasMap map = original.call(display, copy.x(), copy.z());
            if (map != null) {
                return map;
            }
        }

        return null;
    }

    @ModifyExpressionValue(
            method = "drawMap",
            at = @At(value = "INVOKE", target = "Lpepjebs/mapatlases/client/AbstractAtlasDisplay;showMapBorders()Z"))
    private boolean exotic_worlds$noBorderOnShiftedCopy(boolean shows) {
        return shows && this.exotic_worlds$drawnShiftedCopy == null;
    }

    @Inject(
            method = "drawMap",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;"))
    private void exotic_worlds$drawSeamLines(CallbackInfo ci, @Local(argsOnly = true) GuiGraphicsExtractor graphics,
            @Local(argsOnly = true, ordinal = 0) int row, @Local(argsOnly = true, ordinal = 1) int column) {
        if (this.exotic_worlds$seamFold == null) {
            return;
        }

        AtlasTileCopies.TileCenter tile = this.exotic_worlds$drawnShiftedCopy != null
                ? this.exotic_worlds$drawnShiftedCopy
                : new AtlasTileCopies.TileCenter(this.exotic_worlds$gridCenterX + column * this.mapBlocksSize,
                        this.exotic_worlds$gridCenterZ + row * this.mapBlocksSize);
        AtlasSeamLines.draw(graphics, this.exotic_worlds$tileMemo.seamLines(this.exotic_worlds$seamFold, tile.x(),
                tile.z(), this.mapBlocksSize));
    }

    @ModifyExpressionValue(
            method = "makePlayerMarkers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D"))
    private double exotic_worlds$playerXOnMap(double x, @Local(argsOnly = true) MapItemSavedData data,
            @Local(ordinal = 1) Player shown) {
        return exotic_worlds$seatedOnMap(data, x, shown.getZ()).x;
    }

    @ModifyExpressionValue(
            method = "makePlayerMarkers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D"))
    private double exotic_worlds$playerZOnMap(double z, @Local(argsOnly = true) MapItemSavedData data,
            @Local(ordinal = 1) Player shown) {
        return exotic_worlds$seatedOnMap(data, shown.getX(), z).z;
    }

    @Unique
    private static Vec3 exotic_worlds$seatedOnMap(MapItemSavedData data, double x, double z) {
        return AtlasPointSeat.toward(data.dimension, new Vec3(data.centerX, 0.0, data.centerZ), x, z);
    }
}
