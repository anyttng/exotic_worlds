package com.exoticworlds.compat.wover;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.exoticworlds.core.fold.WorldFold;

public final class StackLayers<T> {
    private final Consumer<Class<?>> unclosed;

    private boolean isReported;

    public StackLayers(Consumer<Class<?>> unclosed) {
        this.unclosed = unclosed;
    }

    @SuppressWarnings("unchecked")
    public <M> List<LapMap<T>> of(M[] maps, WorldFold fold) {
        List<LapMap<T>> layers = new ArrayList<>(maps.length);
        for (M map : maps) {
            if (!(map instanceof LapMapHolder<?> holder)) {
                report(map);
                return List.of();
            }

            layers.add(((LapMapHolder<T>) holder).exotic_worlds$lapMap(fold));
        }

        return layers;
    }

    private void report(Object map) {
        if (!this.isReported) {
            this.isReported = true;
            this.unclosed.accept(map.getClass());
        }
    }
}
