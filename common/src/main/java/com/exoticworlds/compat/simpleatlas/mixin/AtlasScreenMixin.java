package com.exoticworlds.compat.simpleatlas.mixin;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.simpleatlas.AtlasCopies;
import com.exoticworlds.compat.simpleatlas.AtlasLayoutFold;
import com.exoticworlds.compat.simpleatlas.AtlasTileFold;
import com.exoticworlds.compat.simpleatlas.AtlasView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import rubbertoe.simple_atlas.client.screen.AtlasScreen;
import rubbertoe.simple_atlas.client.screen.icon.AtlasIcon;
import rubbertoe.simple_atlas.client.screen.icon.PlayerAtlasIcon;
import rubbertoe.simple_atlas.network.AtlasTilePayload;

@Mixin(AtlasScreen.class)
public abstract class AtlasScreenMixin {
    private static final String WORLD_POINT_INIT =
            "Lrubbertoe/simple_atlas/client/screen/AtlasScreen$WorldPoint;<init>(DD)V";

    private static final String ICON_RENDER_ARGS =
            "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/Minecraft;Ljava/util/List;FFF)V";

    private static final int EDGE_ARGB = 0xCCFFFFFF;

    @Shadow
    @Final
    private List<AtlasTilePayload> tiles;

    @Shadow
    @Final
    private Map<String, Object> dimensionTileBounds;

    @Shadow
    private float zoom;

    @Shadow
    private double panX;

    @Shadow
    private double panY;

    @Unique
    private @Nullable List<AtlasCopies.Offset> toroidal$copies;

    @Unique
    private AtlasCopies.@Nullable Layout toroidal$copiesLayout;

    @Unique
    private @Nullable MapCopies toroidal$copiesMode;

    @Unique
    private @Nullable String toroidal$copiesDimension;

    @Unique
    private @Nullable AtlasView toroidal$copiesArea;

    @Unique
    private float toroidal$copiesOriginX;

    @Unique
    private float toroidal$copiesOriginY;

    @Unique
    private float toroidal$copiesTileSize;

    @Unique
    private @Nullable AtlasView toroidal$singleWorld;

    @Shadow
    private static Map<String, Object> buildDimensionTileBounds(List<AtlasTilePayload> tiles) {
        throw new AssertionError();
    }

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

    @Shadow
    private boolean isContextMenuOpen() {
        throw new AssertionError();
    }

