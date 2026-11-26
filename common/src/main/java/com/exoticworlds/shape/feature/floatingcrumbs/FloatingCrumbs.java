package com.exoticworlds.shape.feature.floatingcrumbs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.FeatureStateHolder;
import com.exoticworlds.accessors.ChunkWriteListenerHolder;
import com.exoticworlds.core.feature.CarvedChunkFeature;
import com.exoticworlds.core.feature.ChunkDataFeature;
import com.exoticworlds.core.feature.LitChunkFeature;
import com.exoticworlds.core.world.CoordinateConstants;
import com.exoticworlds.core.fold.FoldOrientation;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.gen.terrain.TerrainCeiling;
import com.exoticworlds.shape.feature.ShapeFeature;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.material.FluidState;

public final class FloatingCrumbs extends ShapeFeature
        implements CarvedChunkFeature, LitChunkFeature, ChunkDataFeature {
    public static final String KEY = "floating_crumbs";

    private static final String DATA_KEY = "terrain_mask";

    private static final int WINDOW_CHUNKS = 3;

    private static final int WINDOW_WIDTH_BLOCKS = WINDOW_CHUNKS * CoordinateConstants.CHUNK_WIDTH_BLOCKS;

    private static final int CENTER_INDEX = WINDOW_CHUNKS + 1;

    private static final int CLEARED_SEED = 64;

    private static final long[] NO_POSITIONS = new long[0];

    private static final int KEPT = 0;

    private static final class Window {
        private final GenerationChunkHolder[] holders;
        private final long[] keys;
        private final TerrainMask[] masks;

        private Window(GenerationChunkHolder[] holders, long[] keys, TerrainMask[] masks) {
            this.holders = holders;
            this.keys = keys;
            this.masks = masks;
        }
    }

    private static final class Cleared {
        private final CellGrid grid;
        private final int lowestY;
        private int[] cells = new int[CLEARED_SEED];
        private int count;

        private Cleared(CellGrid grid, int lowestY) {
            this.grid = grid;
            this.lowestY = lowestY;
        }

        private void add(int cell) {
            if (this.count == this.cells.length) {
                this.cells = Arrays.copyOf(this.cells, this.cells.length * 2);
            }

            this.cells[this.count++] = cell;
        }
    }

    private static final class CrumbSearch {
        private final TerrainMask[] window;
        private final FoldOrientation @Nullable [] frames;
        private final BitSet taken;
        private final BitSet rejected;
        private final int[] pending;
        private int tail;
        private boolean isKept;

        private CrumbSearch(TerrainMask[] window, FoldOrientation @Nullable [] frames, BitSet taken,
                BitSet rejected, int[] pending) {
            this.window = window;
            this.frames = frames;
            this.taken = taken;
            this.rejected = rejected;
            this.pending = pending;
        }
    }

    public static final FloatingCrumbs FEATURE = new FloatingCrumbs();

    private FloatingCrumbs() {
        super(KEY);
    }

    @Override
    public boolean isInstalledOn(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator noise
                && TerrainCeiling.ceiling(noise.generatorSettings().value()) != null;
    }

    @Override
    public void atCarved(ServerLevel level, ChunkAccess chunk) {
        sweep(level, chunk);
        registerMask(level, chunk);
    }

    @Override
    public void atLit(ServerLevel level, ChunkAccess chunk, StaticCache2D<GenerationChunkHolder> chunks,
            Executor mainThread) {
        TerrainMasks masks = getTerrainMasks(level);
        Window window = windowAround(masks, chunks, chunk.getPos());
        // The LIGHT step declares no write radius, so a region on it reports every read of a neighbor as unsafe.
        sweepWindow(chunk, window.masks, () -> new WorldGenRegion(level, chunks,
                ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), chunk));
        markRead(masks, window, mainThread);
    }

    @Override
    public String getDataKey() {
        return DATA_KEY;
    }

    @Override
    public @Nullable CompoundTag getSavedData(ServerLevel level, ChunkPos pos) {
        return getTerrainMasks(level).getSaved(pos);
    }

    @Override
    public void restoreData(ServerLevel level, ChunkAccess chunk, CompoundTag data) {
        TerrainMask mask = getTerrainMasks(level).restore(chunk.getPos(), data, chunk.getMinY(), chunk.getHeight(),
                getTurningFold(level));
        if (mask != null) {
            attachTerrainMask(chunk, mask);
        }
    }

    @Override
    public void releaseData(ServerLevel level, ChunkPos pos) {
        getTerrainMasks(level).release(pos);
    }

    private static TerrainMasks getTerrainMasks(ServerLevel level) {
        return ((FeatureStateHolder) level).exotic_worlds$featureState(FEATURE, TerrainMasks::new);
    }

    private static @Nullable WorldFold getTurningFold(ServerLevel level) {
        WorldFold fold = WorldLoopAttachments.getFold(level);
        return fold.isPreservingLocalIndices() ? null : fold;
    }

    public static @Nullable TerrainMask getTerrainMask(ChunkAccess chunk) {
        return chunk instanceof ChunkWriteListenerHolder holder
                && holder.exotic_worlds$getWriteListener() instanceof TerrainMask mask ? mask : null;
    }

    private static void attachTerrainMask(ChunkAccess chunk, TerrainMask mask) {
        if (chunk instanceof ChunkWriteListenerHolder holder) {
            holder.exotic_worlds$setWriteListener(mask);
        }
    }

    public static void sweep(ServerLevel level, ChunkAccess chunk) {
        int minY = chunk.getMinY();
        CellGrid grid = new CellGrid(CoordinateConstants.CHUNK_WIDTH_BLOCKS, chunk.getHeight());
        boolean[] solid = new boolean[grid.getCellCount()];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        int solidBlocks = CrumbFlood.fill(chunk, grid, solid, fluid, fluids, minY);
        if (solidBlocks > 0) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            CrumbFlood.clearCrumbs(grid, solid, fluid, solidBlocks,
                    cell -> write(chunk, grid, cursor, cell, minY, CrumbFlood.getBlock(fluids, fluid[cell])));
        }

        attachTerrainMask(chunk, TerrainMask.of(solid, chunk.getPos(), minY, grid, getTurningFold(level)));
    }

    private static void registerMask(ServerLevel level, ChunkAccess chunk) {
        TerrainMask mask = getTerrainMask(chunk);
        if (mask != null) {
            getTerrainMasks(level).put(chunk.getPos(), mask);
        }
    }

    private static Window windowAround(TerrainMasks masks, StaticCache2D<GenerationChunkHolder> chunks,
            ChunkPos center) {
        GenerationChunkHolder[] holders = new GenerationChunkHolder[WINDOW_CHUNKS * WINDOW_CHUNKS];
        long[] keys = new long[holders.length];
        TerrainMask[] window = new TerrainMask[keys.length];
        for (int stepZ = -1; stepZ <= 1; stepZ++) {
            for (int stepX = -1; stepX <= 1; stepX++) {
                int index = (stepZ + 1) * WINDOW_CHUNKS + stepX + 1;
                holders[index] = chunks.get(center.x() + stepX, center.z() + stepZ);
                keys[index] = holders[index].getPos().pack();
                window[index] = masks.getMask(keys[index]);
            }
        }

        return new Window(holders, keys, window);
    }

    private static void markRead(TerrainMasks masks, Window window, Executor mainThread) {
        List<GenerationChunkHolder> read = new ArrayList<>(window.keys.length);
        for (int index = 0; index < window.keys.length; index++) {
            if (masks.tryConsume(window.keys[index])) {
                read.add(window.holders[index]);
            }
        }

        if (!read.isEmpty()) {
            // A LevelChunk's markUnsaved reaches ChunkMap.setChunkUnsaved, whose set only the server thread may touch.
            mainThread.execute(() -> markUnsaved(read));
        }
    }

    private static void markUnsaved(List<GenerationChunkHolder> holders) {
        for (GenerationChunkHolder holder : holders) {
            ChunkAccess latest = holder.getLatestChunk();
            if (latest != null) {
                latest.markUnsaved();
            }
        }
    }

    static long[] sweepWindow(ChunkAccess chunk, TerrainMask[] window, Supplier<LevelReader> reader) {
        Cleared cleared = crumbCells(window);
        if (cleared == null || cleared.count == 0) {
            return NO_POSITIONS;
        }

        TerrainMask center = window[CENTER_INDEX];
        CellGrid grid = cleared.grid;
        int[] cells = new int[cleared.count];
        for (int i = 0; i < cleared.count; i++) {
            int cell = cleared.cells[i];
            cells[i] = center.cellOf(centerLocalX(grid, cell), cleared.lowestY + grid.layer(cell),
                    centerLocalZ(grid, cell));
        }

        return clearInChunk(chunk, cells, cleared.count, reader.get());
    }

    public static long[] crumbPositions(TerrainMask[] masks) {
        Cleared cleared = crumbCells(masks);
        if (cleared == null) {
            return NO_POSITIONS;
        }

        ChunkPos pos = masks[CENTER_INDEX].getPos();
        CellGrid grid = cleared.grid;
        long[] positions = new long[cleared.count];
        for (int i = 0; i < cleared.count; i++) {
            int cell = cleared.cells[i];
            positions[i] = BlockPos.asLong(pos.getMinBlockX() + centerLocalX(grid, cell),
                    cleared.lowestY + grid.layer(cell),
                    pos.getMinBlockZ() + centerLocalZ(grid, cell));
        }

        return positions;
    }

    public static long[] clearBorderCrumbs(ChunkAccess chunk, TerrainMask[] masks, LevelReader reader) {
        return sweepWindow(chunk, masks, () -> reader);
    }

    private static @Nullable Cleared crumbCells(TerrainMask[] window) {
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;
        for (TerrainMask mask : window) {
            if (mask == null) {
                return null;
            }

            if (!mask.isEmpty()) {
                lowestY = Math.min(lowestY, mask.getLowestY());
                highestY = Math.max(highestY, mask.getHighestY());
            }
        }

        TerrainMask center = window[CENTER_INDEX];
        if (lowestY > highestY || center.isEmpty()) {
            return null;
        }

        CellGrid grid = new CellGrid(WINDOW_WIDTH_BLOCKS, highestY - lowestY + 1);
        BitSet taken = new BitSet(grid.getCellCount());
        BitSet rejected = new BitSet(grid.getCellCount());
        int[] pending = new int[CrumbFlood.SMALLEST_KEPT_BLOCKS];
        Cleared cleared = new Cleared(grid, lowestY);
        CrumbSearch search = new CrumbSearch(window, center.windowFrames(WINDOW_CHUNKS), taken, rejected, pending);

        for (int y = center.getLowestY(); y <= center.getHighestY(); y++) {
            for (int localZ = 0; localZ < CoordinateConstants.CHUNK_WIDTH_BLOCKS; localZ++) {
                for (int localX = 0; localX < CoordinateConstants.CHUNK_WIDTH_BLOCKS; localX++) {
                    if (!center.isSolidAt(localX, y, localZ)) {
                        continue;
                    }

                    int start = grid.cell(CoordinateConstants.CHUNK_WIDTH_BLOCKS + localX, y - lowestY,
                            CoordinateConstants.CHUNK_WIDTH_BLOCKS + localZ);
                    if (!taken.get(start)) {
                        takeCrumb(search, cleared, start);
                    }
                }
            }
        }

        return cleared;
    }

    private static int centerLocalX(CellGrid window, int cell) {
        return window.localX(cell) - CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    }

    private static int centerLocalZ(CellGrid window, int cell) {
        return window.localZ(cell) - CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    }

    private static void takeCrumb(CrumbSearch search, Cleared cleared, int start) {
        markCleared(search, cleared, gatherCrumb(search, cleared, start));
    }

    private static int gatherCrumb(CrumbSearch search, Cleared cleared, int start) {
        int[] pending = search.pending;
        CellGrid grid = cleared.grid;
        search.taken.set(start);
        pending[0] = start;
        search.tail = 1;
        search.isKept = false;
        int head = 0;

        while (head < search.tail && !search.isKept) {
            int cell = pending[head++];
            if (grid.isOnSide(cell)) {
                search.isKept = true;
                break;
            }

            spreadFrom(search, cleared, cell);
        }

        int tail = search.tail;
        if (search.isKept || tail >= CrumbFlood.SMALLEST_KEPT_BLOCKS) {
            for (int i = 0; i < tail; i++) {
                search.rejected.set(pending[i]);
            }

            return KEPT;
        }

        return tail;
    }

    private static void spreadFrom(CrumbSearch search, Cleared cleared, int cell) {
        BitSet taken = search.taken;
        int[] pending = search.pending;
        CellGrid grid = cleared.grid;
        for (int axis = 0; axis < CellGrid.AXES && !search.isKept; axis++) {
            for (int step = -1; step <= 1; step += 2) {
                int neighbor = grid.neighbor(cell, axis, step);
                if (neighbor == CellGrid.NO_CELL || !isWindowSolid(search, grid, neighbor, cleared.lowestY)) {
                    continue;
                }

                if (search.rejected.get(neighbor) || search.tail == pending.length) {
                    search.isKept = true;
                    break;
                }

                if (!taken.get(neighbor)) {
                    taken.set(neighbor);
                    pending[search.tail++] = neighbor;
                }
            }
        }
    }

    private static void markCleared(CrumbSearch search, Cleared cleared, int size) {
        int[] pending = search.pending;
        CellGrid grid = cleared.grid;
        TerrainMask center = search.window[CENTER_INDEX];
        for (int i = 0; i < size; i++) {
            int cell = pending[i];
            if (chunkIndexOf(grid, cell) == CENTER_INDEX
                    && center.isUntouchedAt(centerLocalX(grid, cell), cleared.lowestY + grid.layer(cell),
                            centerLocalZ(grid, cell))) {
                cleared.add(cell);
            }
        }
    }

    private static long[] clearInChunk(ChunkAccess chunk, int[] cells, int count, LevelReader reader) {
        CellGrid grid = new CellGrid(CoordinateConstants.CHUNK_WIDTH_BLOCKS, chunk.getHeight());
        LongArrayList written = writeCleared(chunk, grid, cells, count);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            clearColumn(chunk, reader, positionOf(chunk, grid, cursor, cells[i], chunk.getMinY()), written);
        }

        return written.toLongArray();
    }

    private static LongArrayList writeCleared(ChunkAccess chunk, CellGrid grid, int[] cells, int count) {
        int minY = chunk.getMinY();
        boolean[] solid = new boolean[grid.getCellCount()];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        CrumbFlood.fill(chunk, grid, solid, fluid, fluids, minY);

        Arrays.sort(cells, 0, count);
        CrumbFlood.floodCleared(grid, fluid, cells, count);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        LongArrayList written = new LongArrayList(count);
        for (int i = 0; i < count; i++) {
            write(chunk, grid, cursor, cells[i], minY, CrumbFlood.getBlock(fluids, fluid[cells[i]]));
            written.add(cursor.asLong());
        }

        return written;
    }

    private static void clearColumn(ChunkAccess chunk, LevelReader reader, BlockPos.MutableBlockPos cursor,
            LongArrayList written) {
        while (cursor.getY() < chunk.getMaxY()) {
            BlockState state = chunk.getBlockState(cursor.move(Direction.UP));
            if (state.isAir() || state.canSurvive(reader, cursor)) {
                return;
            }

            FluidState carried = state.getFluidState();
            chunk.setBlockState(cursor, carried.isEmpty() ? CrumbFlood.AIR : CrumbFlood.sourceBlockOf(carried));
            written.add(cursor.asLong());
        }
    }

    private static void write(ChunkAccess chunk, CellGrid grid, BlockPos.MutableBlockPos cursor, int cell,
            int minY, BlockState state) {
        chunk.setBlockState(positionOf(chunk, grid, cursor, cell, minY), state);
    }

    private static BlockPos.MutableBlockPos positionOf(ChunkAccess chunk, CellGrid grid,
            BlockPos.MutableBlockPos cursor, int cell, int minY) {
        return cursor.set(chunk.getPos().getMinBlockX() + grid.localX(cell), minY + grid.layer(cell),
                chunk.getPos().getMinBlockZ() + grid.localZ(cell));
    }

    private static boolean isWindowSolid(CrumbSearch search, CellGrid grid, int cell, int lowestY) {
        int index = chunkIndexOf(grid, cell);
        TerrainMask mask = search.window[index];
        int localX = grid.localX(cell) % CoordinateConstants.CHUNK_WIDTH_BLOCKS;
        int localZ = grid.localZ(cell) % CoordinateConstants.CHUNK_WIDTH_BLOCKS;
        int y = lowestY + grid.layer(cell);
        FoldOrientation[] frames = search.frames;
        return frames == null ? mask.isSolidAt(localX, y, localZ) : mask.isSolidAt(frames[index], localX, y, localZ);
    }

    private static int chunkIndexOf(CellGrid grid, int cell) {
        return grid.localZ(cell) / CoordinateConstants.CHUNK_WIDTH_BLOCKS * WINDOW_CHUNKS
                + grid.localX(cell) / CoordinateConstants.CHUNK_WIDTH_BLOCKS;
    }

}
