package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.biome.BiomeSource;

@Mixin(BiomeSource.class)
public interface BiomeSourceAccessor {
    @Invoker("codec")
    MapCodec<? extends BiomeSource> exotic_worlds$codec();
}
