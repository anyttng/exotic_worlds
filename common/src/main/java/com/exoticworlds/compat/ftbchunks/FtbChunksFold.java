package com.exoticworlds.compat.ftbchunks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapCopyBudget;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
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

    private static List<WorldCopies.Copy> largeMapCopies = List.of(WorldCopies.IDENTITY);

    public static XZ chunkOf(int chunkX, int chunkZ) {
        ChunkPos folded = foldedChunk(ClientShapes.current(), chunkX, chunkZ);
        return XZ.of(folded.x, folded.z);
    }

    public static XZ chunkOf(ChunkPos pos) {
        return chunkOf(pos.x, pos.z);
    }

    public static XZ chunkOf(XZ chunk) {
        return chunkOf(chunk.x(), chunk.z());
    }

    public static ChunkPos foldedChunkPos(ChunkPos pos) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? pos : shape.fold(pos);
    }

    public static Set<XZ> foldedChunks(Set<XZ> chunks) {
        return foldedChunks(ClientShapes.current(), chunks);
    }

    static Set<XZ> foldedChunks(@Nullable ToroidalShape shape, Set<XZ> chunks) {
        if (shape == null) {
            return chunks;
        }

        Set<XZ> folded = new HashSet<>(chunks.size());
        for (XZ chunk : chunks) {
            ChunkPos pos = shape.fold(new ChunkPos(chunk.x(), chunk.z()));
            folded.add(XZ.of(pos.x, pos.z));
        }

        return folded;
    }

    public static XZ regionOfChunk(int chunkX, int chunkZ) {
        ChunkPos folded = foldedChunk(ClientShapes.current(), chunkX, chunkZ);
        return XZ.regionFromChunk(folded.x, folded.z);
    }

    public static XZ regionOfChunk(ChunkPos pos) {
        return regionOfChunk(pos.x, pos.z);
    }

    public static BlockPos foldBlock(int x, int z) {
        BlockPos pos = new BlockPos(x, 0, z);
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? pos : shape.fold(pos);
    }

    public static Vec3 foldPosition(Vec3 position) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? position : shape.fold(position);
    }

    static ChunkPos foldedChunk(@Nullable ToroidalShape shape, int chunkX, int chunkZ) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        return shape == null ? pos : shape.fold(pos);
    }

    public static int loopedAxes() {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        return (shape.loops(Direction.Axis.X) ? 1 : 0) + (shape.loops(Direction.Axis.Z) ? 1 : 0);
    }

    public static List<WorldCopies.Copy> drawnCopies(int minX, int minZ, int maxX, int maxZ, MapCopies mapCopies) {
        return drawnCopies(ClientShapes.current(), minX, minZ, maxX, maxZ, mapCopies);
    }

    static List<WorldCopies.Copy> drawnCopies(@Nullable ToroidalShape shape, int minX, int minZ, int maxX, int maxZ,
            MapCopies mapCopies) {
        List<WorldCopies.Copy> copies = WorldCopies.meeting(shape, minX, minZ, maxX, maxZ);
        if (mapCopies != MapCopies.SINGLE) {
            return copies;
        }

        return copies.contains(WorldCopies.IDENTITY) ? List.of(WorldCopies.IDENTITY) : List.of();
    }

    public static List<SeamLine> seamLines(SeamView view) {
        return seamLines(ClientShapes.current(), view);
    }

    static List<SeamLine> seamLines(@Nullable ToroidalShape shape, SeamView view) {
        double pixelsPerBlock = view.tilePixels() / (double) REGION_BLOCKS;
        int minX = (int) Math.floor((view.x() - view.originX()) / pixelsPerBlock);
        int maxX = (int) Math.ceil((view.x() + view.width() - view.originX()) / pixelsPerBlock);
        int minZ = (int) Math.floor((view.y() - view.originY()) / pixelsPerBlock);
        int maxZ = (int) Math.ceil((view.y() + view.height() - view.originY()) / pixelsPerBlock);
        List<SeamLine> lines = new ArrayList<>();
        for (WorldCopies.Edge edge : WorldCopies.seams(shape, minX, minZ, maxX, maxZ)) {
            int fromX = pixel(view.originX(), edge.fromX(), pixelsPerBlock);
            int fromY = pixel(view.originY(), edge.fromZ(), pixelsPerBlock);
            int toX = pixel(view.originX(), edge.toX(), pixelsPerBlock);
            int toY = pixel(view.originY(), edge.toZ(), pixelsPerBlock);
            lines.add(edge.fromX() == edge.toX()
                    ? new SeamLine(true, fromX, fromY, toY)
                    : new SeamLine(false, fromY, fromX, toX));
        }

        return lines;
    }

    private static int pixel(int originPixel, int block, double pixelsPerBlock) {
        return originPixel + (int) Math.round(block * pixelsPerBlock);
    }

    public static double clampScroll(Direction.Axis axis, double scroll, int regionMin, int regionTilePixels,
            int panelPixels) {
        return clampScroll(copies(axis), scroll, regionMin, regionTilePixels, panelPixels);
    }

    static double clampScroll(AxisCopies copies, double scroll, int regionMin, int regionTilePixels, int panelPixels) {
        double pixelsPerBlock = regionTilePixels / (double) REGION_BLOCKS;
        double centre = regionMin * (double) REGION_BLOCKS + (scroll + panelPixels / 2.0) / pixelsPerBlock;
        double clamped = copies.clampView(centre, panelPixels / 2.0 / pixelsPerBlock);
        return scroll + (clamped - centre) * pixelsPerBlock;
    }

    public static int tileFloor(int windowWidth, int windowHeight) {
        return zoomFloor(windowWidth, windowHeight) * TILE_PIXELS_PER_ZOOM;
    }

    public static int zoomFloor(int windowWidth, int windowHeight) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        return MapCopies.current() == MapCopies.SINGLE
                ? FullscreenZoomFloor.ftbChunksCoverZoom(shape, windowWidth, windowHeight)
                : FullscreenZoomFloor.ftbChunksZoom(shape);
    }

    public record TileBlit(int x, int y, int width, int height, float u0, float v0, float u1, float v1) {
    }

    public static void recordLargeMapCopies(List<WorldCopies.Copy> copies) {
        largeMapCopies = copies;
    }

    public static Copies largeMapCopies() {
        return MapCopyBudget.painted(ClientShapes.current(), largeMapCopies);
    }

    public record SeamView(int originX, int originY, int tilePixels, int x, int y, int width, int height) {
    }

    public record SeamLine(boolean vertical, int at, int from, int to) {
    }

    record HaloPixel(int sourceX, int sourceZ, int haloX, int haloZ) {
    }

    // The part of a region tile that lies inside the world, as a screen rectangle and its window of the region image
    // in image pixels. The rest of the image is opaque black, so a tile blitted whole paints over the copy beside it.
    public static @Nullable TileBlit worldPartOf(int regionX, int regionZ, int x, int y, int width, int height) {
        int[] spanX = worldSpanInRegion(Direction.Axis.X, regionX);
        int[] spanZ = worldSpanInRegion(Direction.Axis.Z, regionZ);
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
                textureEdge(spanX[0]), textureEdge(spanZ[0]), textureEdge(spanX[1]), textureEdge(spanZ[1]));
    }

    private static float textureEdge(int blockOffset) {
        return blockOffset / (float) REGION_BLOCKS;
    }

    static int[] worldSpanInRegion(Direction.Axis axis, int region) {
        ToroidalShape shape = ClientShapes.current();
        int regionStart = region * REGION_BLOCKS;
        if (shape == null || !shape.loops(axis)) {
            return new int[] {0, REGION_BLOCKS};
        }

        int from = Math.max(shape.minBlock(axis), regionStart) - regionStart;
        int to = Math.min(shape.maxBlock(axis), regionStart + REGION_BLOCKS) - regionStart;
        return new int[] {from, Math.max(from, to)};
    }

    // FTB shades a pixel against its north and west neighbours inside the same region image, so the world's edge
    // pixels would read an unexplored neighbour on every copy; the halo lies outside worldSpanInRegion and is never
    // blitted.
    public static void mirrorSeamEdges(MapDimension dimension, int foldedChunkX, int foldedChunkZ) {
        List<HaloPixel> halo = haloPixels(ClientShapes.current(), foldedChunkX, foldedChunkZ);
        if (halo.isEmpty()) {
            return;
        }

        MapRegionData source = dimension.getRegion(XZ.regionFromChunk(foldedChunkX, foldedChunkZ)).getDataBlocking();
        Map<XZ, MapRegion> touched = new HashMap<>();
        for (HaloPixel pixel : halo) {
            MapRegion target = touched.computeIfAbsent(XZ.regionFromBlock(pixel.haloX(), pixel.haloZ()),
                    dimension::getRegion);
            copyPixel(source, regionIndex(pixel.sourceX(), pixel.sourceZ()), target.getDataBlocking(),
                    regionIndex(pixel.haloX(), pixel.haloZ()));
        }

        for (MapRegion region : touched.values()) {
            region.update(true);
        }
    }

    static List<HaloPixel> haloPixels(@Nullable ToroidalShape shape, int foldedChunkX, int foldedChunkZ) {
        if (shape == null) {
            return List.of();
        }

        int chunkMinX = foldedChunkX * CHUNK_BLOCKS;
        int chunkMinZ = foldedChunkZ * CHUNK_BLOCKS;
        AABB ring = new AABB(ringMin(shape, Direction.Axis.X, chunkMinX), 0.0, ringMin(shape, Direction.Axis.Z, chunkMinZ),
                ringMax(shape, Direction.Axis.X, chunkMinX), 1.0, ringMax(shape, Direction.Axis.Z, chunkMinZ));
        List<HaloPixel> halo = new ArrayList<>();
        for (int z = chunkMinZ; z < chunkMinZ + CHUNK_BLOCKS; z++) {
            for (int x = chunkMinX; x < chunkMinX + CHUNK_BLOCKS; x++) {
                if (!onWorldEdge(shape, Direction.Axis.X, x) && !onWorldEdge(shape, Direction.Axis.Z, z)) {
                    continue;
                }

                BlockPos pixel = new BlockPos(x, 0, z);
                for (ToroidalShape.Oriented<BlockPos> copy : shape.copiesInside(ring, pixel)) {
                    if (!copy.value().equals(pixel)) {
                        halo.add(new HaloPixel(x, z, copy.value().getX(), copy.value().getZ()));
                    }
                }
            }
        }

        return halo;
    }

    private static int ringMin(ToroidalShape shape, Direction.Axis axis, int chunkMin) {
        return shape.loops(axis) ? shape.minBlock(axis) - 1 : chunkMin;
    }

    private static int ringMax(ToroidalShape shape, Direction.Axis axis, int chunkMin) {
        return shape.loops(axis) ? shape.maxBlock(axis) + 1 : chunkMin + CHUNK_BLOCKS;
    }

    private static boolean onWorldEdge(ToroidalShape shape, Direction.Axis axis, int coord) {
        return shape.loops(axis) && (coord == shape.minBlock(axis) || coord == shape.maxBlock(axis) - 1);
    }

    private static int regionIndex(int x, int z) {
        return Math.floorMod(x, REGION_BLOCKS) + Math.floorMod(z, REGION_BLOCKS) * REGION_BLOCKS;
    }

    private static void copyPixel(MapRegionData source, int from, MapRegionData target, int to) {
        target.height[to] = source.height[from];
        target.waterLightAndBiome[to] = source.waterLightAndBiome[from];
        target.foliage[to] = source.foliage[from];
        target.grass[to] = source.grass[from];
        target.water[to] = source.water[from];
        target.setBlockIndex(to, source.getBlockIndex(from));
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static int[] minimapSplits(Direction.Axis axis, XZ centreChunk, int[] unfolded) {
        return minimapSplits(ClientShapes.current(), axis, centreChunk.x(), centreChunk.z(), unfolded);
    }

    // FTB's own loop reads any number of cuts and copies each piece as one run from one region image, so a cut goes
    // wherever any row or column of the folded window stops being one run inside one region.
    static int[] minimapSplits(@Nullable ToroidalShape shape, Direction.Axis axis, int centreChunkX, int centreChunkZ,
            int[] unfolded) {
        if (shape == null || !shape.loops(axis)) {
            return unfolded;
        }

        int[] cuts = new int[MINIMAP_CHUNKS + 1];
        int count = 0;
        cuts[count++] = 0;
        for (int along = 1; along < MINIMAP_CHUNKS; along++) {
            if (runBreaks(shape, axis, centreChunkX - MINIMAP_CENTRE_CHUNK, centreChunkZ - MINIMAP_CENTRE_CHUNK, along)) {
                cuts[count++] = along;
            }
        }

        cuts[count++] = MINIMAP_CHUNKS;
        return Arrays.copyOf(cuts, count);
    }

    private static boolean runBreaks(ToroidalShape shape, Direction.Axis axis, int firstChunkX, int firstChunkZ,
            int along) {
        int stepX = axis == Direction.Axis.X ? 1 : 0;
        int stepZ = 1 - stepX;
        for (int across = 0; across < MINIMAP_CHUNKS; across++) {
            int chunkX = firstChunkX + (stepX == 1 ? along : across);
            int chunkZ = firstChunkZ + (stepZ == 1 ? along : across);
            ChunkPos here = shape.fold(new ChunkPos(chunkX, chunkZ));
            ChunkPos before = shape.fold(new ChunkPos(chunkX - stepX, chunkZ - stepZ));
            if (here.x != before.x + stepX || here.z != before.z + stepZ
                    || regionOf(here.x) != regionOf(before.x) || regionOf(here.z) != regionOf(before.z)) {
                return true;
            }
        }

        return false;
    }

    public static int regionImagePixel(int chunk, int chunksPerRegion) {
        return (chunk & chunksPerRegion - 1) * CHUNK_BLOCKS;
    }

    private static int regionOf(int chunk) {
        return Math.floorDiv(chunk, REGION_CHUNKS);
    }

    public static int nearestChunk(Direction.Axis axis, int refChunk, int chunk) {
        return nearestChunk(ClientShapes.current(), axis, refChunk, chunk);
    }

    static int nearestChunk(@Nullable ToroidalShape shape, Direction.Axis axis, int refChunk, int chunk) {
        if (shape == null || !shape.loops(axis) || !shape.decomposesPerAxis()) {
            return chunk;
        }

        double nearest = shape.nearestCoord(axis, chunkCentre(refChunk), chunkCentre(chunk));
        return Math.floorDiv((int) Math.floor(nearest), CHUNK_BLOCKS);
    }

    private static double chunkCentre(int chunk) {
        return chunk * (double) CHUNK_BLOCKS + CHUNK_BLOCKS / 2.0;
    }

    public static int[] canonicalRegions(Direction.Axis axis, int region) {
        return canonicalRegions(ClientShapes.current(), axis, region);
    }

    static int[] canonicalRegions(@Nullable ToroidalShape shape, Direction.Axis axis, int region) {
        if (shape == null || !shape.loops(axis)) {
            return new int[] {region};
        }

        List<Integer> canonical = new ArrayList<>();
        int firstChunk = region * REGION_CHUNKS;
        for (int chunk = firstChunk; chunk < firstChunk + REGION_CHUNKS; chunk++) {
            int folded = regionOf(shape.foldChunk(axis, chunk));
            if (!canonical.contains(folded)) {
                canonical.add(folded);
            }
        }

        int[] result = new int[canonical.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = canonical.get(i);
        }

        return result;
    }

    private FtbChunksFold() {
    }
}
