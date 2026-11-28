package com.exoticworlds.compat.wover;

import org.jspecify.annotations.Nullable;

public interface PickedBiomeTraits<T> {
    @Nullable T getEdge(T biome);

    int getEdgeSize(T biome);

    @Nullable T getParent(T biome);

    boolean isSame(T biome, T other);

    boolean isVertical(T biome);
}
