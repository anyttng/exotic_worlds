package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.eminus.ingest.BiomeWindow;

import net.minecraft.world.level.biome.BiomeManager;

@Mixin(BiomeWindow.class)
public interface BiomeWindowAccessor {
    @Accessor("manager")
    BiomeManager exotic_worlds$manager();
}
