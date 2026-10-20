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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
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

    @Unique
    private static boolean exotic_worlds$anchorPass;

    @Unique
    private String exotic_worlds$lastLevelDimension;

    @Unique
    private File exotic_worlds$lastWorldDir;

    @Inject(method = CENTER, at = @At("HEAD"))
    private void exotic_worlds$dropTilesOnWorldChange(File worldDir, MapType mapType, double blockX, double blockZ,
            int zoom, CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        String dimension = level.dimension().identifier().toString();
        if (JourneyMapFold.dropsGrid(exotic_worlds$lastLevelDimension, dimension, exotic_worlds$lastWorldDir, worldDir,
                this.regions.size())) {
            this.clear();
        }

        exotic_worlds$lastLevelDimension = dimension;
        if (worldDir != null) {
            exotic_worlds$lastWorldDir = worldDir;
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;DDFZLjava/util/List;DDLorg/joml/Matrix3x2f;)V",
            at = @At("HEAD"))
    private void exotic_worlds$recordView(CallbackInfo ci) {
        JourneyMapFold.recordView(this.getContext(), this.centerBlockX, this.centerBlockZ, this.regions.size());
    }

    @WrapOperation(
            method = "loadInMemoryRegions",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/model/region/RegionImageCache;getRegionImageSets()Ljava/util/Collection;"))
    private Collection<RegionImageSet> exotic_worlds$onlyThisWorldsRegions(RegionImageCache cache,
            Operation<Collection<RegionImageSet>> original) {
        Collection<RegionImageSet> sets = original.call(cache);
        File dir = this.worldDir;
        if (dir == null) {
            return sets;
        }

        return sets.stream().filter(set -> dir.equals(set.getRegionCoord().worldDir)).toList();
    }

    @WrapMethod(method = CENTER)
    private boolean exotic_worlds$foldCenter(File worldDir, MapType mapType, double blockX, double blockZ, int zoom,
            Operation<Boolean> original) {
        JourneyMapFold.Center center = JourneyMapFold.centerView(this.getUIState().ui, blockX, blockZ, zoom);
        return original.call(worldDir, mapType, center.x(), center.z(), center.zoom());
    }

    @Inject(method = BLOCK_PIXEL_IN_GRID, at = @At("HEAD"))
    private void exotic_worlds$beginAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        exotic_worlds$anchorPass = true;
    }

    @Inject(method = BLOCK_PIXEL_IN_GRID, at = @At("RETURN"))
    private void exotic_worlds$endAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        exotic_worlds$anchorPass = false;
    }

    @WrapMethod(method = BLOCK_COORD_PIXEL_IN_GRID)
    private Point2D.Double exotic_worlds$seatPixel(double blockX, double blockZ, Operation<Point2D.Double> original) {
        if (exotic_worlds$anchorPass) {
            return original.call(blockX, blockZ);
        }

        Vec3 seated = JourneyMapFold.seatPixel(this.centerBlockX, this.centerBlockZ, blockX, blockZ,
                JourneyMapFold.copiesOf(this.getUIState().ui));
        return original.call(seated.x, seated.z);
    }

    @ModifyVariable(method = MOVE, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double exotic_worlds$stopMoveXAtTheEdge(double deltaBlockX) {
        return JourneyMapFold.moveDelta(this.getUIState().ui, Direction.Axis.X, this.centerBlockX, deltaBlockX, this.zoom);
    }

    @ModifyVariable(method = MOVE, at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double exotic_worlds$stopMoveZAtTheEdge(double deltaBlockZ) {
        return JourneyMapFold.moveDelta(this.getUIState().ui, Direction.Axis.Z, this.centerBlockZ, deltaBlockZ, this.zoom);
    }

    @Inject(method = "updateGrid", at = @At(value = "FIELD", target = LAST_GRID_TOKEN, opcode = Opcodes.PUTFIELD))
    private void exotic_worlds$addTheSeamsFarSide(RegionCoord centerRegion, int gridSize, long token, CallbackInfo ci,
            @Local Set<String> imageFiles) {
        if (imageFiles == null) {
            return;
        }

        for (long region : JourneyMapFold.farSideRegions(imageFiles, centerRegion.regionX, centerRegion.regionZ, gridSize)) {
            RegionCoord coord = RegionCoord.fromRegionPos(this.worldDir, ChunkPos.getX(region), ChunkPos.getZ(region),
                    this.state.getDimension());
            this.regions.putIfAbsent(token, coord, () -> RegionTileAccessor.exotic_worlds$create(coord, this.state, this::markSurfaceDirty));
        }
    }

    @ModifyVariable(method = "setZoom(D)Z", at = @At("HEAD"), argsOnly = true)
    private double exotic_worlds$floorFullscreenZoom(double zoom) {
        return JourneyMapFold.flooredZoom(this.getUIState().ui, zoom);
    }

    @Override
    public void exotic_worlds$drawSeams(GuiGraphicsExtractor graphics, Matrix3x2fStack pose,
            MultiBufferSource.BufferSource buffers, double offsetX, double offsetZ) {
        List<WorldCopies.Edge> seams = JourneyMapFold.fullscreenSeams(this.getUIState().ui, this.centerBlockX,
                this.centerBlockZ, this.zoom);
        if (seams.isEmpty()) {
            return;
        }

        VertexConsumer seamQuads = buffers.getBuffer(JMRenderTypes.RECTANGLE_RENDER_TYPE);
        for (WorldCopies.Edge seam : seams) {
            Point2D.Double from = this.getBlockPixelInGrid(new BlockPos(seam.fromX(), 0, seam.fromZ()));
            Point2D.Double to = this.getBlockPixelInGrid(new BlockPos(seam.toX(), 0, seam.toZ()));
            int x0 = (int) (from.x + offsetX);
            int y0 = (int) (from.y + offsetZ);
            int x1 = Math.max((int) (to.x + offsetX), x0 + 1);
            int y1 = Math.max((int) (to.y + offsetZ), y0 + 1);
            DrawUtil.drawRectangle(pose, seamQuads, x0, y0, x1 - x0, y1 - y0, SEAM_ARGB & 0xFFFFFF, SEAM_ARGB >>> 24);
        }
    }
}
