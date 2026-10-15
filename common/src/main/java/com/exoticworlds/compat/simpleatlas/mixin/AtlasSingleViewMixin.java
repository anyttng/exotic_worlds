package com.exoticworlds.compat.simpleatlas.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.simpleatlas.AtlasTileFold;
import com.exoticworlds.compat.simpleatlas.AtlasView;
import com.exoticworlds.compat.simpleatlas.AtlasViews;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

import rubbertoe.simple_atlas.client.screen.AtlasScreen;
import rubbertoe.simple_atlas.network.AtlasTilePayload;

@Mixin(AtlasScreen.class)
public abstract class AtlasSingleViewMixin implements AtlasViews {
    private static final String BUILD_CONTEXT = "buildMapInteractionContext";

    private static final String VIEWPORT = "Lrubbertoe/simple_atlas/client/screen/AtlasScreen$AtlasViewport;";

    private static final String GET_ATLAS_VIEWPORT =
            "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;getAtlasViewport()" + VIEWPORT;

    private static final String MAP_ORIGIN_ARGS = "(" + VIEWPORT + "FLjava/lang/String;)F";

    private static final String GET_MAP_ORIGIN_X =
            "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;getMapOriginX" + MAP_ORIGIN_ARGS;

    private static final String GET_MAP_ORIGIN_Y =
            "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;getMapOriginY" + MAP_ORIGIN_ARGS;

    @Shadow
    @Final
    private static int TILE_SIZE;

    @Shadow
    @Final
    private List<AtlasTilePayload> tiles;

    @Shadow
    private float zoom;

    @Shadow
    private double panX;

    @Shadow
    private double panY;

    @Unique
    private @Nullable AtlasView toroidal$view;

    @Unique
    private float toroidal$tileBeforeHold;

    @Unique
    private float toroidal$heldOriginX;

    @Unique
    private float toroidal$originBeforeHoldX;

    @Unique
    private @Nullable AtlasView toroidal$singleWorld;

    @Shadow
    private String getSelectedDimension() {
        throw new AssertionError();
    }

    @Shadow
    private int localTileX(String dimension, AtlasTilePayload tile) {
        throw new AssertionError();
    }

    @Shadow
    private int localTileY(String dimension, AtlasTilePayload tile) {
        throw new AssertionError();
    }

    @Override
    public @Nullable AtlasView toroidal$view() {
        return this.toroidal$view;
    }

    @Override
    public @Nullable AtlasView toroidal$singleWorld() {
        return this.toroidal$singleWorld;
    }

    @Coerce
    @WrapOperation(method = BUILD_CONTEXT, at = @At(value = "INVOKE", target = GET_ATLAS_VIEWPORT))
    private Object toroidal$holdSingleZoom(AtlasScreen screen, Operation<Object> original) {
        Object viewport = original.call(screen);
        AtlasViewportAccessor content = (AtlasViewportAccessor) viewport;
        AtlasView view = new AtlasView(content.toroidal$contentX(), content.toroidal$contentY(),
                content.toroidal$contentWidth(), content.toroidal$contentHeight());
        this.toroidal$view = view;
        this.toroidal$singleWorld = null;
        this.toroidal$tileBeforeHold = 0.0F;
        if (MapCopies.current() != MapCopies.SINGLE) {
            return viewport;
        }

        String dimension = this.getSelectedDimension();
        ToroidalShape shape = AtlasTileFold.shapeOf(dimension);
        int blocksPerTile = AtlasTileFold.blocksPerTile(Minecraft.getInstance().level,
                AtlasTileFold.firstOf(this.tiles, dimension));
        if (shape == null || blocksPerTile == 0) {
            return viewport;
        }

        float heldZoom = this.zoom;
        if (shape.loops(Direction.Axis.X)) {
            heldZoom = Math.max(heldZoom, AtlasView.zoomCovering(view.contentWidth(),
                    shape.widthBlocks(Direction.Axis.X), blocksPerTile, TILE_SIZE));
        }

        if (shape.loops(Direction.Axis.Z)) {
            heldZoom = Math.max(heldZoom, AtlasView.zoomCovering(view.contentHeight(),
                    shape.widthBlocks(Direction.Axis.Z), blocksPerTile, TILE_SIZE));
        }

        this.toroidal$tileBeforeHold = TILE_SIZE * this.zoom;
        this.zoom = heldZoom;
        return viewport;
    }

