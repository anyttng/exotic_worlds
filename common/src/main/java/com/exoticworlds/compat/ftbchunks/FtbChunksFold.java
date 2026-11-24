package com.exoticworlds.compat.ftbchunks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.FullscreenZoomFloor;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.compat.map.WorldCopies;
import com.exoticworlds.core.fold.RectangleCopies;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftblibrary.math.XZ;

public final class FtbChunksFold {
    public static final int REGION_BLOCKS = 512;

    static final int REGION_CHUNKS = 32;
    private static final int CHUNK_BLOCKS = 16;
    private static final int TILE_PIXELS_PER_ZOOM = 2;

    private static final int MINIMAP_CHUNKS = 15;
    private static final int MINIMAP_CENTRE_CHUNK = 7;

    public static XZ chunkOf(int chunkX, int chunkZ) {
        ChunkPos folded = foldedChunk(ClientFolds.getCurrent(), chunkX, chunkZ);
        return XZ.of(folded.x(), folded.z());
    }

    public static XZ chunkOf(XZ chunk) {
        return foldedKey(ClientFolds.getCurrent(), chunk);
    }

    public static XZ chunkOf(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        ChunkPos folded = foldedChunk(ClientFolds.of(dimension), chunkX, chunkZ);
        return XZ.of(folded.x(), folded.z());
    }

    public static XZ chunkOf(ResourceKey<Level> dimension, XZ chunk) {
        return foldedKey(ClientFolds.of(dimension), chunk);
    }

    static XZ foldedKey(@Nullable WorldFold fold, XZ chunk) {
        if (fold == null) {
            return chunk;
        }

        ChunkPos folded = fold.fold(new ChunkPos(chunk.x(), chunk.z()));
        return folded.x() == chunk.x() && folded.z() == chunk.z() ? chunk : XZ.of(folded.x(), folded.z());
    }

    public static ChunkPos foldedChunkPos(ResourceKey<Level> dimension, ChunkPos pos) {
        return ClientFolds.fold(ClientFolds.of(dimension), pos);
    }

    public static Set<XZ> foldedChunks(Set<XZ> chunks) {
        return foldedChunks(ClientFolds.getCurrent(), chunks);
    }

    static Set<XZ> foldedChunks(@Nullable WorldFold fold, Set<XZ> chunks) {
        if (fold == null) {
            return chunks;
        }

        Set<XZ> folded = new HashSet<>(chunks.size());
        for (XZ chunk : chunks) {
            folded.add(foldedKey(fold, chunk));
        }

        return folded;
    }

    public static BlockPos foldBlock(int x, int z) {
        return foldBlock(ClientFolds.getCurrent(), x, z);
    }

    public static BlockPos foldBlock(ResourceKey<Level> dimension, int x, int z) {
        return foldBlock(ClientFolds.of(dimension), x, z);
    }

    private static BlockPos foldBlock(@Nullable WorldFold fold, int x, int z) {
        return ClientFolds.fold(fold, new BlockPos(x, 0, z));
    }

    public static Vec3 foldPosition(ResourceKey<Level> dimension, Vec3 position) {
        return ClientFolds.fold(ClientFolds.of(dimension), position);
    }

    static ChunkPos foldedChunk(@Nullable WorldFold fold, int chunkX, int chunkZ) {
        return ClientFolds.fold(fold, new ChunkPos(chunkX, chunkZ));
    }

    public static int loopedAxes(ResourceKey<Level> dimension) {
        WorldFold fold = ClientFolds.of(dimension);
        if (fold == null) {
            return 0;
        }

        return (fold.isLooped(Direction.Axis.X) ? 1 : 0) + (fold.isLooped(Direction.Axis.Z) ? 1 : 0);
    }

    public static List<WorldCopies.Copy> drawnCopies(ResourceKey<Level> dimension, int minX, int minZ, int maxX,
            int maxZ, MapCopies mapCopies) {
        return drawnCopies(ClientFolds.of(dimension), minX, minZ, maxX, maxZ, mapCopies);
    }

