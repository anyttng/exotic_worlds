package com.exoticworlds.api.v2.feature;

import com.exoticworlds.api.v2.ExperimentalV2;

import net.minecraft.server.level.ServerLevel;

/**
 * What the moments that run on a loaded level share: a check, made once per level, of whether the feature works there
 * at all.
 */
@ExperimentalV2
public interface LevelMomentV2 {
    /**
     * Whether the feature works on {@code level}. A level it does not work on calls none of its level moments. Every
     * level is accepted unless this is overridden.
     */
    default boolean isInstalledOn(ServerLevel level) {
        return true;
    }
}
