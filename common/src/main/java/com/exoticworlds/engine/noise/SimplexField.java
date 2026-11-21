package com.exoticworlds.engine.noise;

import com.exoticworlds.core.WorldFold;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public interface SimplexField {
    WorldFold fold();

    double scale();

    boolean carries();

    double sample(SimplexNoise noise, double x, double z);
}
