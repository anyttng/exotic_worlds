package com.exoticworlds.compat.wover;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.betterx.wover.generator.api.map.BiomeMap;

import com.exoticworlds.core.WorldFold;

public final class StackLayers<T> {
    private final Consumer<Class<?>> unclosed;

    private boolean reported;

    public StackLayers(Consumer<Class<?>> unclosed) {
        this.unclosed = unclosed;
    }

    @SuppressWarnings("unchecked")
    public List<LapMap<T>> of(BiomeMap[] maps, WorldFold fold) {
        List<LapMap<T>> layers = new ArrayList<>(maps.length);
        for (BiomeMap map : maps) {
            if (!(map instanceof LapMapHolder<?> holder)) {
                report(map);
                return List.of();
            }

            layers.add(((LapMapHolder<T>) holder).exotic_worlds$lapMap(fold));
        }

        return layers;
    }

    private void report(BiomeMap map) {
        if (!this.reported) {
            this.reported = true;
            this.unclosed.accept(map.getClass());
        }
    }
}
