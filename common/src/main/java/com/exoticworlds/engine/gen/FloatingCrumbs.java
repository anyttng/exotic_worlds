package com.exoticworlds.engine.gen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.accessors.CrumbSweepCache;
import com.exoticworlds.accessors.TerrainMaskCache;
import com.exoticworlds.accessors.TerrainMaskHolder;
import com.exoticworlds.api.v1.gen.TerrainSnapshot;
import com.exoticworlds.core.CoordinateConstants;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.noise.TerrainCeiling;

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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.material.FluidState;

public final class FloatingCrumbs {
    private static final int WINDOW_CHUNKS = 3;

    private static final int WINDOW_WIDTH = WINDOW_CHUNKS * CoordinateConstants.CHUNK_WIDTH;

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
        private final BitSet taken;
        private final BitSet rejected;
        private final int[] pending;

        private CrumbSearch(TerrainMask[] window, BitSet taken, BitSet rejected, int[] pending) {
            this.window = window;
            this.taken = taken;
            this.rejected = rejected;
            this.pending = pending;
        }
    }

    public static boolean installedOn(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return ShapedChunkGenerator.wrappedFoldOf(generator) != null
                && generator instanceof NoiseBasedChunkGenerator noise
                && TerrainCeiling.ceiling(noise.generatorSettings().value()) != null;
    }

    private static boolean sweepsCrumbs(ServerLevel level) {
        return ((CrumbSweepCache) level).exotic_worlds$sweepsCrumbs();
    }

    private static TerrainMasks terrainMasksOf(ServerLevel level) {
        return ((TerrainMaskCache) level).exotic_worlds$terrainMasks();
    }

    private static @Nullable TerrainMask terrainMaskOf(ChunkAccess chunk) {
        return chunk instanceof TerrainMaskHolder holder ? holder.exotic_worlds$terrainMask() : null;
    }

    private static void attachTerrainMask(ChunkAccess chunk, TerrainMask mask) {
        if (chunk instanceof TerrainMaskHolder holder) {
            holder.exotic_worlds$terrainMask(mask);
        }
    }

    public static void sweep(ServerLevel level, ChunkAccess chunk) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        int minY = chunk.getMinY();
        CellGrid grid = new CellGrid(CoordinateConstants.CHUNK_WIDTH, chunk.getHeight());
        boolean[] solid = new boolean[grid.cells()];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        int solidBlocks = CrumbFlood.fill(chunk, grid, solid, fluid, fluids, minY);
        if (solidBlocks > 0) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            CrumbFlood.clearCrumbs(grid, solid, fluid, solidBlocks,
                    cell -> write(chunk, grid, cursor, cell, minY, CrumbFlood.blockOf(fluids, fluid[cell])));
        }

        attachTerrainMask(chunk, TerrainMask.of(solid, chunk.getPos(), minY, grid));
    }

    public static void registerMask(ServerLevel level, ChunkAccess chunk) {
        TerrainMask mask = terrainMaskOf(chunk);
        if (mask != null) {
            terrainMasksOf(level).put(chunk.getPos(), mask);
        }
    }

    public static @Nullable CompoundTag savedMask(ServerLevel level, ChunkPos pos) {
        return sweepsCrumbs(level) ? terrainMasksOf(level).saved(pos) : null;
    }

    public static void restoreMask(ServerLevel level, ChunkAccess chunk, CompoundTag saved) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        TerrainMask mask = terrainMasksOf(level).restore(chunk.getPos(), saved, chunk.getMinY(), chunk.getHeight());
        if (mask != null) {
            attachTerrainMask(chunk, mask);
        }
    }

    public static void releaseMask(ServerLevel level, ChunkPos pos) {
        if (sweepsCrumbs(level)) {
            terrainMasksOf(level).release(pos);
        }
    }

    public static void sweepAcross(ServerLevel level, ChunkAccess chunk,
            StaticCache2D<GenerationChunkHolder> chunks, Executor mainThread) {
        if (!sweepsCrumbs(level)) {
            return;
        }

        TerrainMasks masks = terrainMasksOf(level);
        Window window = windowAround(masks, chunks, chunk.getPos());
        // The LIGHT step declares no write radius, so a region on it reports every read of a neighbor as unsafe.
        sweepWindow(chunk, window.masks, () -> new WorldGenRegion(level, chunks,
                ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES), chunk));
        markRead(masks, window, mainThread);
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
                window[index] = masks.at(keys[index]);
            }
        }

        return new Window(holders, keys, window);
    }

    private static void markRead(TerrainMasks masks, Window window, Executor mainThread) {
        List<GenerationChunkHolder> read = new ArrayList<>(window.keys.length);
        for (int index = 0; index < window.keys.length; index++) {
            if (masks.consume(window.keys[index])) {
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

    public static @Nullable TerrainSnapshot snapshotOf(ChunkAccess chunk) {
        return terrainMaskOf(chunk);
    }

    public static long[] crumbPositions(TerrainSnapshot[] window) {
        TerrainMask[] masks = masksOf(window);
        Cleared cleared = crumbCells(masks);
        if (cleared == null) {
            return NO_POSITIONS;
        }

        ChunkPos pos = masks[CENTER_INDEX].pos();
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

    public static long[] clearBorderCrumbs(ChunkAccess chunk, TerrainSnapshot[] window, LevelReader reader) {
        return sweepWindow(chunk, masksOf(window), () -> reader);
    }

    private static TerrainMask[] masksOf(TerrainSnapshot[] window) {
        TerrainMask[] masks = new TerrainMask[window.length];
        for (int i = 0; i < window.length; i++) {
            masks[i] = window[i] instanceof TerrainMask mask ? mask : null;
        }

        return masks;
    }

    private static @Nullable Cleared crumbCells(TerrainMask[] window) {
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;
        for (TerrainMask mask : window) {
            if (mask == null) {
                return null;
            }

            if (!mask.isEmpty()) {
                lowestY = Math.min(lowestY, mask.lowestY());
                highestY = Math.max(highestY, mask.highestY());
            }
        }

        TerrainMask center = window[CENTER_INDEX];
        if (lowestY > highestY || center.isEmpty()) {
            return null;
        }

        CellGrid grid = new CellGrid(WINDOW_WIDTH, highestY - lowestY + 1);
        BitSet taken = new BitSet(grid.cells());
        BitSet rejected = new BitSet(grid.cells());
        int[] pending = new int[CrumbFlood.SMALLEST_KEPT_BLOCKS];
        Cleared cleared = new Cleared(grid, lowestY);
        CrumbSearch search = new CrumbSearch(window, taken, rejected, pending);

        for (int y = center.lowestY(); y <= center.highestY(); y++) {
            for (int localZ = 0; localZ < CoordinateConstants.CHUNK_WIDTH; localZ++) {
                for (int localX = 0; localX < CoordinateConstants.CHUNK_WIDTH; localX++) {
                    if (!center.solidAt(localX, y, localZ)) {
                        continue;
                    }

                    int start = grid.cell(CoordinateConstants.CHUNK_WIDTH + localX, y - lowestY,
                            CoordinateConstants.CHUNK_WIDTH + localZ);
                    if (!taken.get(start)) {
                        takeCrumb(search, cleared, start);
                    }
                }
            }
        }

        return cleared;
    }

    private static int centerLocalX(CellGrid window, int cell) {
        return window.localX(cell) - CoordinateConstants.CHUNK_WIDTH;
    }

    private static int centerLocalZ(CellGrid window, int cell) {
        return window.localZ(cell) - CoordinateConstants.CHUNK_WIDTH;
    }

    private static void takeCrumb(CrumbSearch search, Cleared cleared, int start) {
        markCleared(search, cleared, gatherCrumb(search, cleared, start));
    }

    private static int gatherCrumb(CrumbSearch search, Cleared cleared, int start) {
        BitSet taken = search.taken;
        BitSet rejected = search.rejected;
        int[] pending = search.pending;
        CellGrid grid = cleared.grid;
        taken.set(start);
        pending[0] = start;
        int head = 0;
        int tail = 1;
        boolean kept = false;

        while (head < tail && !kept) {
            int cell = pending[head++];
            if (grid.onSide(cell)) {
                kept = true;
                break;
            }

            for (int axis = 0; axis < CellGrid.AXES && !kept; axis++) {
                for (int step = -1; step <= 1; step += 2) {
                    int neighbor = grid.neighbor(cell, axis, step);
                    if (neighbor == CellGrid.NO_CELL
                            || !windowSolid(search.window, grid, neighbor, cleared.lowestY)) {
                        continue;
                    }

                    if (rejected.get(neighbor) || tail == pending.length) {
                        kept = true;
                        break;
                    }

                    if (!taken.get(neighbor)) {
                        taken.set(neighbor);
                        pending[tail++] = neighbor;
                    }
                }
            }
        }

        if (kept || tail >= CrumbFlood.SMALLEST_KEPT_BLOCKS) {
            for (int i = 0; i < tail; i++) {
                rejected.set(pending[i]);
            }

            return KEPT;
        }

        return tail;
    }

    private static void markCleared(CrumbSearch search, Cleared cleared, int size) {
        int[] pending = search.pending;
        CellGrid grid = cleared.grid;
        TerrainMask center = search.window[CENTER_INDEX];
        for (int i = 0; i < size; i++) {
            int cell = pending[i];
            if (chunkIndexOf(grid, cell) == CENTER_INDEX
                    && center.untouchedAt(centerLocalX(grid, cell), cleared.lowestY + grid.layer(cell),
                            centerLocalZ(grid, cell))) {
                cleared.add(cell);
            }
        }
    }

    private static long[] clearInChunk(ChunkAccess chunk, int[] cells, int count, LevelReader reader) {
        CellGrid grid = new CellGrid(CoordinateConstants.CHUNK_WIDTH, chunk.getHeight());
        LongArrayList written = writeCleared(chunk, grid, cells, count);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            clearColumn(chunk, reader, positionOf(chunk, grid, cursor, cells[i], chunk.getMinY()), written);
        }

        return written.toLongArray();
    }

    private static LongArrayList writeCleared(ChunkAccess chunk, CellGrid grid, int[] cells, int count) {
        int minY = chunk.getMinY();
        boolean[] solid = new boolean[grid.cells()];
        byte[] fluid = new byte[solid.length];
        List<BlockState> fluids = new ArrayList<>();
        CrumbFlood.fill(chunk, grid, solid, fluid, fluids, minY);

        Arrays.sort(cells, 0, count);
        CrumbFlood.floodCleared(grid, fluid, cells, count);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        LongArrayList written = new LongArrayList(count);
        for (int i = 0; i < count; i++) {
            write(chunk, grid, cursor, cells[i], minY, CrumbFlood.blockOf(fluids, fluid[cells[i]]));
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

    private static boolean windowSolid(TerrainMask[] window, CellGrid grid, int cell, int lowestY) {
        return window[chunkIndexOf(grid, cell)].solidAt(grid.localX(cell) % CoordinateConstants.CHUNK_WIDTH,
                lowestY + grid.layer(cell), grid.localZ(cell) % CoordinateConstants.CHUNK_WIDTH);
    }

    private static int chunkIndexOf(CellGrid grid, int cell) {
        return grid.localZ(cell) / CoordinateConstants.CHUNK_WIDTH * WINDOW_CHUNKS
                + grid.localX(cell) / CoordinateConstants.CHUNK_WIDTH;
    }

    private FloatingCrumbs() {
    }
}
