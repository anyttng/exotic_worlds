package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.BaseRiverGenerator;

@Mixin(value = BaseRiverGenerator.class, remap = false)
public interface BaseRiverGeneratorAccessor {
    @Accessor("continentScale")
    int exotic_worlds$continentScale();
}