    @WrapOperation(method = BUILD_CONTEXT, at = @At(value = "INVOKE", target = GET_MAP_ORIGIN_X))
    private float toroidal$readOriginX(AtlasScreen screen, @Coerce Object viewport, float scaledTileSize,
            String dimension, Operation<Float> original) {
        float origin = original.call(screen, viewport, scaledTileSize, dimension);
        float tileBefore = this.toroidal$tileBeforeHold;
        if (tileBefore != 0.0F) {
            this.toroidal$heldOriginX = origin;
            this.toroidal$originBeforeHoldX = tileBefore == scaledTileSize
                    ? origin
                    : original.call(screen, viewport, tileBefore, dimension);
        }

        return origin;
    }

    @WrapOperation(method = BUILD_CONTEXT, at = @At(value = "INVOKE", target = GET_MAP_ORIGIN_Y))
    private float toroidal$holdSinglePan(AtlasScreen screen, @Coerce Object viewport, float scaledTileSize,
            String dimension, Operation<Float> original) {
        float origin = original.call(screen, viewport, scaledTileSize, dimension);
        float tileBefore = this.toroidal$tileBeforeHold;
        if (tileBefore != 0.0F) {
            float originBefore = tileBefore == scaledTileSize
                    ? origin
                    : original.call(screen, viewport, tileBefore, dimension);
            toroidal$holdSinglePan(dimension, scaledTileSize, origin, originBefore);
        }

        return origin;
    }

    @Unique
    private void toroidal$holdSinglePan(String dimension, float tile, float originY, float originBeforeY) {
        AtlasView view = this.toroidal$view;
        ToroidalShape shape = AtlasTileFold.shapeOf(dimension);
        AtlasTilePayload first = AtlasTileFold.firstOf(this.tiles, dimension);
        int blocksPerTile = AtlasTileFold.blocksPerTile(Minecraft.getInstance().level, first);
        if (view == null || shape == null || first == null || blocksPerTile == 0) {
            return;
        }

        float originX = this.toroidal$heldOriginX;
        float tileBefore = this.toroidal$tileBeforeHold;
        if (tileBefore != tile) {
            this.panX = AtlasView.panKeepingCentre(this.panX, view.contentX() + view.contentWidth() / 2.0F,
                    this.toroidal$originBeforeHoldX, tileBefore, originX, tile);
            this.panY = AtlasView.panKeepingCentre(this.panY, view.contentY() + view.contentHeight() / 2.0F,
                    originBeforeY, tileBefore, originY, tile);
        }

        float pixelsPerBlock = tile / blocksPerTile;
        float worldX = view.contentX();
        float worldWidth = view.contentWidth();
        if (shape.loops(Direction.Axis.X)) {
            worldWidth = shape.widthBlocks(Direction.Axis.X) * pixelsPerBlock;
            float start = toroidal$worldStart(originX + (float) this.panX, this.localTileX(dimension, first),
                    first.centerX(), shape.minBlock(Direction.Axis.X), tile, blocksPerTile);
            float shift = AtlasView.panInside(start, worldWidth, view.contentX(), view.contentWidth());
            this.panX += shift;
            worldX = start + shift;
        }

        float worldY = view.contentY();
        float worldHeight = view.contentHeight();
        if (shape.loops(Direction.Axis.Z)) {
            worldHeight = shape.widthBlocks(Direction.Axis.Z) * pixelsPerBlock;
            float start = toroidal$worldStart(originY + (float) this.panY, this.localTileY(dimension, first),
                    first.centerZ(), shape.minBlock(Direction.Axis.Z), tile, blocksPerTile);
            float shift = AtlasView.panInside(start, worldHeight, view.contentY(), view.contentHeight());
            this.panY += shift;
            worldY = start + shift;
        }

        this.toroidal$singleWorld = new AtlasView(worldX, worldY, worldWidth, worldHeight);
    }

    @Unique
    private static float toroidal$worldStart(float mapOrigin, int localTile, int tileCentre, int minBlock, float tile,
            int blocksPerTile) {
        return mapOrigin + localTile * tile + (minBlock - (tileCentre - blocksPerTile / 2)) * tile / blocksPerTile;
    }
}
