package com.exoticworlds.compat.journeymap;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import journeymap.api.v2.client.display.Context;

public final class JourneyMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX_TILE_BLITS = 16_384;

    public static final String WORLD_CHANGED = "world";
    public static final String DIMENSION_CHANGED = "dimension";

    private static List<WorldCopies.Copy> fullscreenCopies = List.of(WorldCopies.IDENTITY);
    private static List<WorldCopies.Copy> minimapCopies = List.of(WorldCopies.IDENTITY);
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

    public static double pixelsPerBlock(int zoom) {
        return zoom / (double) FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS;
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

    public static List<WorldCopies.Copy> drawnCopies(int gridTiles, int[] spanX, int[] spanZ, MapCopies copies) {
        return drawnCopies(ClientShapes.current(), gridTiles, spanX, spanZ, copies);
    }

    static List<WorldCopies.Copy> drawnCopies(@Nullable ToroidalShape shape, int gridTiles, int[] spanX, int[] spanZ,
            MapCopies copies) {
        if (copies == MapCopies.SINGLE) {
            return List.of(WorldCopies.IDENTITY);
        }

        List<WorldCopies.Copy> meeting = WorldCopies.meeting(shape, spanX[0], spanZ[0], spanX[1], spanZ[1]);
        int budget = Math.max(1, MAX_TILE_BLITS / Math.max(1, gridTiles));
        if (meeting.size() <= budget) {
            return meeting;
        }

        double offsetX = (spanX[0] + spanX[1]) / 2.0 - worldCenter(shape, Direction.Axis.X);
        double offsetZ = (spanZ[0] + spanZ[1]) / 2.0 - worldCenter(shape, Direction.Axis.Z);
        return meeting.stream()
                .sorted(Comparator.comparingDouble(copy -> copy.isIdentity()
                        ? -1.0
                        : (copy.dx() - offsetX) * (copy.dx() - offsetX) + (copy.dz() - offsetZ) * (copy.dz() - offsetZ)))
                .limit(budget)
                .toList();
    }

    private static double worldCenter(@Nullable ToroidalShape shape, Direction.Axis axis) {
        AxisCopies copies = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
        return copies.loops() ? (copies.min() + copies.max()) / 2.0 : 0.0;
    }

    public static List<WorldCopies.Copy> tileCopies(List<WorldCopies.Copy> drawn, int tileMinX, int tileMinZ,
            int tileSize, int[] spanX, int[] spanZ) {
        List<WorldCopies.Copy> copies = new ArrayList<>();
        for (WorldCopies.Copy copy : drawn) {
            int minX = tileMinX + copy.dx();
            int minZ = tileMinZ + copy.dz();
            if (!copy.isIdentity() && minX + tileSize > spanX[0] && minX < spanX[1]
                    && minZ + tileSize > spanZ[0] && minZ < spanZ[1]) {
                copies.add(copy);
            }
        }

        return copies;
    }

    public static boolean regionInView(Rectangle2D.Double regionBounds, int regionX, int regionZ) {
        return regionInView(ClientShapes.current(), regionX, regionZ,
                regionBounds.getMinX(), regionBounds.getMinY(), regionBounds.getMaxX(), regionBounds.getMaxY());
    }

    static boolean regionInView(@Nullable ToroidalShape shape, int regionX, int regionZ, double boundsMinX,
            double boundsMinZ, double boundsMaxX, double boundsMaxZ) {
        AxisCopies x = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.Z);
        List<WorldCopies.Copy> copies = WorldCopies.meeting(shape, spanMin(boundsMinX), spanMin(boundsMinZ),
                spanMax(boundsMaxX), spanMax(boundsMaxZ));
        for (WorldCopies.Copy copy : copies) {
            if (regionMeets(x, regionX, copy.dx(), boundsMinX, boundsMaxX)
                    && regionMeets(z, regionZ, copy.dz(), boundsMinZ, boundsMaxZ)) {
                return true;
            }
        }

        return false;
    }

    private static boolean regionMeets(AxisCopies axis, int region, int move, double boundsMin, double boundsMax) {
        if (!axis.loops()) {
            return region >= boundsMin && region < boundsMax;
        }

        int tileMin = regionBlock(region) + move;
        return tileMin + FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS > spanMin(boundsMin) && tileMin < spanMax(boundsMax);
    }

    private static int spanMin(double regionBound) {
        return (int) Math.floor(regionBound * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
    }

    private static int spanMax(double regionBound) {
        return (int) Math.ceil(regionBound * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
    }

    public static long[] gridRegions(int firstRegionX, int firstRegionZ, int lastRegionX, int lastRegionZ) {
        return gridRegions(ClientShapes.current(), firstRegionX, firstRegionZ, lastRegionX, lastRegionZ);
    }

    static long[] gridRegions(@Nullable ToroidalShape shape, int firstRegionX, int firstRegionZ, int lastRegionX,
            int lastRegionZ) {
        LongLinkedOpenHashSet regions = new LongLinkedOpenHashSet();
        for (WorldCopies.Piece piece : WorldCopies.pieces(shape, regionBlock(firstRegionX), regionBlock(firstRegionZ),
                regionBlock(lastRegionX + 1), regionBlock(lastRegionZ + 1))) {
            for (int x = regionOf(piece.minX()); x <= regionOf(piece.maxX() - 1); x++) {
                for (int z = regionOf(piece.minZ()); z <= regionOf(piece.maxZ() - 1); z++) {
                    regions.add(ChunkPos.pack(x, z));
                }
            }
        }

        return regions.toLongArray();
    }

    private static int regionBlock(int region) {
        return region * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS;
    }

    private static int regionOf(int block) {
        return Math.floorDiv(block, FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
    }

    public static List<WorldCopies.Edge> seams(int[] spanX, int[] spanZ) {
        return WorldCopies.seams(ClientShapes.current(), spanX[0], spanZ[0], spanX[1], spanZ[1]);
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

    public static void recordCopies(Context.UI ui, List<WorldCopies.Copy> copies) {
        if (ui == Context.UI.Fullscreen) {
            fullscreenCopies = copies;
        } else if (ui == Context.UI.Minimap) {
            minimapCopies = copies;
        }
    }

    public static double[][] copyOffsets(Context.UI ui, int zoom, Rectangle2D.Double bounds, Rectangle2D.Double screen) {
        List<WorldCopies.Copy> copies = ui == Context.UI.Fullscreen ? fullscreenCopies
                : ui == Context.UI.Minimap ? minimapCopies : List.of(WorldCopies.IDENTITY);
        return offsetsOnScreen(copies, pixelsPerBlock(zoom), bounds, screen);
    }

    static double[][] offsetsOnScreen(List<WorldCopies.Copy> copies, double pixelsPerBlock, Rectangle2D.Double bounds,
            Rectangle2D.Double screen) {
        List<double[]> offsets = new ArrayList<>(copies.size());
        for (WorldCopies.Copy copy : copies) {
            double offsetX = copy.dx() * pixelsPerBlock;
            double offsetZ = copy.dz() * pixelsPerBlock;
            if (bounds.getMaxX() + offsetX >= screen.getMinX() && bounds.getMinX() + offsetX <= screen.getMaxX()
                    && bounds.getMaxY() + offsetZ >= screen.getMinY() && bounds.getMinY() + offsetZ <= screen.getMaxY()) {
                offsets.add(new double[] {offsetX, offsetZ});
            }
        }

        return offsets.toArray(double[][]::new);
    }

    public static double[][] nearestCopyOffset(double[][] offsets, Rectangle2D.Double bounds, Rectangle2D.Double screen) {
        if (offsets.length <= 1) {
            return offsets;
        }

        double[] nearest = offsets[0];
        double nearestDistance = Double.MAX_VALUE;
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
