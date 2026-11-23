package com.exoticworlds.compat.ultimatemapatlases;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.map.SeamLine;
import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class AtlasTileMemo {
    static final int MAX_TILES = 4096;

    private static final int COORD_BITS = 32;
    private static final long COORD_MASK = 0xFFFFFFFFL;

    private final Held<List<AtlasTileCopies.TileCenter>> copies = new Held<>(AtlasTileCopies::of);
    private final Held<List<SeamLine>> seamLines = new Held<>(AtlasSeamLines::of);

    private interface PerTile<T> {
        T of(@Nullable WorldFold fold, int centerX, int centerZ, int mapBlocks);
    }

    public List<AtlasTileCopies.TileCenter> copies(@Nullable WorldFold fold, int centerX, int centerZ,
            int mapBlocks) {
        return this.copies.at(fold, centerX, centerZ, mapBlocks);
    }

    public List<SeamLine> seamLines(@Nullable WorldFold fold, int centerX, int centerZ, int mapBlocks) {
        return this.seamLines.at(fold, centerX, centerZ, mapBlocks);
    }

    private static final class Held<T> {
        private final Long2ObjectOpenHashMap<T> byTile = new Long2ObjectOpenHashMap<>();
        private final PerTile<T> perTile;
        private @Nullable WorldFold fold;
        private int mapBlocks;

        private Held(PerTile<T> perTile) {
            this.perTile = perTile;
        }

        private T at(@Nullable WorldFold fold, int centerX, int centerZ, int mapBlocks) {
            if (fold != this.fold || mapBlocks != this.mapBlocks) {
                this.byTile.clear();
                this.fold = fold;
                this.mapBlocks = mapBlocks;
            }

            long tile = ((long) centerX << COORD_BITS) | (centerZ & COORD_MASK);
            T held = this.byTile.get(tile);
            if (held != null) {
                return held;
            }

            if (this.byTile.size() >= MAX_TILES) {
                this.byTile.clear();
            }

            T answered = this.perTile.of(fold, centerX, centerZ, mapBlocks);
            this.byTile.put(tile, answered);
            return answered;
        }
    }
}
