package com.exoticworlds.compat.journeymap;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapCopyBudget;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import journeymap.api.v2.client.display.Context;

public final class JourneyMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REGION_CHUNKS = 32;

    public static final String WORLD_CHANGED = "world";
    public static final String DIMENSION_CHANGED = "dimension";

    private static int fullscreenRangeX;
    private static int fullscreenRangeZ;
    private static int minimapRangeX;
    private static int minimapRangeZ;
    private static @Nullable View fullscreenView;
    private static @Nullable View minimapView;

    public static int foldRegionChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static double foldCenterCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? coord : shape.foldCoord(axis, coord);
    }

    public static double seatPixelCoord(Direction.Axis axis, double ref, double coord, MapCopies copies) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return coord;
        }

        return copies == MapCopies.SINGLE ? shape.foldCoord(axis, coord) : shape.nearestCoord(axis, ref, coord);
    }

    public static MapCopies copiesOf(Context.UI ui) {
        return ui == Context.UI.Fullscreen ? MapCopies.current() : MapCopies.REPEATED;
    }

    public static double clampedMove(Direction.Axis axis, double center, double delta, int zoom, int windowPixels) {
        return copies(axis).clampView(center + delta, halfViewBlocks(zoom, windowPixels)) - center;
    }

    public static double seatSingleCenter(Direction.Axis axis, double coord, int zoom, int windowPixels) {
        return copies(axis).clampView(foldCenterCoord(axis, coord), halfViewBlocks(zoom, windowPixels));
    }

    private static double halfViewBlocks(int zoom, int windowPixels) {
        return windowPixels / 2.0 * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS / Math.max(1, zoom);
    }

    public static Vec3 nearestToPlayer(Vec3 position) {
        return ClientFrame.nearestToPlayer(position);
    }

    public static boolean active() {
        return ClientShapes.current() != null;
    }

    public static int foldUiCoord(Direction.Axis axis, int coord) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static BlockPos foldUiBlock(BlockPos pos) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null || pos == null ? pos : shape.fold(pos);
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static double worldPixelPeriod(Direction.Axis axis, int zoom) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null || !shape.loops(axis)) {
            return 0.0;
        }

        return shape.widthBlocks(axis) * (zoom / (double) FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
    }

    public static int loopedAxes() {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        return (shape.loops(Direction.Axis.X) ? 1 : 0) + (shape.loops(Direction.Axis.Z) ? 1 : 0);
    }

    public static int zoomFloor() {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? 0 : FullscreenZoomFloor.journeyMapZoom(shape);
    }

    public static int fullscreenZoomFloor() {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        if (MapCopies.current() != MapCopies.SINGLE) {
            return FullscreenZoomFloor.journeyMapZoom(shape);
        }

        Window window = Minecraft.getInstance().getWindow();
        return FullscreenZoomFloor.journeyMapCoverZoom(shape, window.getWidth(), window.getHeight());
    }

    public static int seatFullscreenZoom(int zoom) {
        return FullscreenZoomFloor.journeyMapSeatedZoom(zoom, fullscreenZoomFloor(), MapCopies.current());
    }

    public static int[] viewSpan(double centerBlock, int windowPixels, int zoom) {
        double halfSpanBlocks = halfViewBlocks(zoom, windowPixels);
        return new int[] {(int) Math.floor(centerBlock - halfSpanBlocks), (int) Math.ceil(centerBlock + halfSpanBlocks)};
    }

    public static int[] tileLaps(AxisCopies copies, int tileMin, int tileSize, int spanMin, int spanMax, int range) {
        if (!copies.loops()) {
            return new int[] {0};
        }

        int first = Math.max(-range, firstLap(copies, tileMin, tileSize, spanMin));
        int last = Math.min(range, lastLap(copies, tileMin, spanMax));
        return AxisCopies.lapRange(first, last);
    }

    public static boolean regionInView(Rectangle2D.Double regionBounds, int regionX, int regionZ) {
        return regionInView(copies(Direction.Axis.X), regionX, regionBounds.getMinX(), regionBounds.getMaxX())
                && regionInView(copies(Direction.Axis.Z), regionZ, regionBounds.getMinY(), regionBounds.getMaxY());
    }

    static boolean regionInView(AxisCopies copies, int region, double boundsMin, double boundsMax) {
        if (!copies.loops()) {
            return region >= boundsMin && region < boundsMax;
        }

        int tileMin = region * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS;
        int spanMin = (int) Math.floor(boundsMin * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
        int spanMax = (int) Math.ceil(boundsMax * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
        return firstLap(copies, tileMin, FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS, spanMin)
                <= lastLap(copies, tileMin, spanMax);
    }

    private static int firstLap(AxisCopies copies, int tileMin, int tileSize, int spanMin) {
        return Math.floorDiv(spanMin - tileMin - tileSize, copies.width()) + 1;
    }

    private static int lastLap(AxisCopies copies, int tileMin, int spanMax) {
        return Math.floorDiv(spanMax - 1 - tileMin, copies.width());
    }

    public static int[] gridRegions(Direction.Axis axis, int firstRegion, int lastRegion) {
        ToroidalShape shape = ClientShapes.current();
        return foldedRegions(shape == null ? AxisCopies.UNBOUNDED : AxisCopies.ofChunks(shape, axis),
                firstRegion, lastRegion);
    }

    static int[] foldedRegions(AxisCopies chunks, int firstRegion, int lastRegion) {
        if (!chunks.loops()) {
            return IntStream.rangeClosed(firstRegion, lastRegion).toArray();
        }

        int spanChunks = (lastRegion - firstRegion + 1) * REGION_CHUNKS;
        if (spanChunks >= chunks.width()) {
            return IntStream.rangeClosed(regionOf(chunks.min()), regionOf(chunks.max() - 1)).toArray();
        }

        int start = chunks.min() + Math.floorMod(firstRegion * REGION_CHUNKS - chunks.min(), chunks.width());
        int end = start + spanChunks - 1;
        if (end < chunks.max()) {
            return IntStream.rangeClosed(regionOf(start), regionOf(end)).toArray();
        }

        return IntStream.concat(
                IntStream.rangeClosed(regionOf(chunks.min()), regionOf(end - chunks.width())),
                IntStream.rangeClosed(regionOf(start), regionOf(chunks.max() - 1))).distinct().toArray();
    }

    private static int regionOf(int chunk) {
        return Math.floorDiv(chunk, REGION_CHUNKS);
    }

    public static void recordView(Context.UI ui, double centerX, double centerZ, int tiles) {
        View view = new View(centerX, centerZ, tiles);
        if (ui == Context.UI.Fullscreen) {
            fullscreenView = view;
        } else if (ui == Context.UI.Minimap) {
            minimapView = view;
        }
    }

    public static @Nullable View viewOf(Context.UI ui) {
        return ui == Context.UI.Fullscreen ? fullscreenView : ui == Context.UI.Minimap ? minimapView : null;
    }

    public record View(double centerX, double centerZ, int tiles) {
    }

    public static void recordCopyRange(Context.UI ui, int rangeX, int rangeZ) {
        if (ui == Context.UI.Fullscreen) {
            fullscreenRangeX = rangeX;
            fullscreenRangeZ = rangeZ;
        } else if (ui == Context.UI.Minimap) {
            minimapRangeX = rangeX;
            minimapRangeZ = rangeZ;
        }
    }

    public static double[][] copyOffsets(Context.UI ui, int zoom, Rectangle2D.Double bounds, Rectangle2D.Double screen) {
        int rangeX = ui == Context.UI.Fullscreen ? fullscreenRangeX : ui == Context.UI.Minimap ? minimapRangeX : 0;
        int rangeZ = ui == Context.UI.Fullscreen ? fullscreenRangeZ : ui == Context.UI.Minimap ? minimapRangeZ : 0;
        return copyOffsets(rangeX, rangeZ, worldPixelPeriod(Direction.Axis.X, zoom),
                worldPixelPeriod(Direction.Axis.Z, zoom), bounds, screen);
    }

    static double[][] copyOffsets(int rangeX, int rangeZ, double periodX, double periodZ, Rectangle2D.Double bounds,
            Rectangle2D.Double screen) {
        int[] lapsX = visibleLaps(rangeX, periodX, bounds.getMinX(), bounds.getMaxX(), screen.getMinX(), screen.getMaxX());
        int[] lapsZ = visibleLaps(rangeZ, periodZ, bounds.getMinY(), bounds.getMaxY(), screen.getMinY(), screen.getMaxY());
        double[][] offsets = new double[lapsX.length * lapsZ.length][];
        int i = 0;
        for (int lapX : lapsX) {
            for (int lapZ : lapsZ) {
                offsets[i++] = new double[] {lapX * periodX, lapZ * periodZ};
            }
        }

        return offsets;
    }

    public static double[][] nearestCopyOffset(double[][] offsets, Rectangle2D.Double bounds, Rectangle2D.Double screen) {
        if (offsets.length <= 1) {
            return offsets;
        }

        double[] nearest = offsets[0];
        double nearestDistance = java.lang.Double.MAX_VALUE;
        for (double[] offset : offsets) {
            double dx = bounds.getCenterX() + offset[0] - screen.getCenterX();
            double dz = bounds.getCenterY() + offset[1] - screen.getCenterY();
            double distance = dx * dx + dz * dz;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = offset;
            }
        }

        return new double[][] {nearest};
    }

    private static int[] visibleLaps(int range, double period, double min, double max, double screenMin, double screenMax) {
        if (period <= 0.0) {
            return max >= screenMin && min <= screenMax ? new int[] {0} : new int[0];
        }

        int first = Math.max(-range, (int) Math.ceil((screenMin - max) / period));
        int last = Math.min(range, (int) Math.floor((screenMax - min) / period));
        return AxisCopies.lapRange(first, last);
    }

    public static Copies fullscreenCopies() {
        return MapCopyBudget.painted(copies(Direction.Axis.X), fullscreenRangeX,
                copies(Direction.Axis.Z), fullscreenRangeZ);
    }

    public static <D> @Nullable String staleGridReason(@Nullable D lastDimension, @Nullable D dimension,
            @Nullable File lastWorldDir, @Nullable File worldDir) {
        if (lastWorldDir != null && worldDir != null && !lastWorldDir.equals(worldDir)) {
            return WORLD_CHANGED;
        }

        if (lastDimension != null && !lastDimension.equals(dimension)) {
            return DIMENSION_CHANGED;
        }

        return null;
    }

    public static void gridDropped(String reason, String from, String to, int tilesDropped) {
        LOGGER.info("[jm-compat] grid_dropped reason={} from={} to={} tiles_dropped={}",
                reason, from.replace(' ', '_'), to.replace(' ', '_'), tilesDropped);
    }

    private JourneyMapFold() {
    }
}
