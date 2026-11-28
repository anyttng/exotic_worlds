package com.exoticworlds.compat.betterend.betterx.mixin;

import org.betterx.betterend.world.generator.TerrainGenerator;
import org.betterx.wover.generator.api.biomesource.end.WoverEndConfig;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TerrainGenerator.class)
public interface TerrainGeneratorConfigAccessor {
    @Accessor("config")
    static @Nullable WoverEndConfig exotic_worlds$getConfig() {
        throw new AssertionError();
    }
}
