package com.exoticworlds.compat.wover;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;

public abstract class LapMap<T> {
    interface ChunkSource<T> {
        LapChunk<T> chunk(int kx, int kz);
    }

    private final WorldFold fold;

    private final double factor;

    private final LapChunkCache<LapChunk<T>> chunks = new LapChunkCache<>(this::bounded, this::buildChunk);

    private volatile @Nullable ChunkSource<T> shared;

    LapMap(WorldFold fold, double factor) {
        this.fold = fold;
        this.factor = factor;
    }

    public boolean covers(WorldFold fold) {
        return this.fold == fold;
    }

    public double factor() {
        return this.factor;
    }

    public abstract T biomeAt(double blockX, double blockZ);

    abstract LapAxis axis(Direction.Axis axis);

    abstract LapChunk<T> buildChunk(int kx, int kz);

    void shareChunks(ChunkSource<T> source) {
        this.shared = source;
    }

    boolean bounded() {
        return axis(Direction.Axis.X).loops() && axis(Direction.Axis.Z).loops();
    }

    LapChunk<T> chunk(int kx, int kz) {
        ChunkSource<T> source = this.shared;
        if (source != null) {
            return source.chunk(kx, kz);
        }

        return this.chunks.get(kx, kz);
    }
}
