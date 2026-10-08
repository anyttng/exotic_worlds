package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import raccoonman.reterraforged.world.worldgen.cell.continent.advanced.AbstractContinent;

@Mixin(value = AbstractContinent.class, remap = false)
public interface AbstractContinentAccessor {
    @Accessor("seed")
    int toroidal$seed();

    @Accessor("jitter")
    float toroidal$jitter();
}
