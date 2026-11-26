package com.exoticworlds.core.feature;

import java.util.function.DoubleSupplier;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record ClimateFieldMoment(WorldFold fold, ResourceKey<LevelStem> dimension, double verticalShare,
        DoubleSupplier fitted) {
}
