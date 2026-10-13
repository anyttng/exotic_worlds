package com.exoticworlds.compat.seedviewer;

import java.util.function.Supplier;

import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;

public final class FoldedSampleCache<T> {
    private static final int CAPACITY = 1 << 20;

    private static final int STRIPES = 16;

    private static final int STRIPE_CAPACITY = CAPACITY / STRIPES;

    private static final int STRIPE_SHIFT = Long.SIZE - Integer.numberOfTrailingZeros(STRIPES);

    private final WorldFold fold;
    private final boolean perQuart;
    private final Long2ObjectOpenHashMap<T>[] stripes;

    @SuppressWarnings("unchecked")
    private FoldedSampleCache(WorldFold fold, boolean perQuart) {
        this.fold = fold;
        this.perQuart = perQuart;
        this.stripes = (Long2ObjectOpenHashMap<T>[]) new Long2ObjectOpenHashMap<?>[STRIPES];
        for (int stripe = 0; stripe < STRIPES; stripe++) {
            this.stripes[stripe] = new Long2ObjectOpenHashMap<>();
        }
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
        Long2ObjectOpenHashMap<T> stripe = this.stripes[(int) (HashCommon.mix(key) >>> STRIPE_SHIFT)];
        T cached;
        synchronized (stripe) {
            cached = stripe.get(key);
        }
        if (cached != null) {
            return cached;
        }

        T value = sample.get();
        synchronized (stripe) {
            if (stripe.size() >= STRIPE_CAPACITY) {
                stripe.clear();
            }
            stripe.put(key, value);
        }
        return value;
    }

    private static int quartCorner(int block) {
        return QuartPos.toBlock(QuartPos.fromBlock(block));
    }
}
