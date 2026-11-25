package com.exoticworlds.core.feature;

import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.SpawnPoint;

public interface SpawnFeature extends GenerationFeature {
    SpawnPoint spawnPointIn(ShapeOptions options);
}
