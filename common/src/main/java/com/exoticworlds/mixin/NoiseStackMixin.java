package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ContextOctavesHolder;
import com.exoticworlds.engine.noise.ContextOctaves;

import net.minecraft.world.level.levelgen.synth.NoiseStack;

@Mixin(NoiseStack.class)
public class NoiseStackMixin implements ContextOctavesHolder {
    @Unique
    private volatile @Nullable ContextOctaves exotic_worlds$contextOctaves;

    @Override
    public @Nullable ContextOctaves exotic_worlds$contextOctaves() {
        return this.exotic_worlds$contextOctaves;
    }

    @Override
    public void exotic_worlds$contextOctaves(ContextOctaves octaves) {
        this.exotic_worlds$contextOctaves = octaves;
    }
}
