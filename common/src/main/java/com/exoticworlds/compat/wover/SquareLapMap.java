package com.exoticworlds.compat.wover;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class SquareLapMap<T> extends LapMap<T> {
    static final int CHUNK_SIDE = 16;

    private static final double WARP_RATE = 0.2;

    private static final int DEPTH_OFFSET = 2;

    private static final int CRENEL_SHIFT = 1;

    private final LapPicker<T> picker;

    private final LapAxis x;

    private final LapAxis z;

    private final OpenSimplexStandIn noiseX;

    private final OpenSimplexStandIn noiseZ;

    private final double cellBlocks;

    private final int depth;

    public SquareLapMap(WorldFold fold, int sizeXZ, double factor, long seed, LapPicker<T> picker) {
        super(fold, factor);
        this.picker = picker;
        this.cellBlocks = sizeXZ / factor;
        this.x = LapAxis.of(fold.blockDomain(Direction.Axis.X), this.cellBlocks, false, CHUNK_SIDE);
        this.z = LapAxis.of(fold.blockDomain(Direction.Axis.Z), this.cellBlocks, false, CHUNK_SIDE);
        this.noiseX = new OpenSimplexStandIn(seed << 1);
        this.noiseZ = new OpenSimplexStandIn((seed << 1) | 1L);
        this.depth = Math.max(0, (int) Math.ceil(Math.log(this.cellBlocks) / Math.log(2.0)) - DEPTH_OFFSET);
    }

    @Override
    public T biomeAt(double blockX, double blockZ) {
        double latticeX = this.x.toLattice(blockX);
        double latticeZ = this.z.toLattice(blockZ);
        double size = 1 << this.depth;
        double px = latticeX * size;
        double pz = latticeZ * size;
        double noiseScale = this.cellBlocks * WARP_RATE;
        double noiseOffset = 0.0;
        for (int round = 0; round < this.depth; round++) {
            double nx = latticeX * noiseScale + noiseOffset;
            double nz = latticeZ * noiseScale + noiseOffset;
            double xPeriod = this.x.period() * noiseScale;
            double zPeriod = this.z.period() * noiseScale;
            px = (px + this.noiseX.eval(nx, nz, xPeriod, zPeriod)) / 2.0;
            pz = (pz + this.noiseZ.eval(nx, nz, xPeriod, zPeriod)) / 2.0;
            noiseScale /= 2.0;
            noiseOffset = noiseOffset / 2.0 + round;
        }

        int cellX = this.x.wrapCell(Mth.floor(px));
        int cellZ = this.z.wrapCell(Mth.floor(pz));
        int kx = this.x.chunkOf(cellX);
        int kz = this.z.chunkOf(cellZ);
        boolean shiftX = this.x.isBorder(kx, cellX - this.x.startOf(kx)) && ((cellZ >> CRENEL_SHIFT) & 1) == 1;
        boolean shiftZ = this.z.isBorder(kz, cellZ - this.z.startOf(kz)) && ((cellX >> CRENEL_SHIFT) & 1) == 1;
        if (shiftX) {
            cellX = this.x.wrapCell(cellX + 1);
            kx = this.x.chunkOf(cellX);
        }

        if (shiftZ) {
            cellZ = this.z.wrapCell(cellZ + 1);
            kz = this.z.chunkOf(cellZ);
        }

        return chunk(kx, kz).get(cellX - this.x.startOf(kx), cellZ - this.z.startOf(kz));
    }

    @Override
    LapAxis axis(Direction.Axis axis) {
        return axis == Direction.Axis.X ? this.x : this.z;
    }

    @Override
    LapChunk<T> buildChunk(int kx, int kz) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(0L, kx, kz, 0);
        return new SquareLapChunk<>(this.x.sideOf(kx), this.z.sideOf(kz), random, this.picker);
    }
}
