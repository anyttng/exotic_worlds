package com.exoticworlds.engine.noise;

import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public interface SimplexField {
    WorldFold fold();

    double scale();

    boolean canCarry();

    double sample(SimplexNoise noise, double x, double z);
}
