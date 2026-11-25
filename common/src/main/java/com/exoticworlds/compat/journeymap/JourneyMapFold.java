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

import com.exoticworlds.engine.client.ClientFrame;
import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.FullscreenZoomFloor;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.MapViewSpan;
import com.exoticworlds.compat.map.WorldCopies;
import com.exoticworlds.core.fold.WorldFold;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import journeymap.api.v2.common.Context;

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
        return foldRegionChunk(ClientFolds.getCurrent(), chunk);
    }

    static ChunkPos foldRegionChunk(@Nullable WorldFold fold, ChunkPos chunk) {
        return ClientFolds.fold(fold, chunk);
    }

    public static Vec3 seatPixel(double refX, double refZ, double blockX, double blockZ, MapCopies copies) {
        return seatPixel(ClientFolds.getCurrent(), refX, refZ, blockX, blockZ, copies);
    }

    static Vec3 seatPixel(@Nullable WorldFold fold, double refX, double refZ, double blockX, double blockZ,
            MapCopies copies) {
        Vec3 pixel = new Vec3(blockX, 0.0, blockZ);
        if (fold == null) {
            return pixel;
        }

        return copies == MapCopies.SINGLE ? fold.fold(pixel) : fold.nearestCopy(new Vec3(refX, 0.0, refZ), pixel);
    }

    public static MapCopies getCopies(Context.UI ui) {
        return ui == Context.UI.Fullscreen ? MapCopies.getCurrent() : MapCopies.REPEATED;
    }

    public static double clampedMove(Direction.Axis axis, double center, double delta, int zoom, int windowPixels) {
        return clampedMove(axisCopies(axis), center, delta, halfViewBlocks(zoom, windowPixels));
    }

    static double clampedMove(AxisCopies copies, double center, double delta, double halfSpanBlocks) {
        double target = center + delta;
        double clamped = copies.clampView(target, halfSpanBlocks);
        return clamped == target ? delta : clamped - center;
    }

    public static double moveDelta(Context.UI ui, Direction.Axis axis, double center, double delta, int zoom) {
        if (getCopies(ui) != MapCopies.SINGLE) {
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
        Vec3 folded = ClientFolds.fold(ClientFolds.getCurrent(), blockX, blockZ);
        if (getCopies(ui) != MapCopies.SINGLE || !isActive()) {
            return new Center(folded.x, folded.z, zoom);
        }

        Window window = Minecraft.getInstance().getWindow();
        return singleCenter(ClientFolds.getCurrent(), folded, Math.max(zoom, fullscreenZoomFloor()), window.getWidth(),
                window.getHeight());
    }

    static Center singleCenter(@Nullable WorldFold fold, Vec3 folded, int flooredZoom, int windowWidthPixels,
            int windowHeightPixels) {
        return new Center(clampSingleCenter(fold, Direction.Axis.X, folded.x, flooredZoom, windowWidthPixels),
                clampSingleCenter(fold, Direction.Axis.Z, folded.z, flooredZoom, windowHeightPixels), flooredZoom);
    }

    public record Center(double x, double z, int zoom) {
    }

    static double clampSingleCenter(@Nullable WorldFold fold, Direction.Axis axis, double foldedCoord, int zoom,
            int windowPixels) {
        return AxisCopies.of(fold, axis).clampView(foldedCoord, halfViewBlocks(zoom, windowPixels));
    }

    private static double halfViewBlocks(int zoom, int windowPixels) {
        return MapViewSpan.halfBlocks(windowPixels, pixelsPerBlock(Math.max(1, zoom)));
    }

    public static Vec3 nearestToPlayer(Vec3 position) {
        return ClientFrame.nearestToPlayer(position);
    }

    public static boolean isActive() {
        return ClientFolds.getCurrent() != null;
    }

    public static @Nullable BlockPos foldUiBlock(@Nullable BlockPos pos) {
        return foldUiBlock(ClientFolds.getCurrent(), pos);
    }

    static @Nullable BlockPos foldUiBlock(@Nullable WorldFold fold, @Nullable BlockPos pos) {
        return pos == null ? null : ClientFolds.fold(fold, pos);
    }

    public static AxisCopies axisCopies(Direction.Axis axis) {
        return AxisCopies.of(ClientFolds.getCurrent(), axis);
    }

    public static double pixelsPerBlock(int zoom) {
        return zoom / (double) FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS;
    }

    public static int loopedAxes() {
        WorldFold fold = ClientFolds.getCurrent();
        if (fold == null) {
            return 0;
        }

        return (fold.isLooped(Direction.Axis.X) ? 1 : 0) + (fold.isLooped(Direction.Axis.Z) ? 1 : 0);
    }

    public static int zoomFloor() {
        WorldFold fold = ClientFolds.getCurrent();
        return fold == null ? 0 : FullscreenZoomFloor.journeyMapZoom(fold);
    }

    public static int fullscreenZoomFloor() {
        WorldFold fold = ClientFolds.getCurrent();
        if (fold == null) {
            return 0;
        }

        if (MapCopies.getCurrent() != MapCopies.SINGLE) {
            return FullscreenZoomFloor.journeyMapZoom(fold);
        }

        Window window = Minecraft.getInstance().getWindow();
        return FullscreenZoomFloor.journeyMapCoverZoom(fold, window.getWidth(), window.getHeight());
    }

    public static int seatFullscreenZoom(int zoom) {
        return FullscreenZoomFloor.journeyMapSeatedZoom(zoom, fullscreenZoomFloor(), MapCopies.getCurrent());
    }

    public static int[] viewSpanBlocks(double centerBlock, int windowPixels, int zoom) {
        return MapViewSpan.of(centerBlock, windowPixels, pixelsPerBlock(Math.max(1, zoom)), 0);
    }

    public static List<WorldCopies.Copy> drawnCopies(int gridTiles, int[] spanX, int[] spanZ, MapCopies copies) {
        return drawnCopies(ClientFolds.getCurrent(), gridTiles, spanX, spanZ, copies);
    }

    static List<WorldCopies.Copy> drawnCopies(@Nullable WorldFold fold, int gridTiles, int[] spanX, int[] spanZ,
            MapCopies copies) {
        if (copies == MapCopies.SINGLE) {
            return List.of(WorldCopies.IDENTITY);
        }

        List<WorldCopies.Copy> meeting = WorldCopies.meeting(fold, spanX[0], spanZ[0], spanX[1], spanZ[1]);
        int budget = Math.max(1, MAX_TILE_BLITS / Math.max(1, gridTiles));
        if (meeting.size() <= budget) {
            return meeting;
        }

        double offsetX = (spanX[0] + spanX[1]) / 2.0 - worldCenter(fold, Direction.Axis.X);
        double offsetZ = (spanZ[0] + spanZ[1]) / 2.0 - worldCenter(fold, Direction.Axis.Z);
        return meeting.stream()
                .sorted(Comparator.comparingDouble(copy -> copy.isIdentity()
                        ? -1.0
                        : (copy.dx() - offsetX) * (copy.dx() - offsetX) + (copy.dz() - offsetZ) * (copy.dz() - offsetZ)))
                .limit(budget)
                .toList();
    }

    private static double worldCenter(@Nullable WorldFold fold, Direction.Axis axis) {
        AxisCopies copies = AxisCopies.of(fold, axis);
        return copies.isLooped() ? (copies.getMin() + copies.getMax()) / 2.0 : 0.0;
    }

    public static List<WorldCopies.Copy> tileCopies(List<WorldCopies.Copy> drawn, int tileMinX, int tileMinZ,
            int tileSizeBlocks, int[] spanX, int[] spanZ) {
        List<WorldCopies.Copy> copies = new ArrayList<>();
        for (WorldCopies.Copy copy : drawn) {
            int minX = tileMinX + copy.dx();
            int minZ = tileMinZ + copy.dz();
            if (!copy.isIdentity() && minX + tileSizeBlocks > spanX[0] && minX < spanX[1]
                    && minZ + tileSizeBlocks > spanZ[0] && minZ < spanZ[1]) {
                copies.add(copy);
            }
        }

        return copies;
    }

    public static boolean isRegionInView(Rectangle2D regionBounds, int regionX, int regionZ) {
        return isRegionInView(ClientFolds.getCurrent(), regionBounds, regionX, regionZ);
    }

    static boolean isRegionInView(@Nullable WorldFold fold, Rectangle2D regionBounds, int regionX, int regionZ) {
        double boundsMinX = regionBounds.getMinX();
        double boundsMinZ = regionBounds.getMinY();
        double boundsMaxX = regionBounds.getMaxX();
        double boundsMaxZ = regionBounds.getMaxY();
        AxisCopies x = AxisCopies.of(fold, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(fold, Direction.Axis.Z);
        List<WorldCopies.Copy> copies = WorldCopies.meeting(fold, spanMin(boundsMinX), spanMin(boundsMinZ),
                spanMax(boundsMaxX), spanMax(boundsMaxZ));
        for (WorldCopies.Copy copy : copies) {
            if (isRegionMeeting(x, regionX, copy.dx(), boundsMinX, boundsMaxX)
                    && isRegionMeeting(z, regionZ, copy.dz(), boundsMinZ, boundsMaxZ)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isRegionMeeting(AxisCopies axis, int region, int move, double boundsMin, double boundsMax) {
        if (!axis.isLooped()) {
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
        return gridRegions(ClientFolds.getCurrent(), firstRegionX, firstRegionZ, lastRegionX, lastRegionZ);
    }

    static long[] gridRegions(@Nullable WorldFold fold, int firstRegionX, int firstRegionZ, int lastRegionX,
            int lastRegionZ) {
        return WorldCopies.Window.of(fold, regionBlock(firstRegionX), regionBlock(firstRegionZ),
                regionBlock(lastRegionX + 1), regionBlock(lastRegionZ + 1))
                .canonicalCells(FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS);
    }

    public static long[] farSideRegions(Set<String> imageFiles, int centerRegionX, int centerRegionZ, int gridSize) {
        return farSideRegions(ClientFolds.getCurrent(), imageFiles, centerRegionX, centerRegionZ, gridSize);
    }

    static long[] farSideRegions(@Nullable WorldFold fold, Set<String> imageFiles, int centerRegionX,
            int centerRegionZ, int gridSize) {
        if (fold == null) {
            return new long[0];
        }

        int regionCount = gridSize / 2;
        int minX = centerRegionX - regionCount;
        int maxX = centerRegionX + regionCount;
        int minZ = centerRegionZ - regionCount;
        int maxZ = centerRegionZ + regionCount;
        return Arrays.stream(gridRegions(fold, minX, minZ, maxX, maxZ)).filter(region -> {
            int x = ChunkPos.getX(region);
            int z = ChunkPos.getZ(region);
            boolean isWalkedByJourneyMap = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
            return !isWalkedByJourneyMap && imageFiles.contains(x + "," + z + ".png");
        }).toArray();
    }

    private static int regionBlock(int region) {
        return region * FullscreenZoomFloor.JOURNEYMAP_REGION_BLOCKS;
    }

    public static List<WorldCopies.Edge> fullscreenSeams(Context.UI ui, double centerX, double centerZ, int zoom) {
        if (ui != Context.UI.Fullscreen || loopedAxes() == 0
                || !getCopies(Context.UI.Fullscreen).isDrawingSeamLines()) {
            return List.of();
        }

        Window window = Minecraft.getInstance().getWindow();
        return seams(viewSpanBlocks(centerX, window.getWidth(), zoom), viewSpanBlocks(centerZ, window.getHeight(), zoom));
    }

    public static List<WorldCopies.Edge> seams(int[] spanX, int[] spanZ) {
        return WorldCopies.seams(ClientFolds.getCurrent(), spanX[0], spanZ[0], spanX[1], spanZ[1]);
    }

    public static void recordView(Context.UI ui, double centerX, double centerZ, int tiles) {
        View view = new View(centerX, centerZ, tiles);
        if (ui == Context.UI.Fullscreen) {
            fullscreenView = view;
        } else if (ui == Context.UI.Minimap) {
            minimapView = view;
        }
    }

    public static @Nullable View getView(Context.UI ui) {
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
        double nearestSqrDistance = Double.MAX_VALUE;
        for (double[] offset : offsets) {
            double dx = bounds.getCenterX() + offset[0] - screen.getCenterX();
            double dz = bounds.getCenterY() + offset[1] - screen.getCenterY();
            double sqrDistance = dx * dx + dz * dz;
            if (sqrDistance < nearestSqrDistance) {
                nearestSqrDistance = sqrDistance;
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

    public static boolean shouldDropGrid(@Nullable String lastDimension, String dimension, @Nullable File lastWorldDir,
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
