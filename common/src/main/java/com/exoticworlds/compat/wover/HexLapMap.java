package com.exoticworlds.compat.wover;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class HexLapMap<T> extends LapMap<T> {
    static final int CHUNK_SIDE = 32;

    static final float RAD_INNER = (float) Math.sqrt(3.0) * 0.5F;

    private static final float COEF = 0.25F * (float) Math.sqrt(3.0);

    private static final float COEF_HALF = COEF * 0.5F;

    private static final float HEXAGON_STRETCH = 1.1555F;

    private static final float HEXAGON_EDGE_WEIGHT = 0.25F;

    private static final float ROW_CORE = 0.3333F;

    private static final double ROW_OFFSET = 0.5;

    private static final double CELL_CENTRE = 0.5;

    private static final double WARP = 0.2;

    private static final int MAX_WARP_OCTAVES = 5;

    private static final int CRENEL_SHIFT = 2;

    private static final int SEED_X_FACTOR = 374761393;

    private static final int SEED_Z_FACTOR = 668265263;

    private static final int SEED_MIX_SHIFT = 13;

    private static final int SEED_MIX_FACTOR = 1274126177;

    private static final int SEED_FINAL_SHIFT = 16;

    private final int seed;

    private final LapPicker<T> picker;

    private final LapAxis x;

    private final LapAxis z;

    private final OpenSimplexStandIn[] warps;

    private final int warpOctaves;

    public HexLapMap(WorldFold fold, float scale, double factor, int seed, LapPicker<T> picker) {
        super(fold, factor);
        double cellScale = scale / factor;
        this.seed = seed;
        this.picker = picker;
        this.x = LapAxis.of(fold.blockDomain(Direction.Axis.X), cellScale / RAD_INNER, false, CHUNK_SIDE);
        this.z = LapAxis.of(fold.blockDomain(Direction.Axis.Z), cellScale, true, CHUNK_SIDE);
        this.warps = new OpenSimplexStandIn[] {
                new OpenSimplexStandIn((long) seed << 1), new OpenSimplexStandIn(((long) seed << 1) | 1L)};
        this.warpOctaves = (int) Math.min(Math.ceil(Math.log(cellScale) / Math.log(2.0)), MAX_WARP_OCTAVES);
    }

    @Override
    public T biomeAt(double blockX, double blockZ) {
        double px = this.x.toLattice(blockX);
        double pz = this.z.toLattice(blockZ);
        double dx = warp(px, pz, 0, this.x.period(), this.z.period()) * WARP;
        double dz = warp(pz, px, 1, this.z.period(), this.x.period()) * WARP;
        px += dx;
        pz += dz;

        int cellZ = Mth.floor(pz);
        boolean offset = (cellZ & 1) == 1;
        if (offset) {
            px += ROW_OFFSET;
        }

        int cellX = Mth.floor(px);
        float pointX = (float) (px - cellX - CELL_CENTRE);
        float pointZ = (float) (pz - cellZ - CELL_CENTRE);
        if (Math.abs(pointZ) < ROW_CORE || insideHexagon(pointZ * RAD_INNER, pointX)) {
            return cellBiome(cellX, cellZ);
        }

        cellX = pointX < 0.0F ? (offset ? cellX - 1 : cellX) : (offset ? cellX : cellX + 1);
        cellZ = pointZ < 0.0F ? cellZ - 1 : cellZ + 1;
        return cellBiome(cellX, cellZ);
    }

    @Override
    LapAxis axis(Direction.Axis axis) {
        return axis == Direction.Axis.X ? this.x : this.z;
    }

    @Override
    LapChunk<T> buildChunk(int kx, int kz) {
        return new HexLapChunk<>(this.x.sideOf(kx), this.z.sideOf(kz), this.x.wraps(), this.z.wraps(),
                this.z.startOf(kz), new WorldgenRandom(RandomSource.create(chunkSeed(this.seed, kx, kz))),
                this.picker);
    }

    private double warp(double a, double b, int state, double aPeriod, double bPeriod) {
        double result = 0.0;
        for (int octave = 1; octave <= this.warpOctaves; octave++) {
            result += this.warps[state].eval(a * octave, b * octave, aPeriod * octave, bPeriod * octave) / octave;
            state ^= 1;
        }

        return result;
    }

    private T cellBiome(int cellX, int cellZ) {
        int gx = this.x.wrapCell(cellX);
        int gz = this.z.wrapCell(cellZ);
        int kx = this.x.chunkOf(gx);
        int kz = this.z.chunkOf(gz);
        int lx = gx - this.x.startOf(kx);
        int lz = gz - this.z.startOf(kz);
        if (((gz >> CRENEL_SHIFT) & 1) == 0 && this.x.isBorder(kx, lx)) {
            kx = this.x.next(kx);
            lx = 0;
        } else if (((gx >> CRENEL_SHIFT) & 1) == 0 && this.z.isBorder(kz, lz)) {
            kz = this.z.next(kz);
            lz = 0;
        }

        return chunk(kx, kz).get(lx, lz);
    }

    private static int chunkSeed(int seed, int kx, int kz) {
        int hash = seed + kx * SEED_X_FACTOR + kz * SEED_Z_FACTOR;
        hash = (hash ^ hash >> SEED_MIX_SHIFT) * SEED_MIX_FACTOR;
        return hash ^ hash >> SEED_FINAL_SHIFT;
    }

    private static boolean insideHexagon(float x, float z) {
        double dx = Math.abs(x) / HEXAGON_STRETCH;
        double dz = Math.abs(z) / HEXAGON_STRETCH;
        return dz <= COEF && COEF * dx + HEXAGON_EDGE_WEIGHT * dz <= COEF_HALF;
    }
}
