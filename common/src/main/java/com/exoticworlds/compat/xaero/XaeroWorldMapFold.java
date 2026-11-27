package com.exoticworlds.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.AxisCopies;
import com.exoticworlds.compat.map.ClientFolds;
import com.exoticworlds.compat.map.FullscreenZoomFloor;
import com.exoticworlds.compat.map.MapCopies;
import com.exoticworlds.compat.map.WorldCopies;
import com.exoticworlds.core.fold.WorldFold;

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

    private static final int COMPARISON_OFFSET_CHUNKS = 16;

    private static final long[] NO_REGIONS = new long[0];

    public static @Nullable WorldFold getBrowsedFold() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapProcessor processor = session == null ? null : session.getMapProcessor();
        MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
        MapDimension dimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
        return dimension == null ? null : ClientFolds.of(dimension.getDimId());
    }

    public static @Nullable BlockPos foldIdSpawn(ClientLevel level, @Nullable BlockPos spawn) {
        return spawn == null ? null : ClientFolds.fold(ClientFolds.of(level), spawn);
    }

    public static List<TileArea> tilePieces(WorldCopies.Window window, int rawTileX, int rawTileZ) {
        int firstChunkX = rawTileX * TILE_CHUNK_CHUNKS;
        int firstChunkZ = rawTileZ * TILE_CHUNK_CHUNKS;
        int minX = chunkBlock(firstChunkX);
        int minZ = chunkBlock(firstChunkZ);
        int maxX = chunkBlock(firstChunkX + TILE_CHUNK_CHUNKS);
        int maxZ = chunkBlock(firstChunkZ + TILE_CHUNK_CHUNKS);
        List<TileArea> areas = new ArrayList<>();
        for (WorldCopies.Piece piece : window.within(minX, minZ, maxX, maxZ).getPieces()) {
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

    public static boolean isTileLeavingWorld(AxisCopies blockCopiesX, AxisCopies blockCopiesZ, int rawTileX,
            int rawTileZ) {
        int tileBlocks = chunkBlock(TILE_CHUNK_CHUNKS);
        return isSpanLeavingWorld(blockCopiesX, rawTileX * tileBlocks, tileBlocks)
                || isSpanLeavingWorld(blockCopiesZ, rawTileZ * tileBlocks, tileBlocks);
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

    public static ChunkPos canonicalChunk(@Nullable WorldFold fold, int chunkX, int chunkZ) {
        return ClientFolds.fold(fold, new ChunkPos(chunkX, chunkZ));
    }

    public static ChunkPos nearestChunk(@Nullable WorldFold fold, ChunkPos reference, int chunkX, int chunkZ) {
        if (fold == null) {
            return new ChunkPos(chunkX, chunkZ);
        }

        BlockPos nearest = fold.nearestCopy(reference.getWorldPosition(), new ChunkPos(chunkX, chunkZ).getWorldPosition());
        return ChunkPos.containing(nearest);
    }

    public static int insideTile(int canonicalChunk) {
        return Math.floorMod(canonicalChunk, TILE_CHUNK_CHUNKS);
    }

    public static int lastInsideTile(AxisCopies chunkCopies, int canonicalChunk) {
        boolean isLastInWorld = chunkCopies.isLooped() && canonicalChunk == chunkCopies.getMax() - 1;
        return isLastInWorld ? TILE_CHUNK_CHUNKS - 1 : insideTile(canonicalChunk);
    }

    public static ChunkPos tileOfChunk(@Nullable WorldFold fold, int chunkX, int chunkZ) {
        ChunkPos canonical = canonicalChunk(fold, chunkX, chunkZ);
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

    public static ChunkPos foldRegion(@Nullable WorldFold fold, int regionX, int regionZ) {
        ChunkPos tile = tileOfChunk(fold, firstTileChunkOfRegion(regionX) * TILE_CHUNK_CHUNKS,
                firstTileChunkOfRegion(regionZ) * TILE_CHUNK_CHUNKS);
        return new ChunkPos(regionOfTileChunk(tile.x()), regionOfTileChunk(tile.z()));
    }

    public static long[] canonicalRegionCorners(@Nullable WorldFold fold, int leveledRegX, int leveledRegZ,
            int level) {
        int sideBlocks = REGION_BLOCKS << level;
        int minX = leveledRegX * sideBlocks;
        int minZ = leveledRegZ * sideBlocks;
        int maxX = minX + sideBlocks - 1;
        int maxZ = minZ + sideBlocks - 1;
        return new long[] {canonicalRegion(fold, minX, minZ, sideBlocks), canonicalRegion(fold, maxX, minZ, sideBlocks),
                canonicalRegion(fold, minX, maxZ, sideBlocks), canonicalRegion(fold, maxX, maxZ, sideBlocks)};
    }

    private static long canonicalRegion(@Nullable WorldFold fold, int blockX, int blockZ, int sideBlocks) {
        BlockPos folded = foldBlock(fold, blockX, blockZ);
        return ChunkPos.pack(Math.floorDiv(folded.getX(), sideBlocks), Math.floorDiv(folded.getZ(), sideBlocks));
    }

    public static ChunkPos foldComparison(@Nullable WorldFold fold, int comparisonX, int comparisonZ) {
        ChunkPos canonical = canonicalChunk(fold, comparisonX + COMPARISON_OFFSET_CHUNKS,
                comparisonZ + COMPARISON_OFFSET_CHUNKS);
        return new ChunkPos(canonical.x() - COMPARISON_OFFSET_CHUNKS, canonical.z() - COMPARISON_OFFSET_CHUNKS);
    }

    public static long[] canonicalRegions(@Nullable WorldFold fold, int startTileChunkX, int startTileChunkZ,
            int endTileChunkX, int endTileChunkZ) {
        return WorldCopies.Window.of(fold,
                chunkBlock(startTileChunkX * TILE_CHUNK_CHUNKS), chunkBlock(startTileChunkZ * TILE_CHUNK_CHUNKS),
                chunkBlock((endTileChunkX + 1) * TILE_CHUNK_CHUNKS), chunkBlock((endTileChunkZ + 1) * TILE_CHUNK_CHUNKS))
                .canonicalCells(REGION_BLOCKS);
    }

    public static long[] canonicalSlotOrigins(WorldCopies.Window window, int viewBlockX, int viewBlockZ, int slotSizeBlocks) {
        long[] slots = slotCells(window, viewBlockX, viewBlockZ, slotSizeBlocks);
        for (int i = 0; i < slots.length; i++) {
            slots[i] = ChunkPos.pack(ChunkPos.getX(slots[i]) * slotSizeBlocks, ChunkPos.getZ(slots[i]) * slotSizeBlocks);
        }

        return slots;
    }

    private static long[] slotCells(WorldCopies.Window window, int viewBlockX, int viewBlockZ, int slotSizeBlocks) {
        int maxX = viewBlockX + slotSizeBlocks;
        int maxZ = viewBlockZ + slotSizeBlocks;
        return window.within(viewBlockX, viewBlockZ, maxX, maxZ).canonicalCells(slotSizeBlocks);
    }

    public static long @Nullable [] frameRegions(int minTileChunkX, int minTileChunkZ, int maxTileChunkX,
            int maxTileChunkZ) {
        return frameRegions(getBrowsedFold(), minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
    }

    static long @Nullable [] frameRegions(@Nullable WorldFold fold, int minTileChunkX, int minTileChunkZ,
            int maxTileChunkX, int maxTileChunkZ) {
        if (fold == null
                || (!isSpanLeavingWorld(AxisCopies.of(fold, Direction.Axis.X), minTileChunkX * SLOT_BLOCKS,
                        (maxTileChunkX + 1 - minTileChunkX) * SLOT_BLOCKS)
                && !isSpanLeavingWorld(AxisCopies.of(fold, Direction.Axis.Z), minTileChunkZ * SLOT_BLOCKS,
                        (maxTileChunkZ + 1 - minTileChunkZ) * SLOT_BLOCKS))) {
            return null;
        }

        return canonicalRegions(fold, minTileChunkX, minTileChunkZ, maxTileChunkX, maxTileChunkZ);
    }

    public static long[] fannedRegions(WorldCopies.Window window, int leveledRegX, int leveledRegZ, int level) {
        WorldFold fold = window.fold();
        int sideBlocks = REGION_BLOCKS << level;
        if (fold == null
                || (!isSpanLeavingWorld(AxisCopies.of(fold, Direction.Axis.X), leveledRegX * sideBlocks, sideBlocks)
                && !isSpanLeavingWorld(AxisCopies.of(fold, Direction.Axis.Z), leveledRegZ * sideBlocks, sideBlocks))) {
            return NO_REGIONS;
        }

        return slotCells(window, leveledRegX * sideBlocks, leveledRegZ * sideBlocks, sideBlocks);
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

    public static List<TilePlacement> placements(int originX, int originZ, int sizeBlocks, List<WorldCopies.Copy> copies) {
        return placements(getBrowsedFold(), originX, originZ, sizeBlocks, copies);
    }

    public static List<TilePlacement> placements(@Nullable WorldFold fold, int originX, int originZ, int sizeBlocks,
            List<WorldCopies.Copy> copies) {
        AxisCopies x = AxisCopies.of(fold, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(fold, Direction.Axis.Z);
        int minX = x.clipMin(originX);
        int maxX = x.clipMax(originX + sizeBlocks);
        int minZ = z.clipMin(originZ);
        int maxZ = z.clipMax(originZ + sizeBlocks);
        if (minX >= maxX || minZ >= maxZ) {
            return List.of();
        }

        float u1 = (float) (minX - originX) / sizeBlocks;
        float u2 = (float) (maxX - originX) / sizeBlocks;
        float v1 = (float) (minZ - originZ) / sizeBlocks;
        float v2 = (float) (maxZ - originZ) / sizeBlocks;
        List<TilePlacement> placements = new ArrayList<>(copies.size());
        for (WorldCopies.Copy copy : copies) {
            placements.add(new TilePlacement(minX + copy.dx(), minZ + copy.dz(), maxX - minX, maxZ - minZ,
                    u1, v1, u2, v2));
        }

        return placements;
    }

    public record TilePlacement(int minX, int minZ, int widthBlocks, int heightBlocks, float u1, float v1, float u2, float v2) {
        public boolean isMeeting(int spanMinX, int spanMinZ, int spanMaxX, int spanMaxZ) {
            return this.minX < spanMaxX && this.minX + this.widthBlocks > spanMinX
                    && this.minZ < spanMaxZ && this.minZ + this.heightBlocks > spanMinZ;
        }
    }

    public static boolean isSpanLeavingWorld(AxisCopies copies, int first, int size) {
        int end = first + size;
        return copies.clipMin(first) != first || copies.clipMax(end) != end;
    }

    public static BlockPos foldBlock(int blockX, int blockZ) {
        return foldBlock(getBrowsedFold(), blockX, blockZ);
    }

    public static BlockPos foldBlock(@Nullable WorldFold fold, int blockX, int blockZ) {
        return ClientFolds.fold(fold, new BlockPos(blockX, 0, blockZ));
    }

    public static boolean isGlueableAt(@Nullable WorldFold fold, int slotSizeBlocks) {
        if (fold == null) {
            return false;
        }

        AxisCopies x = AxisCopies.of(fold, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(fold, Direction.Axis.Z);
        if ((x.isLooped() && Math.floorMod(x.getMin(), slotSizeBlocks) != 0)
                || (z.isLooped() && Math.floorMod(z.getMin(), slotSizeBlocks) != 0)) {
            return false;
        }

        List<WorldCopies.Copy> neighbors = WorldCopies.meeting(fold,
                x.isLooped() ? x.getMin() - x.getWidth() : 0, z.isLooped() ? z.getMin() - z.getWidth() : 0,
                x.isLooped() ? x.getMax() + x.getWidth() : 1, z.isLooped() ? z.getMax() + z.getWidth() : 1);
        for (WorldCopies.Copy copy : neighbors) {
            if (Math.floorMod(copy.dx(), slotSizeBlocks) != 0 || Math.floorMod(copy.dz(), slotSizeBlocks) != 0) {
                return false;
            }
        }

        return true;
    }

    public static List<WorldCopies.Copy> drawnCopies(int[] spanX, int[] spanZ, MapCopies mapCopies) {
        return drawnCopies(getBrowsedFold(), spanX, spanZ, mapCopies);
    }

    public static List<WorldCopies.Copy> drawnCopies(@Nullable WorldFold fold, int[] spanX, int[] spanZ,
            MapCopies mapCopies) {
        return drawnCopies(WorldCopies.meeting(fold, spanX[0], spanZ[0], spanX[1], spanZ[1]), mapCopies);
    }

    public static List<WorldCopies.Copy> drawnCopies(List<WorldCopies.Copy> copies, MapCopies mapCopies) {
        if (mapCopies != MapCopies.SINGLE) {
            return copies;
        }

        return copies.contains(WorldCopies.IDENTITY) ? List.of(WorldCopies.IDENTITY) : List.of();
    }

    public static double zoomFloorScale(double scaleMultiplier, MapCopies mapCopies, int windowWidthPixels, int windowHeightPixels) {
        WorldFold fold = getBrowsedFold();
        if (fold == null) {
            return 0.0;
        }

        return mapCopies == MapCopies.SINGLE
                ? FullscreenZoomFloor.xaeroCoverScale(fold, scaleMultiplier, windowWidthPixels, windowHeightPixels)
                : FullscreenZoomFloor.xaeroScale(fold, scaleMultiplier);
    }

    public static Vec3 foldFootprint(ClientLevel level, double blockX, double blockZ) {
        return ClientFolds.fold(ClientFolds.of(level), blockX, blockZ);
    }

    private XaeroWorldMapFold() {
    }
}
