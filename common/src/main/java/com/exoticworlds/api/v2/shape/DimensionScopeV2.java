package com.exoticworlds.api.v2.shape;

import java.util.List;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * The dimensions a feature works in on one shape. A feature attached with {@link #ALL} works in every dimension that
 * takes the shape, a mod's own dimensions included; one attached with {@link #of} works in the named dimensions alone,
 * and its line is shown on those dimensions' pages only.
 */
public final class DimensionScopeV2 {
    /** Every dimension that takes the shape. */
    public static final DimensionScopeV2 ALL = new DimensionScopeV2(List.of());

    private final List<ResourceKey<LevelStem>> dimensions;

    private DimensionScopeV2(List<ResourceKey<LevelStem>> dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * The named dimensions alone.
     *
     * @throws IllegalArgumentException if no dimension is named: use {@link #ALL} for every dimension
     */
    public static DimensionScopeV2 of(List<ResourceKey<LevelStem>> dimensions) {
        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException(
                    "A dimension scope names at least one dimension: use DimensionScopeV2.ALL for every dimension.");
        }

        return new DimensionScopeV2(List.copyOf(dimensions));
    }

    /** Whether the scope is {@link #ALL}. */
    public boolean isAll() {
        return this.dimensions.isEmpty();
    }

    /** The named dimensions, empty for {@link #ALL}. */
    public List<ResourceKey<LevelStem>> getDimensions() {
        return this.dimensions;
    }

    /** Whether a feature attached with this scope works in {@code dimension}. */
    public boolean contains(ResourceKey<LevelStem> dimension) {
        return isAll() || this.dimensions.contains(dimension);
    }
}