    static List<WorldCopies.Copy> drawnCopies(@Nullable WorldFold fold, int minX, int minZ, int maxX, int maxZ,
            MapCopies mapCopies) {
        List<WorldCopies.Copy> copies = WorldCopies.meeting(fold, minX, minZ, maxX, maxZ);
        if (mapCopies != MapCopies.SINGLE) {
            return copies;
        }

        return copies.contains(WorldCopies.IDENTITY) ? List.of(WorldCopies.IDENTITY) : List.of();
    }

    public static List<SeamLine> seamLines(ResourceKey<Level> dimension, SeamView view) {
        return seamLines(ClientFolds.of(dimension), view);
    }

    static List<SeamLine> seamLines(@Nullable WorldFold fold, SeamView view) {
        double pixelsPerBlock = view.tilePixels() / (double) REGION_BLOCKS;
        int minX = (int) Math.floor((view.x() - view.originX()) / pixelsPerBlock);
        int maxX = (int) Math.ceil((view.x() + view.width() - view.originX()) / pixelsPerBlock);
        int minZ = (int) Math.floor((view.y() - view.originY()) / pixelsPerBlock);
        int maxZ = (int) Math.ceil((view.y() + view.height() - view.originY()) / pixelsPerBlock);
        List<SeamLine> lines = new ArrayList<>();
        for (WorldCopies.Edge edge : WorldCopies.seams(fold, minX, minZ, maxX, maxZ)) {
            lines.add(SeamLine.of(edge, view.originX(), view.originY(), pixelsPerBlock));
        }

        return lines;
    }

    public static double clampScroll(ResourceKey<Level> dimension, Direction.Axis axis, double scroll, int regionMin,
            int regionTilePixels, int panelPixels) {
        return clampScroll(copies(dimension, axis), scroll, regionMin, regionTilePixels, panelPixels);
    }

    static double clampScroll(AxisCopies copies, double scroll, int regionMin, int regionTilePixels, int panelPixels) {
        double pixelsPerBlock = regionTilePixels / (double) REGION_BLOCKS;
        double centre = regionMin * (double) REGION_BLOCKS + (scroll + panelPixels / 2.0) / pixelsPerBlock;
        double clamped = copies.clampView(centre, panelPixels / 2.0 / pixelsPerBlock);
        return scroll + (clamped - centre) * pixelsPerBlock;
    }

    public static int tileFloor(ResourceKey<Level> dimension, int windowWidth, int windowHeight) {
        return zoomFloor(dimension, windowWidth, windowHeight) * TILE_PIXELS_PER_ZOOM;
    }

    public static int zoomFloor(ResourceKey<Level> dimension, int windowWidth, int windowHeight) {
        WorldFold fold = ClientFolds.of(dimension);
        if (fold == null) {
            return 0;
        }

        return MapCopies.getCurrent() == MapCopies.SINGLE
                ? FullscreenZoomFloor.ftbChunksCoverZoom(fold, windowWidth, windowHeight)
                : FullscreenZoomFloor.ftbChunksZoom(fold);
    }

    public record TileBlit(int x, int y, int width, int height, int u, int v, int srcWidth, int srcHeight) {
    }

    public record SeamView(int originX, int originY, int tilePixels, int x, int y, int width, int height) {
    }

    record HaloPixel(int sourceX, int sourceZ, int haloX, int haloZ) {
    }

    public static @Nullable TileBlit worldPartOf(ResourceKey<Level> dimension, int regionX, int regionZ, int x, int y,
            int width, int height) {
        WorldFold fold = ClientFolds.of(dimension);
        int[] spanX = worldSpanInRegion(fold, Direction.Axis.X, regionX);
        int[] spanZ = worldSpanInRegion(fold, Direction.Axis.Z, regionZ);
        if (spanX[0] >= spanX[1] || spanZ[0] >= spanZ[1]) {
            return null;
        }

        double pixelsPerBlockX = width / (double) REGION_BLOCKS;
        double pixelsPerBlockY = height / (double) REGION_BLOCKS;
        return new TileBlit(
                x + (int) Math.floor(spanX[0] * pixelsPerBlockX),
                y + (int) Math.floor(spanZ[0] * pixelsPerBlockY),
                (int) Math.ceil((spanX[1] - spanX[0]) * pixelsPerBlockX),
                (int) Math.ceil((spanZ[1] - spanZ[0]) * pixelsPerBlockY),
                spanX[0], spanZ[0], spanX[1] - spanX[0], spanZ[1] - spanZ[0]);
    }

