package com.exoticworlds.core.feature;

import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public final class DimensionScope {
    public static final DimensionScope ALL = new DimensionScope(Set.of(), true);

    private final Set<ResourceKey<LevelStem>> dimensions;
    private final boolean isAll;

    private DimensionScope(Set<ResourceKey<LevelStem>> dimensions, boolean isAll) {
        this.dimensions = dimensions;
        this.isAll = isAll;
    }

    public static DimensionScope of(List<ResourceKey<LevelStem>> dimensions) {
        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException("A dimension scope names at least one dimension; use ALL for every one");
        }

        return new DimensionScope(Set.copyOf(dimensions), false);
    }

    public boolean contains(ResourceKey<LevelStem> dimension) {
        return this.isAll || this.dimensions.contains(dimension);
    }
}
