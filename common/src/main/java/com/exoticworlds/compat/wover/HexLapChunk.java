package com.exoticworlds.compat.wover;

import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.level.levelgen.WorldgenRandom;

final class HexLapChunk<T> extends LapChunk<T> {
    private static final int SEED_SPACING = 8;

    private static final int CELLS_PER_SEED = SEED_SPACING * SEED_SPACING;

    private static final int MIN_WRAPPED_SEED_LINES = 2;

    private static final int SUB_BIOME_ODDS = 4;

    private static final int BORDER_SOURCE_STEP = 2;

    private static final int[][] EVEN_ROW_NEIGHBORS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}};

    private static final int[][] ODD_ROW_NEIGHBORS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {-1, 1}, {-1, -1}};

    private final boolean wrapX;

    private final boolean wrapZ;

    private final int zOrigin;

    HexLapChunk(int sideX, int sideZ, boolean wrapX, boolean wrapZ, int zOrigin, WorldgenRandom random,
            LapPicker<T> picker) {
        super(sideX, sideZ);
        this.wrapX = wrapX;
        this.wrapZ = wrapZ;
        this.zOrigin = zOrigin;
        fill(random, picker);
    }

    private void fill(WorldgenRandom random, LapPicker<T> picker) {
        Object[][] buffers = {this.cells, new Object[this.cells.length]};
        seed(buffers[0], random, picker);

        int bufferIndex = 0;
        boolean hasEmptyCells = true;
        while (hasEmptyCells) {
            Object[] in = buffers[bufferIndex];
            bufferIndex ^= 1;
            Object[] out = buffers[bufferIndex];
            hasEmptyCells = false;
            for (int x = interiorStart(this.wrapX); x < interiorEnd(sideX(), this.wrapX); x++) {
                for (int z = interiorStart(this.wrapZ); z < interiorEnd(sideZ(), this.wrapZ); z++) {
                    int index = index(x, z);
                    if (in[index] == null) {
                        hasEmptyCells = true;
                        continue;
                    }

                    out[index] = in[index];
                    int[] step = neighbors(z)[random.nextInt(EVEN_ROW_NEIGHBORS.length)];
                    int side = neighbor(x, z, step);
                    if (side >= 0 && out[side] == null) {
                        out[side] = in[index];
                    }
                }
            }
        }

        if (buffers[bufferIndex] != this.cells) {
            System.arraycopy(buffers[bufferIndex], 0, this.cells, 0, this.cells.length);
        }

        Object[] out = this.cells;
        copyBorders(out);
        for (int x = 0; x < sideX(); x++) {
            for (int z = 0; z < sideZ(); z++) {
                int index = index(x, z);
                if (out[index] == null) {
                    out[index] = picker.pick().apply(random);
                } else if (random.nextInt(SUB_BIOME_ODDS) == 0) {
                    @SuppressWarnings("unchecked")
                    T parent = (T) out[index];
                    circle(out, x, z, picker.subBiome().apply(parent, random), parent);
                }
            }
        }
    }

    private void seed(Object[] buffer, WorldgenRandom random, LapPicker<T> picker) {
        int columns = seedLines(sideX(), this.wrapX);
        int rows = seedLines(sideZ(), this.wrapZ);
        int seeds = seedCount(sideX(), sideZ(), this.wrapX, this.wrapZ);
        IntArrayList slots = new IntArrayList(columns * rows);
        for (int slot = 0; slot < columns * rows; slot++) {
            slots.add(slot);
        }

        while (slots.size() > seeds) {
            slots.removeInt(random.nextInt(slots.size()));
        }

        for (int slot : slots) {
            int x = seedCoordinate(sideX(), columns, slot / rows, random);
            int z = seedCoordinate(sideZ(), rows, slot % rows, random);
            circle(buffer, x, z, picker.pick().apply(random), null);
        }

        for (int extra = slots.size(); extra < seeds; extra++) {
            circle(buffer, random.nextInt(sideX()), random.nextInt(sideZ()), picker.pick().apply(random), null);
        }
    }

    static int seedLines(int side, boolean wraps) {
        int lines = Math.max(1, Math.round((float) side / SEED_SPACING));
        return wraps ? Math.max(lines, Math.min(side, MIN_WRAPPED_SEED_LINES)) : lines;
    }

    static int seedCount(int sideX, int sideZ, boolean wrapX, boolean wrapZ) {
        int floor = wrappedSeedFloor(sideX, wrapX) * wrappedSeedFloor(sideZ, wrapZ);
        return Math.max(floor, Math.round((float) sideX * sideZ / CELLS_PER_SEED));
    }

    private static int wrappedSeedFloor(int side, boolean wraps) {
        return wraps ? Math.min(side, MIN_WRAPPED_SEED_LINES) : 1;
    }

    private static int seedCoordinate(int side, int lines, int line, WorldgenRandom random) {
        int start = side * line / lines;
        int end = side * (line + 1) / lines;
        return start + random.nextInt(end - start);
    }

    private void copyBorders(Object[] buffer) {
        if (!this.wrapZ) {
            for (int x = 0; x < sideX(); x++) {
                buffer[index(x, 0)] = buffer[index(x, BORDER_SOURCE_STEP)];
                buffer[index(x, sideZ() - 1)] = buffer[index(x, sideZ() - 1 - BORDER_SOURCE_STEP)];
            }
        }

        if (!this.wrapX) {
            for (int z = 0; z < sideZ(); z++) {
                buffer[index(0, z)] = buffer[index(BORDER_SOURCE_STEP, z)];
                buffer[index(sideX() - 1, z)] = buffer[index(sideX() - 1 - BORDER_SOURCE_STEP, z)];
            }
        }
    }

    private void circle(Object[] buffer, int x, int z, Object biome, @Nullable Object mask) {
        int center = index(x, z);
        if (buffer[center] == mask) {
            buffer[center] = biome;
        }

        for (int[] step : neighbors(z)) {
            int side = neighbor(x, z, step);
            if (side >= 0 && buffer[side] == mask) {
                buffer[side] = biome;
            }
        }
    }

    private int[][] neighbors(int z) {
        return ((this.zOrigin + z) & 1) == 0 ? EVEN_ROW_NEIGHBORS : ODD_ROW_NEIGHBORS;
    }

    private int neighbor(int x, int z, int[] step) {
        int nx = x + step[0];
        int nz = z + step[1];
        if (this.wrapX) {
            nx = Math.floorMod(nx, sideX());
        } else if (nx < 0 || nx >= sideX()) {
            return -1;
        }

        if (this.wrapZ) {
            nz = Math.floorMod(nz, sideZ());
        } else if (nz < 0 || nz >= sideZ()) {
            return -1;
        }

        return index(nx, nz);
    }

    private static int interiorStart(boolean wraps) {
        return wraps ? 0 : 1;
    }

    private static int interiorEnd(int side, boolean wraps) {
        return wraps ? side : side - 1;
    }
}
