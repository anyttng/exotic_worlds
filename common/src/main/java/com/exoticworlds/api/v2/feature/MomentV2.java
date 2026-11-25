package com.exoticworlds.api.v2.feature;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * What every moment tells a feature: the looped world it is called in, the dimension, and the value of the feature's
 * own option. Each moment interface takes one object that extends this one, so a later release can tell a feature
 * more without changing a single signature.
 */
public interface MomentV2 {
    /** The geometry of the dimension the moment is called in. */
    WorldGeometryV2 getGeometry();

    /** The dimension the moment is called in. */
    ResourceKey<LevelStem> getDimension();

    /**
     * The value {@code feature}'s option holds in this dimension: what the world stored, or the option's stored
     * default where the world never stated it.
     *
     * @throws IllegalStateException if {@code feature} was never registered
     */
    <T> T getValue(OptionFeatureV2<T> feature);
}
