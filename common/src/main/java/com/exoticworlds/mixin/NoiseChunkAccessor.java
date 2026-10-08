package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;

@Mixin(NoiseChunk.class)
public interface NoiseChunkAccessor {
    @Accessor("randomState")
    RandomState toroidal$randomState();
}
