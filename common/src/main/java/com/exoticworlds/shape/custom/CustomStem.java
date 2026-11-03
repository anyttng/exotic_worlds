package com.exoticworlds.shape.custom;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public sealed interface CustomStem permits VanillaDimension, ModDimension {
    ResourceKey<LevelStem> stem();

    int defaultChunkWidth();
}
