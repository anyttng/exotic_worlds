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
    private volatile @Nullable ContextOctaves toroidal$contextOctaves;

    @Override
    public @Nullable ContextOctaves toroidal$contextOctaves() {
        return this.toroidal$contextOctaves;
    }

    @Override
    public void toroidal$contextOctaves(ContextOctaves octaves) {
        this.toroidal$contextOctaves = octaves;
    }
}
