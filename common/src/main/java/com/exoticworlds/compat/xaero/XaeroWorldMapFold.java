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
import com.exoticworlds.compat.MapCopyBudget;
import com.exoticworlds.compat.WorldCopies;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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

    public static Copies worldMapCopies(int minX, int maxX, int minZ, int maxZ) {
        ToroidalShape shape = browsedShape();
        return new Copies(MapCopyBudget.reach(shape, WorldCopies.meeting(shape, minX, minZ, maxX, maxZ)),
                BoundingBox.infinite());
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
        return canonicalChunk(browsedShape(), chunkX, chunkZ);
    }

    static ChunkPos canonicalChunk(@Nullable ToroidalShape shape, int chunkX, int chunkZ) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        return shape == null ? chunk : shape.fold(chunk);
    }

    public static ChunkPos nearestChunk(ChunkPos reference, int chunkX, int chunkZ) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return new ChunkPos(chunkX, chunkZ);
        }

        BlockPos nearest = shape.nearestCopy(reference.getWorldPosition(), new ChunkPos(chunkX, chunkZ).getWorldPosition());
        return new ChunkPos(nearest);
    }

    public static int insideTile(int canonicalChunk) {
        return Math.floorMod(canonicalChunk, TILE_CHUNK_CHUNKS);
    }

    public static int lastInsideTile(AxisCopies chunkCopies, int canonicalChunk) {
        boolean lastInWorld = chunkCopies.loops() && canonicalChunk == chunkCopies.max() - 1;
        return lastInWorld ? TILE_CHUNK_CHUNKS - 1 : insideTile(canonicalChunk);
    }

    public static ChunkPos tileOfChunk(int chunkX, int chunkZ) {
        return tileOfChunk(browsedShape(), chunkX, chunkZ);
    }

    static ChunkPos tileOfChunk(@Nullable ToroidalShape shape, int chunkX, int chunkZ) {
        ChunkPos canonical = canonicalChunk(shape, chunkX, chunkZ);
        return new ChunkPos(Math.floorDiv(canonical.x, TILE_CHUNK_CHUNKS),
                Math.floorDiv(canonical.z, TILE_CHUNK_CHUNKS));
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

    public static ChunkPos foldRegion(int regionX, int regionZ) {
        return foldRegion(browsedShape(), regionX, regionZ);
    }

    static ChunkPos foldRegion(@Nullable ToroidalShape shape, int regionX, int regionZ) {
        ChunkPos tile = tileOfChunk(shape, firstTileChunkOfRegion(regionX) * TILE_CHUNK_CHUNKS,
                firstTileChunkOfRegion(regionZ) * TILE_CHUNK_CHUNKS);
        return new ChunkPos(regionOfTileChunk(tile.x), regionOfTileChunk(tile.z));
    }

    public static ChunkPos foldComparison(int comparisonX, int comparisonZ) {
        return foldComparison(browsedShape(), comparisonX, comparisonZ);
    }

    static ChunkPos foldComparison(@Nullable ToroidalShape shape, int comparisonX, int comparisonZ) {
        ChunkPos canonical = canonicalChunk(shape, comparisonX + COMPARISON_CHUNK_OFFSET,
                comparisonZ + COMPARISON_CHUNK_OFFSET);
        return new ChunkPos(canonical.x - COMPARISON_CHUNK_OFFSET, canonical.z - COMPARISON_CHUNK_OFFSET);
    }

    public static long[] canonicalRegions(int startTileChunkX, int startTileChunkZ, int endTileChunkX,
            int endTileChunkZ) {
        return canonicalRegions(browsedShape(), startTileChunkX, startTileChunkZ, endTileChunkX, endTileChunkZ);
    }

    static long[] canonicalRegions(@Nullable ToroidalShape shape, int startTileChunkX, int startTileChunkZ,
            int endTileChunkX, int endTileChunkZ) {
        long[] regions = gridOrigins(shape,
                chunkBlock(startTileChunkX * TILE_CHUNK_CHUNKS), chunkBlock(startTileChunkZ * TILE_CHUNK_CHUNKS),
                chunkBlock((endTileChunkX + 1) * TILE_CHUNK_CHUNKS), chunkBlock((endTileChunkZ + 1) * TILE_CHUNK_CHUNKS),
                REGION_BLOCKS);
        for (int i = 0; i < regions.length; i++) {
            regions[i] = ChunkPos.asLong(Math.floorDiv(ChunkPos.getX(regions[i]), REGION_BLOCKS),
                    Math.floorDiv(ChunkPos.getZ(regions[i]), REGION_BLOCKS));
        }

        return regions;
    }

    public static long[] canonicalSlotOrigins(int viewBlockX, int viewBlockZ, int slotSize) {
        return canonicalSlotOrigins(browsedShape(), viewBlockX, viewBlockZ, slotSize);
    }

    static long[] canonicalSlotOrigins(@Nullable ToroidalShape shape, int viewBlockX, int viewBlockZ, int slotSize) {
        return gridOrigins(shape, viewBlockX, viewBlockZ, viewBlockX + slotSize, viewBlockZ + slotSize, slotSize);
    }

    private static long[] gridOrigins(@Nullable ToroidalShape shape, int minX, int minZ, int maxX, int maxZ,
            int cell) {
        LongLinkedOpenHashSet origins = new LongLinkedOpenHashSet();
        for (WorldCopies.Piece piece : WorldCopies.pieces(shape, minX, minZ, maxX, maxZ)) {
            for (int x = Math.floorDiv(piece.minX(), cell) * cell; x < piece.maxX(); x += cell) {
                for (int z = Math.floorDiv(piece.minZ(), cell) * cell; z < piece.maxZ(); z += cell) {
                    origins.add(ChunkPos.asLong(x, z));
                }
            }
        }

        return origins.toLongArray();
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

    public static long[] fannedRegions(int leveledRegX, int leveledRegZ, int level) {
        return fannedRegions(browsedShape(), leveledRegX, leveledRegZ, level);
    }

    static long[] fannedRegions(@Nullable ToroidalShape shape, int leveledRegX, int leveledRegZ, int level) {
        int side = REGION_BLOCKS << level;
        if (shape == null
                || (!spanLeavesWorld(AxisCopies.of(shape, Direction.Axis.X), leveledRegX * side, side)
                && !spanLeavesWorld(AxisCopies.of(shape, Direction.Axis.Z), leveledRegZ * side, side))) {
            return NO_REGIONS;
        }

        long[] regions = canonicalSlotOrigins(shape, leveledRegX * side, leveledRegZ * side, side);
        for (int i = 0; i < regions.length; i++) {
            regions[i] = ChunkPos.asLong(Math.floorDiv(ChunkPos.getX(regions[i]), side),
                    Math.floorDiv(ChunkPos.getZ(regions[i]), side));
        }

        return regions;
    }

    public static long[] fannedLeaves(int leveledRegX, int leveledRegZ, int level) {
        return fannedLeaves(browsedShape(), leveledRegX, leveledRegZ, level);
    }

    static long[] fannedLeaves(@Nullable ToroidalShape shape, int leveledRegX, int leveledRegZ, int level) {
        int leaves = 1 << level;
        long[] regions = fannedRegions(shape, leveledRegX, leveledRegZ, level);
        long[] fanned = new long[regions.length * leaves * leaves];
        int next = 0;
        for (long region : regions) {
            for (int leafX = 0; leafX < leaves; leafX++) {
                for (int leafZ = 0; leafZ < leaves; leafZ++) {
                    fanned[next++] = ChunkPos.asLong(ChunkPos.getX(region) * leaves + leafX,
                            ChunkPos.getZ(region) * leaves + leafZ);
                }
            }
        }

        return fanned;
    }

    public static List<TilePlacement> placements(int originX, int originZ, int size, List<WorldCopies.Copy> copies) {
        return placements(browsedShape(), originX, originZ, size, copies);
    }

    static List<TilePlacement> placements(@Nullable ToroidalShape shape, int originX, int originZ, int size,
            List<WorldCopies.Copy> copies) {
        AxisCopies x = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.Z);
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

    static BlockPos foldBlock(@Nullable ToroidalShape shape, int blockX, int blockZ) {
        BlockPos pos = new BlockPos(blockX, 0, blockZ);
        return shape == null ? pos : shape.fold(pos);
    }

    public static boolean glueableAt(int slotSizeBlocks) {
        return glueableAt(browsedShape(), slotSizeBlocks);
    }

    static boolean glueableAt(@Nullable ToroidalShape shape, int slotSizeBlocks) {
        if (shape == null) {
            return false;
        }

        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        if ((x.loops() && Math.floorMod(x.min(), slotSizeBlocks) != 0)
                || (z.loops() && Math.floorMod(z.min(), slotSizeBlocks) != 0)) {
            return false;
        }

        List<WorldCopies.Copy> neighbours = WorldCopies.meeting(shape,
                x.loops() ? x.min() - x.width() : 0, z.loops() ? z.min() - z.width() : 0,
                x.loops() ? x.max() + x.width() : 1, z.loops() ? z.max() + z.width() : 1);
        for (WorldCopies.Copy copy : neighbours) {
            if (Math.floorMod(copy.dx(), slotSizeBlocks) != 0 || Math.floorMod(copy.dz(), slotSizeBlocks) != 0) {
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

    public static Vec3 foldPoint(double blockX, double blockZ) {
        return foldPoint(browsedShape(), blockX, blockZ);
    }

    public static Vec3 foldFootprint(ClientLevel level, double blockX, double blockZ) {
        return foldPoint(ClientShapes.of(level), blockX, blockZ);
    }

    static Vec3 foldPoint(@Nullable ToroidalShape shape, double blockX, double blockZ) {
        Vec3 point = new Vec3(blockX, 0.0, blockZ);
        return shape == null ? point : shape.fold(point);
    }

    private XaeroWorldMapFold() {
    }
}
