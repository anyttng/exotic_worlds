package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.pandaismyname1.ultraterraforged.world.worldgen.continent.advanced.AbstractContinent;

@Mixin(value = AbstractContinent.class, remap = false)
public interface AbstractContinentAccessor {
    @Accessor("seed")
    int exotic_worlds$seed();

    @Accessor("jitter")
    float exotic_worlds$jitter();
}