    @Shadow
    private void renderHoveredTileHighlight(GuiGraphicsExtractor graphics, float x, float y, float scaledTileSize) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$relayTiles(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        MapItemSavedData first = this.tiles.isEmpty() || minecraft.level == null
                ? null
                : minecraft.level.getMapData(new MapId(this.tiles.getFirst().mapId()));
        if (first == null) {
            return;
        }

        List<AtlasTilePayload> relaid = AtlasLayoutFold.relaid(List.copyOf(this.tiles),
                AtlasTileFold.MAP_PIXELS << first.scale, AtlasTileFold::shapeOf, MapCopies.current());
        this.tiles.clear();
        this.tiles.addAll(relaid);
        this.dimensionTileBounds.clear();
        this.dimensionTileBounds.putAll(buildDimensionTileBounds(this.tiles));
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void toroidal$holdSingleView(CallbackInfo ci) {
        this.toroidal$singleWorld = null;
        if (MapCopies.current() != MapCopies.SINGLE) {
            return;
        }

        String dimension = this.getSelectedDimension();
        ToroidalShape shape = AtlasTileFold.shapeOf(dimension);
        AtlasTilePayload first = toroidal$firstTile(dimension);
        int blocksPerTile = toroidal$blocksPerTile(first);
        if (shape == null || first == null || blocksPerTile == 0) {
            return;
        }

        Screen screen = (Screen) (Object) this;
        AtlasView view = AtlasView.of(screen.width, screen.height);
        float floor = AtlasView.MIN_ZOOM;
        if (shape.loops(Direction.Axis.X)) {
            floor = Math.max(floor, AtlasView.zoomCovering(view.contentWidth(), shape.widthBlocks(Direction.Axis.X),
                    blocksPerTile));
        }

        if (shape.loops(Direction.Axis.Z)) {
            floor = Math.max(floor, AtlasView.zoomCovering(view.contentHeight(),
                    shape.widthBlocks(Direction.Axis.Z), blocksPerTile));
        }

        int columns = toroidal$span(dimension, Direction.Axis.X);
        int rows = toroidal$span(dimension, Direction.Axis.Z);
        float heldZoom = Math.min(AtlasView.MAX_ZOOM, Math.max(this.zoom, floor));
        if (heldZoom != this.zoom) {
            this.panX = toroidal$panKeepingCentre(this.panX, view.contentX(), view.contentWidth(), columns, this.zoom,
                    heldZoom);
            this.panY = toroidal$panKeepingCentre(this.panY, view.contentY(), view.contentHeight(), rows, this.zoom,
                    heldZoom);
            this.zoom = heldZoom;
        }

        float tileSize = AtlasView.TILE_PIXELS_AT_ZOOM_ONE * this.zoom;
        float pixelsPerBlock = tileSize / blocksPerTile;
        float worldX = view.contentX();
        float worldWidth = view.contentWidth();
        if (shape.loops(Direction.Axis.X)) {
            worldWidth = shape.widthBlocks(Direction.Axis.X) * pixelsPerBlock;
            this.panX += AtlasView.panInside(toroidal$worldStartX(view, dimension, first, columns, tileSize,
                    blocksPerTile, shape), worldWidth, view.contentX(), view.contentWidth());
            worldX = toroidal$worldStartX(view, dimension, first, columns, tileSize, blocksPerTile, shape);
        }

        float worldY = view.contentY();
        float worldHeight = view.contentHeight();
        if (shape.loops(Direction.Axis.Z)) {
            worldHeight = shape.widthBlocks(Direction.Axis.Z) * pixelsPerBlock;
            this.panY += AtlasView.panInside(toroidal$worldStartY(view, dimension, first, rows, tileSize,
                    blocksPerTile, shape), worldHeight, view.contentY(), view.contentHeight());
            worldY = toroidal$worldStartY(view, dimension, first, rows, tileSize, blocksPerTile, shape);
        }

        this.toroidal$singleWorld = new AtlasView(worldX, worldY, worldWidth, worldHeight);
    }

    @Unique
    private float toroidal$worldStartX(AtlasView view, String dimension, AtlasTilePayload first, int columns,
            float tileSize, int blocksPerTile, ToroidalShape shape) {
        float originX = view.contentX() + (view.contentWidth() - columns * tileSize) / 2.0F + (float) this.panX;
        return originX + this.localTileX(dimension, first) * tileSize
                + (shape.minBlock(Direction.Axis.X) - (first.centerX() - blocksPerTile / 2)) * tileSize / blocksPerTile;
    }

    @Unique
    private float toroidal$worldStartY(AtlasView view, String dimension, AtlasTilePayload first, int rows,
            float tileSize, int blocksPerTile, ToroidalShape shape) {
        float originY = view.contentY() + (view.contentHeight() - rows * tileSize) / 2.0F + (float) this.panY;
        return originY + this.localTileY(dimension, first) * tileSize
                + (shape.minBlock(Direction.Axis.Z) - (first.centerZ() - blocksPerTile / 2)) * tileSize / blocksPerTile;
    }

    @ModifyArg(method = "screenToWorldPoint", at = @At(value = "INVOKE", target = WORLD_POINT_INIT), index = 0)
    private double toroidal$foldClickX(double x, @Local(name = "tile") AtlasTilePayload tile,
            @Local(name = "worldZ") double z) {
        return AtlasTileFold.foldOnTile(tile, x, z).x;
    }

    @ModifyArg(method = "screenToWorldPoint", at = @At(value = "INVOKE", target = WORLD_POINT_INIT), index = 1)
    private double toroidal$foldClickZ(double z, @Local(name = "tile") AtlasTilePayload tile,
            @Local(name = "worldX") double x) {
        return AtlasTileFold.foldOnTile(tile, x, z).z;
    }

    @Inject(method = {"screenToWorldPoint", "findMapIdAtScreenPoint"}, at = @At("HEAD"))
    private void toroidal$pointerOntoBase(CallbackInfoReturnable<?> cir,
            @Local(argsOnly = true, ordinal = 0) LocalDoubleRef mouseX,
            @Local(argsOnly = true, ordinal = 1) LocalDoubleRef mouseY,
            @Local(argsOnly = true, ordinal = 0) float mapOriginX, @Local(argsOnly = true, ordinal = 1) float mapOriginY,
            @Local(argsOnly = true, ordinal = 2) float scaledTileSize) {
        List<AtlasCopies.Offset> copies = toroidal$tileCopies(mapOriginX, mapOriginY, scaledTileSize);
        AtlasCopies.Layout layout = this.toroidal$copiesLayout;
        if (layout == null) {
            return;
        }

        AtlasCopies.Offset under = AtlasCopies.under(mouseX.get(), mouseY.get(), copies, layout);
        mouseX.set(mouseX.get() - under.x());
        mouseY.set(mouseY.get() - under.y());
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;renderMapTile(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFFF)V"))
    private void toroidal$drawTileCopies(AtlasScreen screen, GuiGraphicsExtractor graphics, int mapId, float x,
            float y, float scale, Operation<Void> original, @Local(name = "tile") AtlasTilePayload tile,
            @Local(name = "mapOriginX") float mapOriginX, @Local(name = "mapOriginY") float mapOriginY,
            @Local(name = "scaledTileSize") float scaledTileSize,
            @Local(name = "mouseWithinAtlasContent") boolean mouseWithinAtlasContent,
            @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        AtlasView singleWorld = this.toroidal$singleWorld;
        for (AtlasCopies.Offset offset : toroidal$tileCopies(mapOriginX, mapOriginY, scaledTileSize)) {
            float copyX = x + offset.x();
            float copyY = y + offset.y();
            if (singleWorld != null) {
                graphics.enableScissor(Mth.floor(singleWorld.contentX()), Mth.floor(singleWorld.contentY()),
                        Mth.ceil(singleWorld.contentX() + singleWorld.contentWidth()),
                        Mth.ceil(singleWorld.contentY() + singleWorld.contentHeight()));
                original.call(screen, graphics, mapId, copyX, copyY, scale);
                graphics.disableScissor();
            } else {
                original.call(screen, graphics, mapId, copyX, copyY, scale);
                toroidal$drawWorldEdge(graphics, tile, copyX, copyY, scale);
            }

            boolean copy = offset.x() != 0.0F || offset.y() != 0.0F;
            if (copy && mouseWithinAtlasContent && !this.isContextMenuOpen() && mouseX >= copyX
                    && mouseX < copyX + scaledTileSize && mouseY >= copyY && mouseY < copyY + scaledTileSize) {
                this.renderHoveredTileHighlight(graphics, copyX, copyY, scaledTileSize);
            }
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon;render" + ICON_RENDER_ARGS))
    private void toroidal$drawIconCopies(AtlasIcon icon, GuiGraphicsExtractor graphics, Minecraft minecraft,
            List<AtlasTilePayload> visibleTiles, float mapOriginX, float mapOriginY, float scaledTileSize,
            Operation<Void> original) {
        for (AtlasCopies.Offset offset : toroidal$iconCopies(mapOriginX, mapOriginY, scaledTileSize)) {
            original.call(icon, graphics, minecraft, visibleTiles, mapOriginX + offset.x(), mapOriginY + offset.y(),
                    scaledTileSize);
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/icon/PlayerAtlasIcon;render" + ICON_RENDER_ARGS))
    private void toroidal$drawPlayerCopies(PlayerAtlasIcon icon, GuiGraphicsExtractor graphics, Minecraft minecraft,
            List<AtlasTilePayload> visibleTiles, float mapOriginX, float mapOriginY, float scaledTileSize,
            Operation<Void> original) {
        for (AtlasCopies.Offset offset : toroidal$iconCopies(mapOriginX, mapOriginY, scaledTileSize)) {
            original.call(icon, graphics, minecraft, visibleTiles, mapOriginX + offset.x(), mapOriginY + offset.y(),
                    scaledTileSize);
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;renderPinnedWaypointMarkers(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/Minecraft;FFF)V"))
    private void toroidal$drawPinnedCopies(AtlasScreen screen, GuiGraphicsExtractor graphics, Minecraft minecraft,
            float mapOriginX, float mapOriginY, float scaledTileSize, Operation<Void> original) {
        for (AtlasCopies.Offset offset : toroidal$iconCopies(mapOriginX, mapOriginY, scaledTileSize)) {
            original.call(screen, graphics, minecraft, mapOriginX + offset.x(), mapOriginY + offset.y(),
                    scaledTileSize);
        }
    }

    @WrapOperation(
            method = {"findHoveredIcon", "findHoveredWaypointIndex"},
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;resolveHoveredAnchor(Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon;Lnet/minecraft/client/Minecraft;Ljava/util/List;FFFII)Lrubbertoe/simple_atlas/client/screen/icon/AtlasIcon$Anchor;"))
    private AtlasIcon.@Nullable Anchor toroidal$hoverAnyCopy(AtlasScreen screen, AtlasIcon icon, Minecraft minecraft,
            List<AtlasTilePayload> visibleTiles, float mapOriginX, float mapOriginY, float scaledTileSize, int mouseX,
            int mouseY, Operation<AtlasIcon.Anchor> original) {
        for (AtlasCopies.Offset offset : toroidal$iconCopies(mapOriginX, mapOriginY, scaledTileSize)) {
            AtlasIcon.Anchor anchor = original.call(screen, icon, minecraft, visibleTiles, mapOriginX + offset.x(),
                    mapOriginY + offset.y(), scaledTileSize, mouseX, mouseY);
            if (anchor != null) {
                return anchor;
            }
        }

        return null;
    }

    @Unique
    private List<AtlasCopies.Offset> toroidal$iconCopies(float mapOriginX, float mapOriginY, float scaledTileSize) {
        return MapCopies.current() == MapCopies.SINGLE
                ? AtlasCopies.BASE_ONLY
                : toroidal$tileCopies(mapOriginX, mapOriginY, scaledTileSize);
    }

    @Unique
    private List<AtlasCopies.Offset> toroidal$tileCopies(float mapOriginX, float mapOriginY, float scaledTileSize) {
        MapCopies mode = MapCopies.current();
        Screen screen = (Screen) (Object) this;
        AtlasView area = mode == MapCopies.SINGLE ? this.toroidal$singleWorld : AtlasView.of(screen.width,
                screen.height);
        if (area == null) {
            return AtlasCopies.BASE_ONLY;
        }

        String dimension = this.getSelectedDimension();
        if (this.toroidal$copies != null && mode == this.toroidal$copiesMode
                && dimension.equals(this.toroidal$copiesDimension) && area.equals(this.toroidal$copiesArea)
                && mapOriginX == this.toroidal$copiesOriginX && mapOriginY == this.toroidal$copiesOriginY
                && scaledTileSize == this.toroidal$copiesTileSize) {
            return this.toroidal$copies;
        }

        AtlasTilePayload first = toroidal$firstTile(dimension);
        int blocksPerTile = toroidal$blocksPerTile(first);
        AtlasCopies.Layout layout = first == null || blocksPerTile == 0
                ? null
                : new AtlasCopies.Layout(mapOriginX, mapOriginY,
                        toroidal$span(dimension, Direction.Axis.X) * scaledTileSize,
                        toroidal$span(dimension, Direction.Axis.Z) * scaledTileSize,
                        first.centerX() - blocksPerTile / 2 - this.localTileX(dimension, first) * blocksPerTile,
                        first.centerZ() - blocksPerTile / 2 - this.localTileY(dimension, first) * blocksPerTile,
                        scaledTileSize / blocksPerTile);
        List<AtlasCopies.Offset> copies = layout == null
                ? AtlasCopies.BASE_ONLY
                : AtlasCopies.visible(AtlasTileFold.shapeOf(dimension), layout, area,
                        mode == MapCopies.SINGLE ? AtlasCopies.ANY_MOVE : blocksPerTile);
        this.toroidal$copies = copies;
        this.toroidal$copiesLayout = layout;
        this.toroidal$copiesMode = mode;
        this.toroidal$copiesDimension = dimension;
        this.toroidal$copiesArea = area;
        this.toroidal$copiesOriginX = mapOriginX;
        this.toroidal$copiesOriginY = mapOriginY;
        this.toroidal$copiesTileSize = scaledTileSize;
        return copies;
    }

    @Unique
    private int toroidal$span(String dimension, Direction.Axis axis) {
        int span = 0;
        for (AtlasTilePayload tile : this.tiles) {
            if (tile.dimension().equals(dimension)) {
                int local = axis == Direction.Axis.X
                        ? this.localTileX(dimension, tile)
                        : this.localTileY(dimension, tile);
                span = Math.max(span, local + 1);
            }
        }

        return span;
    }

    @Unique
    private @Nullable AtlasTilePayload toroidal$firstTile(String dimension) {
        for (AtlasTilePayload tile : this.tiles) {
            if (tile.dimension().equals(dimension)) {
                return tile;
            }
        }

        return null;
    }

    @Unique
    private static int toroidal$blocksPerTile(@Nullable AtlasTilePayload tile) {
        Minecraft minecraft = Minecraft.getInstance();
        MapItemSavedData data = tile == null || minecraft.level == null
                ? null
                : minecraft.level.getMapData(new MapId(tile.mapId()));
        return data == null ? 0 : AtlasTileFold.MAP_PIXELS << data.scale;
    }

    @Unique
    private static double toroidal$panKeepingCentre(double pan, float contentStart, float contentLength, int span,
            float oldZoom, float newZoom) {
        float oldTile = AtlasView.TILE_PIXELS_AT_ZOOM_ONE * oldZoom;
        float newTile = AtlasView.TILE_PIXELS_AT_ZOOM_ONE * newZoom;
        float centre = contentStart + contentLength / 2.0F;
        double oldOrigin = contentStart + (contentLength - span * oldTile) / 2.0F + pan;
        double tilesFromOrigin = (centre - oldOrigin) / oldTile;
        double newBase = contentStart + (contentLength - span * newTile) / 2.0F;
        return centre - tilesFromOrigin * newTile - newBase;
    }

    @Unique
    private static void toroidal$drawWorldEdge(GuiGraphicsExtractor graphics, AtlasTilePayload tile, float x, float y,
            float scale) {
        int blocksPerTile = toroidal$blocksPerTile(tile);
        if (blocksPerTile == 0) {
            return;
        }

        List<AtlasTileFold.SeamLine> lines = AtlasTileFold.seamLines(tile, blocksPerTile);
        if (lines.isEmpty()) {
            return;
        }

        int thickness = Math.max(1, Mth.ceil(1.0F / scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (AtlasTileFold.SeamLine line : lines) {
            if (line.vertical()) {
                toroidal$fillEdge(graphics, line.at(), line.from(), line.at() + thickness, line.to());
            } else {
                toroidal$fillEdge(graphics, line.from(), line.at(), line.to(), line.at() + thickness);
            }
        }

        graphics.pose().popMatrix();
    }

    @Unique
    private static void toroidal$fillEdge(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        graphics.fill(x0, y0, Math.min(x1, AtlasTileFold.MAP_PIXELS), Math.min(y1, AtlasTileFold.MAP_PIXELS),
                EDGE_ARGB);
    }
}
