package com.exoticworlds.core.feature;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public record NoiseMoment(WorldFold fold, ResourceKey<LevelStem> dimension, Holder<NormalNoise> noise, NoiseStack stack,
        double xzScale, double verticalShare) {
}
