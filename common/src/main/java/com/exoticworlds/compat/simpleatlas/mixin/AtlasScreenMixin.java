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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.simpleatlas.AtlasCopies;
import com.exoticworlds.compat.simpleatlas.AtlasEdgeLines;
import com.exoticworlds.compat.simpleatlas.AtlasLayoutFold;
import com.exoticworlds.compat.simpleatlas.AtlasTileFold;
import com.exoticworlds.compat.simpleatlas.AtlasView;
import com.exoticworlds.compat.simpleatlas.AtlasViews;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    @Shadow
    @Final
    private List<AtlasTilePayload> tiles;

    @Shadow
    @Final
    private Map<String, Object> dimensionTileBounds;

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
    private boolean toroidal$seamLines;

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
    private void toroidal$readSeamLines(CallbackInfo ci) {
        this.toroidal$seamLines = MapCopies.current().drawsSeamLines();
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
        AtlasCopies.Offset under = toroidal$copyUnder(mouseX.get(), mouseY.get(), mapOriginX, mapOriginY,
                scaledTileSize);
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
            @Local(name = "scaledTileSize") float scaledTileSize) {
        AtlasView singleWorld = toroidal$views().toroidal$singleWorld();
        List<AtlasTileFold.SeamLine> lines = this.toroidal$seamLines ? toroidal$seamLinesOf(tile) : List.of();
        for (AtlasCopies.Offset offset : toroidal$tileCopies(mapOriginX, mapOriginY, scaledTileSize)) {
            float copyX = x + offset.x();
            float copyY = y + offset.y();
            if (singleWorld != null) {
                toroidal$scissorTo(graphics, singleWorld);
                original.call(screen, graphics, mapId, copyX, copyY, scale);
                graphics.disableScissor();
            } else {
                original.call(screen, graphics, mapId, copyX, copyY, scale);
                AtlasEdgeLines.draw(graphics, lines, copyX, copyY, scale);
            }
        }
    }

    @ModifyVariable(method = "extractRenderState", at = @At("STORE"), name = "hovered")
    private boolean toroidal$hoverTileCopy(boolean hovered, @Local(name = "x") float x, @Local(name = "y") float y,
            @Local(name = "mapOriginX") float mapOriginX, @Local(name = "mapOriginY") float mapOriginY,
            @Local(name = "scaledTileSize") float scaledTileSize,
            @Local(name = "mouseWithinAtlasContent") boolean mouseWithinAtlasContent,
            @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        AtlasCopies.Offset under = toroidal$copyUnder(mouseX, mouseY, mapOriginX, mapOriginY, scaledTileSize);
        double baseX = mouseX - under.x();
        double baseY = mouseY - under.y();
        return mouseWithinAtlasContent && baseX >= x && baseX < x + scaledTileSize && baseY >= y
                && baseY < y + scaledTileSize;
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lrubbertoe/simple_atlas/client/screen/AtlasScreen;renderHoveredTileHighlight(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FFF)V"))
    private void toroidal$highlightCopyUnder(AtlasScreen screen, GuiGraphicsExtractor graphics, float x, float y,
            float scaledTileSize, Operation<Void> original, @Local(name = "mapOriginX") float mapOriginX,
            @Local(name = "mapOriginY") float mapOriginY, @Local(argsOnly = true, ordinal = 0) int mouseX,
            @Local(argsOnly = true, ordinal = 1) int mouseY) {
        AtlasCopies.Offset under = toroidal$copyUnder(mouseX, mouseY, mapOriginX, mapOriginY, scaledTileSize);
        AtlasView singleWorld = toroidal$views().toroidal$singleWorld();
        if (singleWorld != null) {
            toroidal$scissorTo(graphics, singleWorld);
        }

        original.call(screen, graphics, x + under.x(), y + under.y(), scaledTileSize);
        if (singleWorld != null) {
            graphics.disableScissor();
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
    private AtlasCopies.Offset toroidal$copyUnder(double mouseX, double mouseY, float mapOriginX, float mapOriginY,
            float scaledTileSize) {
        List<AtlasCopies.Offset> copies = toroidal$tileCopies(mapOriginX, mapOriginY, scaledTileSize);
        AtlasCopies.Layout layout = this.toroidal$copiesLayout;
        return layout == null ? AtlasCopies.BASE : AtlasCopies.under(mouseX, mouseY, copies, layout);
    }

    @Unique
    private List<AtlasCopies.Offset> toroidal$tileCopies(float mapOriginX, float mapOriginY, float scaledTileSize) {
        MapCopies mode = MapCopies.current();
        AtlasViews views = toroidal$views();
        AtlasView area = mode == MapCopies.SINGLE ? views.toroidal$singleWorld() : views.toroidal$view();
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

        AtlasTilePayload first = AtlasTileFold.firstOf(this.tiles, dimension);
        int blocksPerTile = AtlasTileFold.blocksPerTile(Minecraft.getInstance().level, first);
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
    private AtlasViews toroidal$views() {
        return (AtlasViews) (Object) this;
    }

    @Unique
    private static void toroidal$scissorTo(GuiGraphicsExtractor graphics, AtlasView area) {
        graphics.enableScissor(Mth.floor(area.contentX()), Mth.floor(area.contentY()),
                Mth.ceil(area.contentX() + area.contentWidth()), Mth.ceil(area.contentY() + area.contentHeight()));
    }

    @Unique
    private static List<AtlasTileFold.SeamLine> toroidal$seamLinesOf(AtlasTilePayload tile) {
        int blocksPerTile = AtlasTileFold.blocksPerTile(Minecraft.getInstance().level, tile);
        return blocksPerTile == 0 ? List.of() : AtlasTileFold.seamLines(tile, blocksPerTile);
    }
}
