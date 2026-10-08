package com.exoticworlds.compat.seedviewer;

import java.util.function.Supplier;

import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;

public final class FoldedSampleCache<T> {
    private static final int CAPACITY = 1 << 20;

    private final WorldFold fold;
    private final boolean perQuart;
    private final Long2ObjectOpenHashMap<T> values = new Long2ObjectOpenHashMap<>();

    private FoldedSampleCache(WorldFold fold, boolean perQuart) {
        this.fold = fold;
        this.perQuart = perQuart;
    }

    public static <T> FoldedSampleCache<T> perQuart(WorldFold fold) {
        return new FoldedSampleCache<>(fold, true);
    }

    public static <T> FoldedSampleCache<T> perBlock(WorldFold fold) {
        return new FoldedSampleCache<>(fold, false);
    }

    public T get(int blockX, int blockY, int blockZ, Supplier<T> sample) {
        long key = this.fold.foldBlockNode(this.perQuart
                ? BlockPos.asLong(quartCorner(blockX), quartCorner(blockY), quartCorner(blockZ))
                : BlockPos.asLong(blockX, blockY, blockZ));
        T cached;
        synchronized (this.values) {
            cached = this.values.get(key);
        }
        if (cached != null) {
            return cached;
        }

        T value = sample.get();
        synchronized (this.values) {
            if (this.values.size() >= CAPACITY) {
                this.values.clear();
            }
            this.values.put(key, value);
        }
        return value;
    }

    private static int quartCorner(int block) {
        return QuartPos.toBlock(QuartPos.fromBlock(block));
    }
}
