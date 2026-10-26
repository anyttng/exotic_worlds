package com.exoticworlds.compat.ultraterraforged.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.ultraterraforged.LapCarrier;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.ClimateScaleCompression;

import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;

@Mixin(value = Levels.class, remap = false)
public abstract class LevelsMixin implements LapCarrier {
    @Unique
    private volatile @Nullable WorldFold exotic_worlds$fold;

    @Unique
    private volatile double exotic_worlds$climateCompression = ClimateScaleCompression.NO_COMPRESSION;

    @Override
    public @Nullable WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public double exotic_worlds$climateCompression() {
        return this.exotic_worlds$climateCompression;
    }

    @Override
    public void exotic_worlds$carryFold(WorldFold fold, double climateCompression) {
        this.exotic_worlds$climateCompression = climateCompression;
        this.exotic_worlds$fold = fold;
    }
}
