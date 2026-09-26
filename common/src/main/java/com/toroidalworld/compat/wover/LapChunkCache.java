package com.toroidalworld.compat.wover;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import net.minecraft.world.level.ChunkPos;

final class LapChunkCache<V> {
    interface Builder<V> {
        V build(int kx, int kz);
    }

    static final int LIMIT = 127;

    private final Map<Long, V> chunks = new ConcurrentHashMap<>();

    private final BooleanSupplier bounded;

    private final Builder<V> builder;

    LapChunkCache(BooleanSupplier bounded, Builder<V> builder) {
        this.bounded = bounded;
        this.builder = builder;
    }

    V get(int kx, int kz) {
        if (!this.bounded.getAsBoolean() && this.chunks.size() > LIMIT) {
            this.chunks.clear();
        }

        return this.chunks.computeIfAbsent(ChunkPos.pack(kx, kz), key -> this.builder.build(kx, kz));
    }
}