    static int[] worldSpanInRegion(@Nullable WorldFold fold, Direction.Axis axis, int region) {
        int regionStart = region * REGION_BLOCKS;
        if (fold == null || !fold.isLooped(axis)) {
            return new int[] {0, REGION_BLOCKS};
        }

        int from = Math.max(fold.getMinBlock(axis), regionStart) - regionStart;
        int to = Math.min(fold.getMaxBlock(axis), regionStart + REGION_BLOCKS) - regionStart;
        return new int[] {from, Math.max(from, to)};
    }

    public static void mirrorSeamEdges(MapDimension dimension, int foldedChunkX, int foldedChunkZ) {
        List<HaloPixel> halo = haloPixels(ClientFolds.of(dimension.dimension), foldedChunkX, foldedChunkZ);
        if (halo.isEmpty()) {
            return;
        }

        MapRegionData source = dimension.getRegion(XZ.regionFromChunk(foldedChunkX, foldedChunkZ)).getDataBlocking();
        Map<XZ, MapRegion> touched = new HashMap<>();
        Set<MapRegion> changed = new HashSet<>();
        for (HaloPixel pixel : halo) {
            MapRegion target = touched.computeIfAbsent(XZ.regionFromBlock(pixel.haloX(), pixel.haloZ()),
                    dimension::getRegion);
            int from = regionIndex(pixel.sourceX(), pixel.sourceZ());
            int to = regionIndex(pixel.haloX(), pixel.haloZ());
            MapRegionData targetData = target.getDataBlocking();
            if (!samePixel(source, from, targetData, to)) {
                copyPixel(source, from, targetData, to);
                changed.add(target);
            }
        }

        for (MapRegion region : changed) {
            region.update(true);
        }
    }

    static List<HaloPixel> haloPixels(@Nullable WorldFold fold, int foldedChunkX, int foldedChunkZ) {
        if (fold == null) {
            return List.of();
        }

        int chunkMinX = foldedChunkX * CHUNK_BLOCKS;
        int chunkMinZ = foldedChunkZ * CHUNK_BLOCKS;
        AABB ring = new AABB(ringMin(fold, Direction.Axis.X, chunkMinX), 0.0, ringMin(fold, Direction.Axis.Z, chunkMinZ),
                ringMax(fold, Direction.Axis.X, chunkMinX), 1.0, ringMax(fold, Direction.Axis.Z, chunkMinZ));
        List<HaloPixel> halo = new ArrayList<>();
        for (int z = chunkMinZ; z < chunkMinZ + CHUNK_BLOCKS; z++) {
            for (int x = chunkMinX; x < chunkMinX + CHUNK_BLOCKS; x++) {
                if (!onWorldEdge(fold, Direction.Axis.X, x) && !onWorldEdge(fold, Direction.Axis.Z, z)) {
                    continue;
                }

                BlockPos pixel = new BlockPos(x, 0, z);
                for (WorldFold.Folded<BlockPos> copy : RectangleCopies.inside(fold, ring, pixel, WorldFold.Folded::new)) {
                    if (!copy.value().equals(pixel)) {
                        halo.add(new HaloPixel(x, z, copy.value().getX(), copy.value().getZ()));
                    }
                }
            }
        }

        return halo;
    }

    private static int ringMin(WorldFold fold, Direction.Axis axis, int chunkMin) {
        return fold.isLooped(axis) ? fold.getMinBlock(axis) - 1 : chunkMin;
    }

    private static int ringMax(WorldFold fold, Direction.Axis axis, int chunkMin) {
        return fold.isLooped(axis) ? fold.getMaxBlock(axis) + 1 : chunkMin + CHUNK_BLOCKS;
    }

    private static boolean onWorldEdge(WorldFold fold, Direction.Axis axis, int coord) {
        return fold.isLooped(axis) && (coord == fold.getMinBlock(axis) || coord == fold.getMaxBlock(axis) - 1);
    }

    private static int regionIndex(int x, int z) {
        return Math.floorMod(x, REGION_BLOCKS) + Math.floorMod(z, REGION_BLOCKS) * REGION_BLOCKS;
    }

