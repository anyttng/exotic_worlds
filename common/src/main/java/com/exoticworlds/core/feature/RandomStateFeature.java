package com.exoticworlds.core.feature;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.levelgen.RandomState;

public interface RandomStateFeature extends GenerationFeature {
    void atRandomState(RandomState randomState, WorldFold fold, int seaLevel);
}
