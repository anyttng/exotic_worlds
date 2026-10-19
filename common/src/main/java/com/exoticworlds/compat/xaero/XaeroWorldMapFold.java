package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.ClientShapes;
import com.exoticworlds.compat.FullscreenZoomFloor;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.WorldCopies;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

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

    private static final long[] NO_REGIONS = new long[0];

    public static @Nullable ToroidalShape browsedShape() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapProcessor processor = session == null ? null : session.getMapProcessor();
        MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
        MapDimension dimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
        return dimension == null ? null : ClientShapes.of(dimension.getDimId());
    }

    public static @Nullable BlockPos foldIdSpawn(ClientLevel level, @Nullable BlockPos spawn) {
        return spawn == null ? null : ClientShapes.fold(ClientShapes.of(level), spawn);
    }

    public static List<TileArea> tilePieces(WorldCopies.Window window, int rawTileX, int rawTileZ) {
        int firstChunkX = rawTileX * TILE_CHUNK_CHUNKS;
        int firstChunkZ = rawTileZ * TILE_CHUNK_CHUNKS;
        int minX = chunkBlock(firstChunkX);
        int minZ = chunkBlock(firstChunkZ);
        int maxX = chunkBlock(firstChunkX + TILE_CHUNK_CHUNKS);
        int maxZ = chunkBlock(firstChunkZ + TILE_CHUNK_CHUNKS);
        List<TileArea> areas = new ArrayList<>();
        for (WorldCopies.Piece piece : WorldCopies.pieces(window.shape(), window.copiesFor(minX, minZ, maxX, maxZ),
                minX, minZ, maxX, maxZ)) {
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

    public static boolean tileLeavesWorld(AxisCopies blockCopiesX, AxisCopies blockCopiesZ, int rawTileX,
            int rawTileZ) {
        int tileBlocks = chunkBlock(TILE_CHUNK_CHUNKS);
        return spanLeavesWorld(blockCopiesX, rawTileX * tileBlocks, tileBlocks)
                || spanLeavesWorld(blockCopiesZ, rawTileZ * tileBlocks, tileBlocks);
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

    public static int tileBlock(int tile) {
        return chunkBlock(tile * TILE_CHUNK_CHUNKS);
    }

    private static int chunkBlock(int chunk) {
        return SectionPos.sectionToBlockCoord(chunk);
    }

    public static ChunkPos canonicalChunk(@Nullable ToroidalShape shape, int chunkX, int chunkZ) {
        return ClientShapes.fold(shape, new ChunkPos(chunkX, chunkZ));
    }

    public static ChunkPos nearestChunk(@Nullable ToroidalShape shape, ChunkPos reference, int chunkX, int chunkZ) {
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

    public static ChunkPos tileOfChunk(@Nullable ToroidalShape shape, int chunkX, int chunkZ) {
        ChunkPos canonical = canonicalChunk(shape, chunkX, chunkZ);
        return new ChunkPos(Math.floorDiv(canonical.x(), TILE_CHUNK_CHUNKS),
                Math.floorDiv(canonical.z(), TILE_CHUNK_CHUNKS));
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

    public static ChunkPos foldRegion(@Nullable ToroidalShape shape, int regionX, int regionZ) {
        ChunkPos tile = tileOfChunk(shape, firstTileChunkOfRegion(regionX) * TILE_CHUNK_CHUNKS,
                firstTileChunkOfRegion(regionZ) * TILE_CHUNK_CHUNKS);
        return new ChunkPos(regionOfTileChunk(tile.x()), regionOfTileChunk(tile.z()));
    }

    public static long[] canonicalRegionCorners(@Nullable ToroidalShape shape, int leveledRegX, int leveledRegZ,
            int level) {
        int side = REGION_BLOCKS << level;
        int minX = leveledRegX * side;
        int minZ = leveledRegZ * side;
        int maxX = minX + side - 1;
        int maxZ = minZ + side - 1;
        return new long[] {canonicalRegion(shape, minX, minZ, side), canonicalRegion(shape, maxX, minZ, side),
                canonicalRegion(shape, minX, maxZ, side), canonicalRegion(shape, maxX, maxZ, side)};
    }

    private static long canonicalRegion(@Nullable ToroidalShape shape, int blockX, int blockZ, int side) {
        BlockPos folded = foldBlock(shape, blockX, blockZ);
        return ChunkPos.pack(Math.floorDiv(folded.getX(), side), Math.floorDiv(folded.getZ(), side));
    }

    public static ChunkPos foldComparison(@Nullable ToroidalShape shape, int comparisonX, int comparisonZ) {
        ChunkPos canonical = canonicalChunk(shape, comparisonX + COMPARISON_CHUNK_OFFSET,
                comparisonZ + COMPARISON_CHUNK_OFFSET);
        return new ChunkPos(canonical.x() - COMPARISON_CHUNK_OFFSET, canonical.z() - COMPARISON_CHUNK_OFFSET);
    }

    public static long[] canonicalRegions(@Nullable ToroidalShape shape, int startTileChunkX, int startTileChunkZ,
            int endTileChunkX, int endTileChunkZ) {
        return WorldCopies.canonicalCells(shape,
                chunkBlock(startTileChunkX * TILE_CHUNK_CHUNKS), chunkBlock(startTileChunkZ * TILE_CHUNK_CHUNKS),
                chunkBlock((endTileChunkX + 1) * TILE_CHUNK_CHUNKS), chunkBlock((endTileChunkZ + 1) * TILE_CHUNK_CHUNKS),
                REGION_BLOCKS);
    }

    public static long[] canonicalSlotOrigins(WorldCopies.Window window, int viewBlockX, int viewBlockZ, int slotSize) {
        long[] slots = slotCells(window, viewBlockX, viewBlockZ, slotSize);
        for (int i = 0; i < slots.length; i++) {
            slots[i] = ChunkPos.pack(ChunkPos.getX(slots[i]) * slotSize, ChunkPos.getZ(slots[i]) * slotSize);
        }

        return slots;
    }

    private static long[] slotCells(WorldCopies.Window window, int viewBlockX, int viewBlockZ, int slotSize) {
        int maxX = viewBlockX + slotSize;
        int maxZ = viewBlockZ + slotSize;
        return WorldCopies.canonicalCells(window.shape(), window.copiesFor(viewBlockX, viewBlockZ, maxX, maxZ),
                viewBlockX, viewBlockZ, maxX, maxZ, slotSize);
    }

    public static long @Nullable [] frameRegions(int minTileChunkX, int minTileChunkZ, int maxTileChunkX,
            int maxTileChunkZ) {
        return frameRegions(browsedShape(), minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
    }

    static long @Nullable [] frameRegions(@Nullable ToroidalShape shape, int minTileChunkX, int minTileChunkZ,
            int maxTileChunkX, int maxTileChunkZ) {
        if (shape == null
                || (!spanLeavesWorld(AxisCopies.of(shape, Direction.Axis.X), minTileChunkX * SLOT_BLOCKS,
                        (maxTileChunkX + 1 - minTileChunkX) * SLOT_BLOCKS)
                && !spanLeavesWorld(AxisCopies.of(shape, Direction.Axis.Z), minTileChunkZ * SLOT_BLOCKS,
                        (maxTileChunkZ + 1 - minTileChunkZ) * SLOT_BLOCKS))) {
            return null;
        }

        return canonicalRegions(shape, minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
    }

    public static long[] fannedRegions(WorldCopies.Window window, int leveledRegX, int leveledRegZ, int level) {
        ToroidalShape shape = window.shape();
        int side = REGION_BLOCKS << level;
        if (shape == null
                || (!spanLeavesWorld(AxisCopies.of(shape, Direction.Axis.X), leveledRegX * side, side)
                && !spanLeavesWorld(AxisCopies.of(shape, Direction.Axis.Z), leveledRegZ * side, side))) {
            return NO_REGIONS;
        }

        return slotCells(window, leveledRegX * side, leveledRegZ * side, side);
    }

    public static long[] fannedLeaves(WorldCopies.Window window, int leveledRegX, int leveledRegZ, int level) {
        int leaves = 1 << level;
        long[] regions = fannedRegions(window, leveledRegX, leveledRegZ, level);
        long[] fanned = new long[regions.length * leaves * leaves];
        int next = 0;
        for (long region : regions) {
            for (int leafX = 0; leafX < leaves; leafX++) {
                for (int leafZ = 0; leafZ < leaves; leafZ++) {
                    fanned[next++] = ChunkPos.pack(ChunkPos.getX(region) * leaves + leafX,
                            ChunkPos.getZ(region) * leaves + leafZ);
                }
            }
        }

        return fanned;
    }

    public static List<TilePlacement> placements(int originX, int originZ, int size, List<WorldCopies.Copy> copies) {
        return placements(browsedShape(), originX, originZ, size, copies);
    }

    public static List<TilePlacement> placements(@Nullable ToroidalShape shape, int originX, int originZ, int size,
            List<WorldCopies.Copy> copies) {
        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        int minX = x.clipMin(originX);
        int maxX = x.clipMax(originX + size);
        int minZ = z.clipMin(originZ);
        int maxZ = z.clipMax(originZ + size);
        if (minX >= maxX || minZ >= maxZ) {
            return List.of();
        }

        float u1 = (float) (minX - originX) / size;
        float u2 = (float) (maxX - originX) / size;
        float v1 = (float) (minZ - originZ) / size;
        float v2 = (float) (maxZ - originZ) / size;
        List<TilePlacement> placements = new ArrayList<>(copies.size());
        for (WorldCopies.Copy copy : copies) {
            placements.add(new TilePlacement(minX + copy.dx(), minZ + copy.dz(), maxX - minX, maxZ - minZ,
                    u1, v1, u2, v2));
        }

        return placements;
    }

    public record TilePlacement(int minX, int minZ, int width, int height, float u1, float v1, float u2, float v2) {
        public boolean meets(int spanMinX, int spanMinZ, int spanMaxX, int spanMaxZ) {
            return this.minX < spanMaxX && this.minX + this.width > spanMinX
                    && this.minZ < spanMaxZ && this.minZ + this.height > spanMinZ;
        }
    }

    public static boolean spanLeavesWorld(AxisCopies copies, int first, int size) {
        int end = first + size;
        return copies.clipMin(first) != first || copies.clipMax(end) != end;
    }

    public static BlockPos foldBlock(int blockX, int blockZ) {
        return foldBlock(browsedShape(), blockX, blockZ);
    }

    public static BlockPos foldBlock(@Nullable ToroidalShape shape, int blockX, int blockZ) {
        return ClientShapes.fold(shape, new BlockPos(blockX, 0, blockZ));
    }

    public static boolean glueableAt(@Nullable ToroidalShape shape, int slotSizeBlocks) {
        if (shape == null) {
            return false;
        }

        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        if ((x.loops() && Math.floorMod(x.min(), slotSizeBlocks) != 0)
                || (z.loops() && Math.floorMod(z.min(), slotSizeBlocks) != 0)) {
            return false;
        }

        List<WorldCopies.Copy> neighbors = WorldCopies.meeting(shape,
                x.loops() ? x.min() - x.width() : 0, z.loops() ? z.min() - z.width() : 0,
                x.loops() ? x.max() + x.width() : 1, z.loops() ? z.max() + z.width() : 1);
        for (WorldCopies.Copy copy : neighbors) {
            if (Math.floorMod(copy.dx(), slotSizeBlocks) != 0 || Math.floorMod(copy.dz(), slotSizeBlocks) != 0) {
                return false;
            }
        }

        return true;
    }

    public static List<WorldCopies.Copy> drawnCopies(int[] spanX, int[] spanZ, MapCopies mapCopies) {
        return drawnCopies(browsedShape(), spanX, spanZ, mapCopies);
    }

    public static List<WorldCopies.Copy> drawnCopies(@Nullable ToroidalShape shape, int[] spanX, int[] spanZ,
            MapCopies mapCopies) {
        return drawnCopies(WorldCopies.meeting(shape, spanX[0], spanZ[0], spanX[1], spanZ[1]), mapCopies);
    }

    public static List<WorldCopies.Copy> drawnCopies(List<WorldCopies.Copy> copies, MapCopies mapCopies) {
        if (mapCopies != MapCopies.SINGLE) {
            return copies;
        }

        return copies.contains(WorldCopies.IDENTITY) ? List.of(WorldCopies.IDENTITY) : List.of();
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

    public static Vec3 foldPoint(double blockX, double blockZ) {
        return foldPoint(browsedShape(), blockX, blockZ);
    }

    public static Vec3 foldFootprint(ClientLevel level, double blockX, double blockZ) {
        return foldPoint(ClientShapes.of(level), blockX, blockZ);
    }

    static Vec3 foldPoint(@Nullable ToroidalShape shape, double blockX, double blockZ) {
        return ClientShapes.fold(shape, new Vec3(blockX, 0.0, blockZ));
    }

    private XaeroWorldMapFold() {
    }
}
