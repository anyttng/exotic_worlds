package com.exoticworlds.accessors;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public interface NoiseNode {
    Holder<NormalNoise> exotic_worlds$getNoise();
}
