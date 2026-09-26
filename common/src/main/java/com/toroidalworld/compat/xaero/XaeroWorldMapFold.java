package com.toroidalworld.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldClientApi;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.ClientShapes;
import com.toroidalworld.compat.FullscreenZoomFloor;
import com.toroidalworld.compat.MapCopies;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WrapDomain;

import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public final class XaeroWorldMapFold {
    public static final int REGION_BLOCKS = 512;
    public static final int SLOT_BLOCKS = 64;

    public static final int REGION_TILE_CHUNKS = 8;

    private static final int TILE_CHUNK_CHUNKS = 4;

    private static final int COMPARISON_CHUNK_OFFSET = 16;

    private static @Nullable ToroidalShape browsedShape() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapProcessor processor = session == null ? null : session.getMapProcessor();
        MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
        MapDimension dimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
        return dimension == null ? null : ToroidalWorldClientApi.shapeOf(dimension.getDimId()).orElse(null);
    }

    public static boolean active() {
        return browsedShape() != null;
    }

    public static BlockPos foldIdSpawn(ClientLevel level, BlockPos spawn) {
        ToroidalShape shape = ClientShapes.of(level);
        if (shape == null || spawn == null) {
            return spawn;
        }

        return shape.fold(spawn);
    }

    public static List<TilePiece> tilePieces(AxisCopies chunkCopies, int rawTile) {
        int firstChunk = rawTile * TILE_CHUNK_CHUNKS;
        if (!chunkCopies.loops()) {
            return List.of(new TilePiece(rawTile, 0, TILE_CHUNK_CHUNKS, 0));
        }

        List<TilePiece> pieces = new ArrayList<>();
        int rawOffset = 0;
        while (rawOffset < TILE_CHUNK_CHUNKS) {
            int canonical = canonicalChunk(chunkCopies, firstChunk + rawOffset);
            int inside = Math.floorMod(canonical, TILE_CHUNK_CHUNKS);
            int count = Math.min(TILE_CHUNK_CHUNKS - inside, TILE_CHUNK_CHUNKS - rawOffset);
            count = Math.min(count, chunkCopies.max() - canonical);
            pieces.add(new TilePiece(Math.floorDiv(canonical, TILE_CHUNK_CHUNKS), inside, count, rawOffset));
            rawOffset += count;
        }

        return pieces;
    }

    public static int insideTile(AxisCopies chunkCopies, int chunk) {
        return Math.floorMod(canonicalChunk(chunkCopies, chunk), TILE_CHUNK_CHUNKS);
    }

    public static int lastInsideTile(AxisCopies chunkCopies, int chunk) {
        int canonical = canonicalChunk(chunkCopies, chunk);
        boolean lastInWorld = chunkCopies.loops() && canonical == chunkCopies.max() - 1;
        return lastInWorld ? TILE_CHUNK_CHUNKS - 1 : Math.floorMod(canonical, TILE_CHUNK_CHUNKS);
    }

    private static int canonicalChunk(AxisCopies chunkCopies, int chunk) {
        return chunkCopies.loops() ? new WrapDomain(chunkCopies.min(), chunkCopies.max()).wrap(chunk) : chunk;
    }

    public static int tileOfChunk(Direction.Axis axis, int chunk) {
        return Math.floorDiv(foldChunk(axis, chunk), TILE_CHUNK_CHUNKS);
    }

    public record TilePiece(int canonicalTile, int firstInside, int count, int rawOffset) {
    }

    public static int firstTileChunkOfRegion(int region) {
        return region * REGION_TILE_CHUNKS;
    }

    public static int regionOfTileChunk(int tileChunk) {
        return Math.floorDiv(tileChunk, REGION_TILE_CHUNKS);
    }

    public static int tileChunkInRegion(int tileChunk) {
        return Math.floorMod(tileChunk, REGION_TILE_CHUNKS);
    }

    public static int foldRegion(Direction.Axis axis, int region) {
        return regionOfTileChunk(tileOfChunk(axis, firstTileChunkOfRegion(region) * TILE_CHUNK_CHUNKS));
    }

    public static boolean regionCrossesSeam(Direction.Axis axis, int region) {
        return regionCrossesSeam(copies(axis), region);
    }

    static boolean regionCrossesSeam(AxisCopies copies, int region) {
        return spanLeavesWorld(copies, region * REGION_BLOCKS, REGION_BLOCKS);
    }

    public static int foldChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = browsedShape();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static int foldComparisonChunk(Direction.Axis axis, int comparison) {
        return foldChunk(axis, comparison + COMPARISON_CHUNK_OFFSET) - COMPARISON_CHUNK_OFFSET;
    }

    public static int[] canonicalRegions(Direction.Axis axis, int startTileChunk, int endTileChunk) {
        List<Integer> regions = new ArrayList<>();
        for (int chunk = startTileChunk * TILE_CHUNK_CHUNKS; chunk < (endTileChunk + 1) * TILE_CHUNK_CHUNKS; chunk++) {
            int region = regionOfTileChunk(tileOfChunk(axis, chunk));
            if (!regions.contains(region)) {
                regions.add(region);
            }
        }

        int[] result = new int[regions.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = regions.get(i);
        }

        return result;
    }

    public static int[] canonicalSlotOrigins(AxisCopies copies, int viewBlock, int slotSize) {
        if (!copies.loops()) {
            return new int[] {viewBlock};
        }

        int end = viewBlock + slotSize;
        IntLinkedOpenHashSet origins = new IntLinkedOpenHashSet();
        for (int lap : copies.laps(viewBlock, end)) {
            int offset = copies.offset(lap);
            int pieceMin = copies.clipMin(viewBlock - offset);
            int pieceMax = copies.clipMax(end - offset);
            for (int origin = Math.floorDiv(pieceMin, slotSize) * slotSize; origin < pieceMax; origin += slotSize) {
                origins.add(origin);
            }
        }

        return origins.toIntArray();
    }

    public static boolean spanLeavesWorld(AxisCopies copies, int first, int size) {
        int end = first + size;
        return copies.clipMin(first) != first || copies.clipMax(end) != end;
    }

    public static int foldBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = browsedShape();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static boolean glueableAt(int slotSizeBlocks) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return false;
        }

        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)
                    && (shape.widthBlocks(axis) % slotSizeBlocks != 0
                            || Math.floorMod(shape.minBlock(axis), slotSizeBlocks) != 0)) {
                return false;
            }
        }

        return true;
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = browsedShape();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static AxisCopies chunkCopies(Direction.Axis axis) {
        ToroidalShape shape = browsedShape();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.ofChunks(shape, axis);
    }

    public static double zoomFloorScale(double scaleMultiplier, MapCopies mapCopies, int windowWidth, int windowHeight) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return 0.0;
        }

        return mapCopies == MapCopies.SINGLE
                ? FullscreenZoomFloor.xaeroCoverScale(shape, scaleMultiplier, windowWidth, windowHeight)
                : FullscreenZoomFloor.xaeroScale(shape, scaleMultiplier);
    }

    public static int[] viewSpan(double camera, int windowPixels, double scale, int margin) {
        double halfSpan = windowPixels / 2.0 / scale;
        return new int[] {(int) Math.floor(camera - halfSpan) - margin, (int) Math.ceil(camera + halfSpan) + margin};
    }

    public static double foldCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = browsedShape();
        return shape == null ? coord : shape.foldCoord(axis, coord);
    }

    public static double foldFootprintCoord(ClientLevel level, Direction.Axis axis, double coord) {
        ToroidalShape shape = ClientShapes.of(level);
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    private XaeroWorldMapFold() {
    }
}
