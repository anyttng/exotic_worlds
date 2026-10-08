package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
import journeymap.client.model.map.MapState;
import journeymap.client.model.map.MapType;
import journeymap.client.model.region.RegionCoord;
import journeymap.client.model.region.RegionImageCache;
import journeymap.client.model.region.RegionImageSet;
import journeymap.client.render.map.RegionTile;
import journeymap.client.render.map.TileGrid;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Mixin(targets = "journeymap.client.render.map.MapRenderer", remap = false)
public abstract class MapRendererMixin {
    @Unique
    private static final String CENTER = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z";

    @Unique
    private static final String BLOCK_PIXEL_IN_GRID =
            "getBlockPixelInGrid(Lnet/minecraft/core/BlockPos;)Ljava/awt/geom/Point2D$Double;";

    @Unique
    private static final String BLOCK_COORD_PIXEL_IN_GRID = "getBlockPixelInGrid(DD)Ljava/awt/geom/Point2D$Double;";

    @Unique
    private static final String MOVE = "move(DD)V";

    @Unique
    private static final String RENDER = "render(Lnet/minecraft/client/gui/GuiGraphics;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDFZ)V";

    @Unique
    private static final String LAST_GRID_TOKEN = "Ljourneymap/client/render/map/MapRenderer;lastGridToken:J";

    @Shadow(remap = false)
    protected double centerBlockX;

    @Shadow(remap = false)
    protected double centerBlockZ;

    @Shadow(remap = false)
    protected int zoom;

    @Shadow(remap = false)
    private volatile File worldDir;

    @Shadow(remap = false)
    protected volatile MapState state;

    @Shadow(remap = false)
    @Final
    TileGrid<RegionCoord, RegionTile> regions;

    @Shadow(remap = false)
    public abstract void clear();

    @Shadow(remap = false)
    @Final
    protected Context.UI contextUi;

    @Shadow(remap = false)
    public abstract UIState getUIState();

    // Render-thread only, like every caller of these methods.
    @Unique
    private static boolean toroidal$anchorPass;

    @Unique
    private ResourceKey<Level> toroidal$lastLevelDimension;

    @Unique
    private File toroidal$lastWorldDir;

    @Inject(method = CENTER, at = @At("HEAD"))
    private void toroidal$dropTilesOnWorldChange(File worldDir, MapType mapType, double blockX, double blockZ,
            int zoom, CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        String reason = JourneyMapFold.staleGridReason(toroidal$lastLevelDimension, dimension,
                toroidal$lastWorldDir, worldDir);
        if (reason != null) {
            boolean byWorld = JourneyMapFold.WORLD_CHANGED.equals(reason);
            JourneyMapFold.gridDropped(reason,
                    byWorld ? toroidal$lastWorldDir.getName() : toroidal$lastLevelDimension.location().toString(),
                    byWorld ? worldDir.getName() : dimension.location().toString(),
                    this.regions.size());
            this.clear();
        }

        toroidal$lastLevelDimension = dimension;
        if (worldDir != null) {
            toroidal$lastWorldDir = worldDir;
        }
    }

    @Inject(method = RENDER, at = @At("HEAD"))
    private void toroidal$recordView(CallbackInfo ci) {
        JourneyMapFold.recordView(this.contextUi, this.centerBlockX, this.centerBlockZ, this.regions.size());
    }

