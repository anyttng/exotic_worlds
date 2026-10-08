package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.ExoticWorldsClientApi;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.core.CoordinateConstants;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

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
        return dimension == null ? null : ExoticWorldsClientApi.shapeOf(dimension.getDimId()).orElse(null);
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

    public static List<TileArea> tilePieces(int rawTileX, int rawTileZ) {
        return tilePieces(browsedShape(), rawTileX, rawTileZ);
    }

    static List<TileArea> tilePieces(@Nullable ToroidalShape shape, int rawTileX, int rawTileZ) {
        int firstChunkX = rawTileX * TILE_CHUNK_CHUNKS;
        int firstChunkZ = rawTileZ * TILE_CHUNK_CHUNKS;
        List<TileArea> areas = new ArrayList<>();
        for (WorldCopies.Piece piece : WorldCopies.pieces(shape, chunkBlock(firstChunkX), chunkBlock(firstChunkZ),
                chunkBlock(firstChunkX + TILE_CHUNK_CHUNKS), chunkBlock(firstChunkZ + TILE_CHUNK_CHUNKS))) {
            List<TilePiece> piecesX = axisPieces(SectionPos.blockToSectionCoord(piece.minX()),
                    SectionPos.blockToSectionCoord(piece.maxX()),
                    SectionPos.blockToSectionCoord(piece.copy().dx()) - firstChunkX);
            List<TilePiece> piecesZ = axisPieces(SectionPos.blockToSectionCoord(piece.minZ()),
                    SectionPos.blockToSectionCoord(piece.maxZ()),
                    SectionPos.blockToSectionCoord(piece.copy().dz()) - firstChunkZ);
            for (TilePiece pieceX : piecesX) {
                for (TilePiece pieceZ : piecesZ) {
                    areas.add(new TileArea(pieceX, pieceZ));
                }
            }
        }

        return areas;
    }

    private static List<TilePiece> axisPieces(int minChunk, int maxChunk, int rawShift) {
        List<TilePiece> pieces = new ArrayList<>();
        int canonical = minChunk;
        while (canonical < maxChunk) {
            int inside = Math.floorMod(canonical, TILE_CHUNK_CHUNKS);
            int count = Math.min(TILE_CHUNK_CHUNKS - inside, maxChunk - canonical);
            pieces.add(new TilePiece(Math.floorDiv(canonical, TILE_CHUNK_CHUNKS), inside, count, canonical + rawShift));
            canonical += count;
        }

        return pieces;
    }

    private static int chunkBlock(int chunk) {
        return SectionPos.sectionToBlockCoord(chunk);
    }

    public static ChunkPos canonicalChunk(int chunkX, int chunkZ) {
        ToroidalShape shape = browsedShape();
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        return shape == null ? chunk : shape.fold(chunk);
    }

    public static ChunkPos nearestChunk(ChunkPos reference, int chunkX, int chunkZ) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return new ChunkPos(chunkX, chunkZ);
        }

        BlockPos nearest = shape.nearestCopy(reference.getWorldPosition(), new ChunkPos(chunkX, chunkZ).getWorldPosition());
        return ChunkPos.containing(nearest);
    }

    public static int insideTile(int canonicalChunk) {
        return Math.floorMod(canonicalChunk, TILE_CHUNK_CHUNKS);
    }

    public static int lastInsideTile(AxisCopies chunkCopies, int canonicalChunk) {
        boolean lastInWorld = chunkCopies.loops() && canonicalChunk == chunkCopies.max() - 1;
        return lastInWorld ? TILE_CHUNK_CHUNKS - 1 : insideTile(canonicalChunk);
    }

    public static int tileOfChunk(Direction.Axis axis, int chunk) {
        return Math.floorDiv(foldChunk(axis, chunk), TILE_CHUNK_CHUNKS);
    }

    public record TilePiece(int canonicalTile, int firstInside, int count, int rawOffset) {
    }

    public record TileArea(TilePiece x, TilePiece z) {
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

    public static long[] canonicalSlotOrigins(int viewBlockX, int viewBlockZ, int slotSize) {
        return canonicalSlotOrigins(browsedShape(), viewBlockX, viewBlockZ, slotSize);
    }

    static long[] canonicalSlotOrigins(@Nullable ToroidalShape shape, int viewBlockX, int viewBlockZ, int slotSize) {
        LongLinkedOpenHashSet origins = new LongLinkedOpenHashSet();
        for (WorldCopies.Piece piece : WorldCopies.pieces(shape, viewBlockX, viewBlockZ,
                viewBlockX + slotSize, viewBlockZ + slotSize)) {
            for (int x = Math.floorDiv(piece.minX(), slotSize) * slotSize; x < piece.maxX(); x += slotSize) {
                for (int z = Math.floorDiv(piece.minZ(), slotSize) * slotSize; z < piece.maxZ(); z += slotSize) {
                    origins.add(ChunkPos.pack(x, z));
                }
            }
        }

        return origins.toLongArray();
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

    public static List<WorldCopies.Copy> drawnCopies(int[] spanX, int[] spanZ, MapCopies mapCopies) {
        return drawnCopies(browsedShape(), spanX, spanZ, mapCopies);
    }

    static List<WorldCopies.Copy> drawnCopies(@Nullable ToroidalShape shape, int[] spanX, int[] spanZ,
            MapCopies mapCopies) {
        List<WorldCopies.Copy> copies = WorldCopies.meeting(shape, spanX[0], spanZ[0], spanX[1], spanZ[1]);
        if (mapCopies != MapCopies.SINGLE) {
            return copies;
        }

        return copies.contains(WorldCopies.IDENTITY) ? List.of(WorldCopies.IDENTITY) : List.of();
    }

    public static List<WorldCopies.Edge> seams(int[] spanX, int[] spanZ) {
        return WorldCopies.seams(browsedShape(), spanX[0], spanZ[0], spanX[1], spanZ[1]);
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
