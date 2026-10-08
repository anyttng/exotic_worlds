package com.exoticworlds.compat.journeymap.mixin;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.compat.journeymap.JourneyMapFold;
import com.exoticworlds.compat.journeymap.JourneyMapSeamPass;

import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.client.display.Context;
import journeymap.client.model.map.MapState;
import journeymap.client.model.map.MapType;
import journeymap.client.model.region.RegionCoord;
import journeymap.client.model.region.RegionImageCache;
import journeymap.client.model.region.RegionImageSet;
import journeymap.client.render.JMRenderTypes;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.RegionTile;
import journeymap.client.render.map.TileGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;

@Mixin(targets = "journeymap.client.render.map.MapRenderer", remap = false)
public abstract class MapRendererMixin implements JourneyMapSeamPass {
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
    public abstract UIState getUIState();

    @Shadow(remap = false)
    public abstract Point2D.Double getBlockPixelInGrid(BlockPos pos);

    @Shadow(remap = false)
    public abstract Context.UI getContext();

    @Shadow(remap = false)
    private void markSurfaceDirty() {
    }

    @Unique
    private static final int SEAM_ARGB = 0x59FFFFFF;

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
                    byWorld ? toroidal$lastWorldDir.getName() : toroidal$lastLevelDimension.identifier().toString(),
                    byWorld ? worldDir.getName() : dimension.identifier().toString(),
                    this.regions.size());
            this.clear();
        }

        toroidal$lastLevelDimension = dimension;
        if (worldDir != null) {
            toroidal$lastWorldDir = worldDir;
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;DDFZLjava/util/List;DDLorg/joml/Matrix3x2f;)V",
            at = @At("HEAD"))
    private void toroidal$recordView(CallbackInfo ci) {
        JourneyMapFold.recordView(this.getContext(), this.centerBlockX, this.centerBlockZ, this.regions.size());
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
                this.regions.putIfAbsent(token, coord, () -> RegionTileAccessor.toroidal$create(coord, this.state, this::markSurfaceDirty));
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

    @Override
    public void toroidal$drawSeams(GuiGraphicsExtractor graphics, Matrix3x2fStack pose,
            MultiBufferSource.BufferSource buffers, double offsetX, double offsetZ) {
        if (!Context.UI.Fullscreen.equals(this.getUIState().ui) || JourneyMapFold.loopedAxes() == 0
                || JourneyMapFold.copiesOf(Context.UI.Fullscreen) == MapCopies.SINGLE) {
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = JourneyMapFold.viewSpan(this.centerBlockX, window.getWidth(), this.zoom);
        int[] spanZ = JourneyMapFold.viewSpan(this.centerBlockZ, window.getHeight(), this.zoom);
        VertexConsumer seamQuads = buffers.getBuffer(JMRenderTypes.RECTANGLE_RENDER_TYPE);
        for (WorldCopies.Edge seam : JourneyMapFold.seams(spanX, spanZ)) {
            Point2D.Double from = this.getBlockPixelInGrid(new BlockPos(seam.fromX(), 0, seam.fromZ()));
            Point2D.Double to = this.getBlockPixelInGrid(new BlockPos(seam.toX(), 0, seam.toZ()));
            int x0 = (int) (from.x + offsetX);
            int y0 = (int) (from.y + offsetZ);
            int x1 = (int) (to.x + offsetX);
            int y1 = (int) (to.y + offsetZ);
            toroidal$fillSeam(pose, seamQuads, x0, y0, Math.max(x1 - x0, 1), Math.max(y1 - y0, 1));
        }
    }

    @Unique
    private static void toroidal$fillSeam(Matrix3x2fStack pose, VertexConsumer quads, int x, int y, int width,
            int height) {
        DrawUtil.drawRectangle(pose, quads, x, y, width, height, SEAM_ARGB & 0xFFFFFF, SEAM_ARGB >>> 24);
    }
}
