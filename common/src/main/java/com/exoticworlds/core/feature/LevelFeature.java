package com.exoticworlds.core.feature;

import net.minecraft.server.level.ServerLevel;

public interface LevelFeature extends GenerationFeature {
    default boolean isInstalledOn(ServerLevel level) {
        return true;
    }
}