    @WrapOperation(
            method = "loadInMemoryRegions",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/model/region/RegionImageCache;getRegionImageSets()Ljava/util/Collection;"))
    private Collection<RegionImageSet> toroidal$onlyThisWorldsRegions(RegionImageCache cache,
            Operation<Collection<RegionImageSet>> original) {
        Collection<RegionImageSet> sets = original.call(cache);
        File dir = this.worldDir;
        if (dir == null) {
            return sets;
        }

        return sets.stream().filter(set -> dir.equals(set.getRegionCoord().worldDir)).toList();
    }

    @WrapMethod(method = CENTER)
    private boolean toroidal$keepSingleViewOnTheMap(File worldDir, MapType mapType, double blockX, double blockZ, int zoom,
            Operation<Boolean> original) {
        if (JourneyMapFold.copiesOf(this.getUIState().ui) != MapCopies.SINGLE || !JourneyMapFold.active()) {
            return original.call(worldDir, mapType, blockX, blockZ, zoom);
        }

        Window window = Minecraft.getInstance().getWindow();
        int flooredZoom = Math.max(zoom, JourneyMapFold.fullscreenZoomFloor());
        return original.call(worldDir, mapType,
                JourneyMapFold.seatSingleCenter(Direction.Axis.X, blockX, flooredZoom, window.getWidth()),
                JourneyMapFold.seatSingleCenter(Direction.Axis.Z, blockZ, flooredZoom, window.getHeight()),
                flooredZoom);
    }

    @ModifyVariable(
            method = CENTER,
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$foldCenterX(double blockX) {
        return JourneyMapFold.foldCenterCoord(Direction.Axis.X, blockX);
    }

    @ModifyVariable(
            method = CENTER,
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$foldCenterZ(double blockZ) {
        return JourneyMapFold.foldCenterCoord(Direction.Axis.Z, blockZ);
    }

    @Inject(method = BLOCK_PIXEL_IN_GRID, at = @At("HEAD"))
    private void toroidal$beginAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        toroidal$anchorPass = true;
    }

    @Inject(method = BLOCK_PIXEL_IN_GRID, at = @At("RETURN"))
    private void toroidal$endAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        toroidal$anchorPass = false;
    }

    @ModifyVariable(method = BLOCK_COORD_PIXEL_IN_GRID, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$foldPixelX(double blockX) {
        return toroidal$anchorPass ? blockX : JourneyMapFold.seatPixelCoord(
                Direction.Axis.X, this.centerBlockX, blockX, JourneyMapFold.copiesOf(this.getUIState().ui));
    }

    @ModifyVariable(method = BLOCK_COORD_PIXEL_IN_GRID, at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$foldPixelZ(double blockZ) {
        return toroidal$anchorPass ? blockZ : JourneyMapFold.seatPixelCoord(
                Direction.Axis.Z, this.centerBlockZ, blockZ, JourneyMapFold.copiesOf(this.getUIState().ui));
    }

    @ModifyVariable(method = MOVE, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$stopMoveXAtTheEdge(double deltaBlockX) {
        return JourneyMapFold.copiesOf(this.getUIState().ui) == MapCopies.SINGLE
                ? JourneyMapFold.clampedMove(Direction.Axis.X, this.centerBlockX, deltaBlockX, this.zoom,
                        Minecraft.getInstance().getWindow().getWidth())
                : deltaBlockX;
    }

    @ModifyVariable(method = MOVE, at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$stopMoveZAtTheEdge(double deltaBlockZ) {
        return JourneyMapFold.copiesOf(this.getUIState().ui) == MapCopies.SINGLE
                ? JourneyMapFold.clampedMove(Direction.Axis.Z, this.centerBlockZ, deltaBlockZ, this.zoom,
                        Minecraft.getInstance().getWindow().getHeight())
                : deltaBlockZ;
    }

    @Inject(method = "updateGrid", at = @At(value = "FIELD", target = LAST_GRID_TOKEN, opcode = Opcodes.PUTFIELD))
    private void toroidal$addTheSeamsFarSide(RegionCoord centerRegion, int gridSize, long token, CallbackInfo ci,
            @Local Set<String> imageFiles) {
        if (imageFiles == null || !JourneyMapFold.active()) {
            return;
        }

        int regionCount = gridSize / 2;
        int minX = centerRegion.regionX - regionCount;
        int maxX = centerRegion.regionX + regionCount;
        int minZ = centerRegion.regionZ - regionCount;
        int maxZ = centerRegion.regionZ + regionCount;
        for (long region : JourneyMapFold.gridRegions(minX, minZ, maxX, maxZ)) {
            int x = ChunkPos.getX(region);
            int z = ChunkPos.getZ(region);
            boolean walkedByJourneyMap = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            if (!walkedByJourneyMap && imageFiles.contains(x + "," + z + ".png")) {
                RegionCoord coord = RegionCoord.fromRegionPos(this.worldDir, x, z, this.state.getDimension());
                this.regions.putIfAbsent(token, coord, () -> RegionTileAccessor.toroidal$create(coord, this.state));
            }
        }
    }

    @ModifyVariable(method = "setZoom(D)Z", at = @At("HEAD"), argsOnly = true)
    private double toroidal$floorFullscreenZoom(double zoom) {
        if (!Context.UI.Fullscreen.equals(this.getUIState().ui)) {
            return zoom;
        }

        return Math.max(zoom, JourneyMapFold.fullscreenZoomFloor());
    }

    @WrapOperation(
            method = RENDER,
            at = @At(
                    value = "INVOKE",
                    target = "Ljourneymap/client/render/map/RegionTile;render(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;DDFLjourneymap/client/model/map/MapType;I)V"))
    private void toroidal$renderWrappedCopies(@Coerce Object tile, GuiGraphics graphics, MultiBufferSource buffers,
            double pixelOffsetX, double pixelOffsetZ, float alpha, @Coerce Object mapType, int shaderIndex,
            Operation<Void> original) {
        original.call(tile, graphics, buffers, pixelOffsetX, pixelOffsetZ, alpha, mapType, shaderIndex);
        if (this.contextUi == Context.UI.Webmap) {
            return;
        }

        JourneyMapFold.View view = JourneyMapFold.viewOf(this.contextUi);
        if (JourneyMapFold.loopedAxes() == 0 || view == null) {
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        int viewportX = toroidal$viewportPixels(this.contextUi, window.getWidth());
        int viewportZ = toroidal$viewportPixels(this.contextUi, window.getHeight());
        int[] spanX = JourneyMapFold.viewSpan(view.centerX(), viewportX, this.zoom);
        int[] spanZ = JourneyMapFold.viewSpan(view.centerZ(), viewportZ, this.zoom);
        List<WorldCopies.Copy> drawn = JourneyMapFold.drawnCopies(view.tiles(), spanX, spanZ,
                JourneyMapFold.copiesOf(this.contextUi));
        JourneyMapFold.recordCopies(this.contextUi, drawn);
        RegionCoord region = ((RegionTile) tile).getRegionCoord();
        List<WorldCopies.Copy> copies = JourneyMapFold.tileCopies(drawn,
                region.regionX * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS,
                region.regionZ * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS,
                FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS, spanX, spanZ);
        double pixelsPerBlock = JourneyMapFold.pixelsPerBlock(this.zoom);
        for (WorldCopies.Copy copy : copies) {
            original.call(tile, graphics, buffers, pixelOffsetX + copy.dx() * pixelsPerBlock,
                    pixelOffsetZ + copy.dz() * pixelsPerBlock, alpha, mapType, shaderIndex);
        }
    }

    @Unique
    private static int toroidal$viewportPixels(Context.UI context, int windowPixels) {
        if (context != Context.UI.Minimap) {
            return windowPixels;
        }

        DisplayVars displayVars = UIManager.INSTANCE.getMiniMap().getDisplayVars();
        return displayVars == null
                ? windowPixels
                : (int) Math.ceil(Math.hypot(displayVars.minimapWidth, displayVars.minimapHeight));
    }
}
