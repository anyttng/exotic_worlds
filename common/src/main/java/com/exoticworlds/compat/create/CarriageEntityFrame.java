package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface CarriageEntityFrame {
    @Nullable
    Level exotic_worlds$getCarriageLevel();

    @Nullable
    ResourceKey<Level> exotic_worlds$getCarriageDimension();

    void exotic_worlds$bindCarriageDimension(ResourceKey<Level> dimension);
}
