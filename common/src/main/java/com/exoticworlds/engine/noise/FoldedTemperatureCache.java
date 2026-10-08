package com.exoticworlds.engine.noise;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;

public final class FoldedTemperatureCache {
    private static final int CAPACITY = 1024;

    private static final float LOAD_FACTOR = 0.25F;

    private final Long2FloatLinkedOpenHashMap temperatures = new Long2FloatLinkedOpenHashMap(CAPACITY, LOAD_FACTOR) {
        @Override
        protected void rehash(int newN) {
        }
    };

    private @Nullable WorldFold filledUnder;

    public FoldedTemperatureCache() {
        this.temperatures.defaultReturnValue(Float.NaN);
    }

    public Long2FloatLinkedOpenHashMap temperaturesUnder(WorldFold fold) {
        if (this.filledUnder != fold) {
            this.temperatures.clear();
            this.filledUnder = fold;
        }

        return this.temperatures;
    }
}
