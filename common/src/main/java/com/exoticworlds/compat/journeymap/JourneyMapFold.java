package com.exoticworlds.compat.journeymap;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.client.engine.ClientFrame;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.WorldCopies;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;

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

    public static ChunkPos foldRegionChunk(ChunkPos chunk) {
        return foldRegionChunk(ClientShapes.current(), chunk);
    }

    static ChunkPos foldRegionChunk(@Nullable ToroidalShape shape, ChunkPos chunk) {
        return ClientShapes.fold(shape, chunk);
    }

    public static Vec3 foldCenter(double blockX, double blockZ) {
        return foldCenter(ClientShapes.current(), blockX, blockZ);
    }

    static Vec3 foldCenter(@Nullable ToroidalShape shape, double blockX, double blockZ) {
        return ClientShapes.fold(shape, new Vec3(blockX, 0.0, blockZ));
    }

    public static Vec3 seatPixel(double refX, double refZ, double blockX, double blockZ, MapCopies copies) {
        return seatPixel(ClientShapes.current(), refX, refZ, blockX, blockZ, copies);
    }

    static Vec3 seatPixel(@Nullable ToroidalShape shape, double refX, double refZ, double blockX, double blockZ,
            MapCopies copies) {
        Vec3 pixel = new Vec3(blockX, 0.0, blockZ);
        if (shape == null) {
            return pixel;
        }

        return copies == MapCopies.SINGLE ? shape.fold(pixel) : shape.nearestCopy(new Vec3(refX, 0.0, refZ), pixel);
    }

    public static MapCopies copiesOf(Context.UI ui) {
        return ui == Context.UI.Fullscreen ? MapCopies.current() : MapCopies.REPEATED;
    }

    public static double clampedMove(Direction.Axis axis, double center, double delta, int zoom, int windowPixels) {
        return copies(axis).clampView(center + delta, halfViewBlocks(zoom, windowPixels)) - center;
    }

    public static double moveDelta(Context.UI ui, Direction.Axis axis, double center, double delta, int zoom) {
        if (copiesOf(ui) != MapCopies.SINGLE) {
            return delta;
        }

        Window window = Minecraft.getInstance().getWindow();
        return clampedMove(axis, center, delta, zoom,
                axis == Direction.Axis.X ? window.getWidth() : window.getHeight());
    }

    public static double flooredZoom(Context.UI ui, double zoom) {
        return ui == Context.UI.Fullscreen ? Math.max(zoom, fullscreenZoomFloor()) : zoom;
    }

    public static Center centerView(Context.UI ui, double blockX, double blockZ, int zoom) {
        Vec3 folded = foldCenter(blockX, blockZ);
        if (copiesOf(ui) != MapCopies.SINGLE || !active()) {
            return new Center(folded.x, folded.z, zoom);
        }

        Window window = Minecraft.getInstance().getWindow();
        return singleCenter(ClientShapes.current(), folded, Math.max(zoom, fullscreenZoomFloor()), window.getWidth(),
                window.getHeight());
    }

    static Center singleCenter(@Nullable ToroidalShape shape, Vec3 folded, int flooredZoom, int windowWidth,
            int windowHeight) {
        return new Center(clampSingleCenter(shape, Direction.Axis.X, folded.x, flooredZoom, windowWidth),
                clampSingleCenter(shape, Direction.Axis.Z, folded.z, flooredZoom, windowHeight), flooredZoom);
    }

    public record Center(double x, double z, int zoom) {
    }

    static double clampSingleCenter(@Nullable ToroidalShape shape, Direction.Axis axis, double foldedCoord, int zoom,
            int windowPixels) {
        return AxisCopies.of(shape, axis).clampView(foldedCoord, halfViewBlocks(zoom, windowPixels));
    }

    private static double halfViewBlocks(int zoom, int windowPixels) {
        return MapViewSpan.halfBlocks(windowPixels, pixelsPerBlock(Math.max(1, zoom)));
    }

    public static Vec3 nearestToPlayer(Vec3 position) {
        return ClientFrame.nearestToPlayer(position);
    }

    public static boolean active() {
        return ClientShapes.current() != null;
    }

    public static @Nullable BlockPos foldUiBlock(@Nullable BlockPos pos) {
        return foldUiBlock(ClientShapes.current(), pos);
    }

    static @Nullable BlockPos foldUiBlock(@Nullable ToroidalShape shape, @Nullable BlockPos pos) {
        return pos == null ? null : ClientShapes.fold(shape, pos);
    }

    public static AxisCopies copies(Direction.Axis axis) {
        return AxisCopies.of(ClientShapes.current(), axis);
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
        return MapViewSpan.of(centerBlock, windowPixels, pixelsPerBlock(Math.max(1, zoom)), 0);
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
        AxisCopies copies = AxisCopies.of(shape, axis);
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

    public static boolean regionInView(Rectangle2D regionBounds, int regionX, int regionZ) {
        return regionInView(ClientShapes.current(), regionBounds, regionX, regionZ);
    }

    static boolean regionInView(@Nullable ToroidalShape shape, Rectangle2D regionBounds, int regionX, int regionZ) {
        double boundsMinX = regionBounds.getMinX();
        double boundsMinZ = regionBounds.getMinY();
        double boundsMaxX = regionBounds.getMaxX();
        double boundsMaxZ = regionBounds.getMaxY();
        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
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
        return WorldCopies.Window.of(shape, regionBlock(firstRegionX), regionBlock(firstRegionZ),
                regionBlock(lastRegionX + 1), regionBlock(lastRegionZ + 1))
                .canonicalCells(FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
    }

    public static long[] farSideRegions(Set<String> imageFiles, int centerRegionX, int centerRegionZ, int gridSize) {
        return farSideRegions(ClientShapes.current(), imageFiles, centerRegionX, centerRegionZ, gridSize);
    }

    static long[] farSideRegions(@Nullable ToroidalShape shape, Set<String> imageFiles, int centerRegionX,
            int centerRegionZ, int gridSize) {
        if (shape == null) {
            return new long[0];
        }

        int regionCount = gridSize / 2;
        int minX = centerRegionX - regionCount;
        int maxX = centerRegionX + regionCount;
        int minZ = centerRegionZ - regionCount;
        int maxZ = centerRegionZ + regionCount;
        return Arrays.stream(gridRegions(shape, minX, minZ, maxX, maxZ)).filter(region -> {
            int x = ChunkPos.getX(region);
            int z = ChunkPos.getZ(region);
            boolean walkedByJourneyMap = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            return !walkedByJourneyMap && imageFiles.contains(x + "," + z + ".png");
        }).toArray();
    }

    private static int regionBlock(int region) {
        return region * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS;
    }

    public static List<WorldCopies.Edge> fullscreenSeams(Context.UI ui, double centerX, double centerZ, int zoom) {
        if (ui != Context.UI.Fullscreen || loopedAxes() == 0 || !copiesOf(Context.UI.Fullscreen).drawsSeamLines()) {
            return List.of();
        }

        Window window = Minecraft.getInstance().getWindow();
        return seams(viewSpan(centerX, window.getWidth(), zoom), viewSpan(centerZ, window.getHeight(), zoom));
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

    // mc/1.21: places an overlay that draws once on its copy nearest the screen center.
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

    public static boolean dropsGrid(@Nullable String lastDimension, String dimension, @Nullable File lastWorldDir,
            @Nullable File worldDir, int tiles) {
        String reason = staleGridReason(lastDimension, dimension, lastWorldDir, worldDir);
        if (WORLD_CHANGED.equals(reason) && lastWorldDir != null && worldDir != null) {
            gridDropped(WORLD_CHANGED, lastWorldDir.getName(), worldDir.getName(), tiles);
            return true;
        }

        if (DIMENSION_CHANGED.equals(reason) && lastDimension != null) {
            gridDropped(DIMENSION_CHANGED, lastDimension, dimension, tiles);
            return true;
        }

        return false;
    }

    private static void gridDropped(String reason, String from, String to, int tilesDropped) {
        LOGGER.info("[ew/jm-compat] grid_dropped reason={} from={} to={} tiles_dropped={}",
                reason, from.replace(' ', '_'), to.replace(' ', '_'), tilesDropped);
    }

    private JourneyMapFold() {
    }
}
