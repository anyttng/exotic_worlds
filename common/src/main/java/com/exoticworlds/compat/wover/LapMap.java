package com.exoticworlds.compat.wover;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;

import net.minecraft.core.Direction;

public abstract class LapMap<T> {
    interface ChunkSource<T> {
        LapChunk<T> getChunk(int kx, int kz);
    }

    private final WorldFold fold;

    private final TranslationLattice lattice;

    private final double factor;

    private final LapChunkCache<LapChunk<T>> chunks = new LapChunkCache<>(this::isBounded, this::buildChunk);

    private volatile @Nullable ChunkSource<T> shared;

    LapMap(WorldFold fold, double factor) {
        this.fold = fold;
        this.lattice = fold.getBlockLattice();
        this.factor = factor;
    }

    WrapDomain getBlocks(Direction.Axis axis) {
        return this.lattice.getAxis(axis);
    }

    double frameX(double blockX, double blockZ) {
        return this.lattice.rectangleX(blockX, blockZ);
    }

    public boolean isCovering(WorldFold fold) {
        return this.fold == fold;
    }

    public double getFactor() {
        return this.factor;
    }

    public abstract T biomeAt(double blockX, double blockZ);

    abstract LapAxis getAxis(Direction.Axis axis);

    abstract LapChunk<T> buildChunk(int kx, int kz);

    void shareChunks(ChunkSource<T> source) {
        this.shared = source;
    }

    boolean isBounded() {
        return getAxis(Direction.Axis.X).isLooped() && getAxis(Direction.Axis.Z).isLooped();
    }

    LapChunk<T> getChunk(int kx, int kz) {
        ChunkSource<T> source = this.shared;
        if (source != null) {
            return source.getChunk(kx, kz);
        }

        return this.chunks.get(kx, kz);
    }
}