    private static boolean samePixel(MapRegionData source, int from, MapRegionData target, int to) {
        return target.height[to] == source.height[from]
                && target.waterLightAndBiome[to] == source.waterLightAndBiome[from]
                && target.foliage[to] == source.foliage[from]
                && target.grass[to] == source.grass[from]
                && target.water[to] == source.water[from]
                && target.getBlockIndex(to) == source.getBlockIndex(from);
    }

    private static void copyPixel(MapRegionData source, int from, MapRegionData target, int to) {
        target.height[to] = source.height[from];
        target.waterLightAndBiome[to] = source.waterLightAndBiome[from];
        target.foliage[to] = source.foliage[from];
        target.grass[to] = source.grass[from];
        target.water[to] = source.water[from];
        target.setBlockIndex(to, source.getBlockIndex(from));
    }

    public static AxisCopies copies(ResourceKey<Level> dimension, Direction.Axis axis) {
        WorldFold fold = ClientFolds.of(dimension);
        return fold == null ? AxisCopies.UNBOUNDED : AxisCopies.of(fold, axis);
    }

    public static int[] minimapSplits(Direction.Axis axis, XZ centreChunk, int[] unfolded) {
        return minimapSplits(ClientFolds.getCurrent(), axis, centreChunk.x(), centreChunk.z(), unfolded);
    }

    static int[] minimapSplits(@Nullable WorldFold fold, Direction.Axis axis, int centreChunkX, int centreChunkZ,
            int[] unfolded) {
        if (fold == null || !fold.isLooped(axis)) {
            return unfolded;
        }

        int[] cuts = new int[MINIMAP_CHUNKS + 1];
        int count = 0;
        cuts[count++] = 0;
        for (int along = 1; along < MINIMAP_CHUNKS; along++) {
            if (runBreaks(fold, axis, centreChunkX - MINIMAP_CENTRE_CHUNK, centreChunkZ - MINIMAP_CENTRE_CHUNK, along)) {
                cuts[count++] = along;
            }
        }

        cuts[count++] = MINIMAP_CHUNKS;
        return Arrays.copyOf(cuts, count);
    }

    private static boolean runBreaks(WorldFold fold, Direction.Axis axis, int firstChunkX, int firstChunkZ,
            int along) {
        int stepX = axis == Direction.Axis.X ? 1 : 0;
        int stepZ = 1 - stepX;
        for (int across = 0; across < MINIMAP_CHUNKS; across++) {
            int chunkX = firstChunkX + (stepX == 1 ? along : across);
            int chunkZ = firstChunkZ + (stepZ == 1 ? along : across);
            ChunkPos here = fold.fold(new ChunkPos(chunkX, chunkZ));
            ChunkPos before = fold.fold(new ChunkPos(chunkX - stepX, chunkZ - stepZ));
            if (here.x() != before.x() + stepX || here.z() != before.z() + stepZ
                    || regionOf(here.x()) != regionOf(before.x()) || regionOf(here.z()) != regionOf(before.z())) {
                return true;
            }
        }

        return false;
    }

    public static int minimapSourcePixel(Direction.Axis axis, int stored, int chunkX, int chunkZ,
            int chunksPerRegion) {
        return minimapSourcePixel(ClientFolds.getCurrent(), axis, stored, chunkX, chunkZ, chunksPerRegion);
    }

    static int minimapSourcePixel(@Nullable WorldFold fold, Direction.Axis axis, int stored, int chunkX,
            int chunkZ, int chunksPerRegion) {
        ChunkPos folded = foldedChunk(fold, chunkX, chunkZ);
        int raw = axis == Direction.Axis.X ? chunkX : chunkZ;
        int canonical = axis == Direction.Axis.X ? folded.x() : folded.z();
        return canonical == raw ? stored : regionImagePixel(canonical, chunksPerRegion);
    }

    private static int regionImagePixel(int chunk, int chunksPerRegion) {
        return (chunk & chunksPerRegion - 1) * CHUNK_BLOCKS;
    }

    private static int regionOf(int chunk) {
        return Math.floorDiv(chunk, REGION_CHUNKS);
    }

    private FtbChunksFold() {
    }
}
