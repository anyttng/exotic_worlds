package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import raccoonman.reterraforged.world.worldgen.cell.rivermap.river.BaseRiverGenerator;

@Mixin(value = BaseRiverGenerator.class, remap = false)
public interface BaseRiverGeneratorAccessor {
    @Accessor("continentScale")
    int exotic_worlds$continentScale();
}
