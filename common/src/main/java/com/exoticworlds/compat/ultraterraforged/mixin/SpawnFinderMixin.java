package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.gen.SpawnSearch;

import com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn.SpawnFinder;

@Mixin(value = SpawnFinder.class, remap = false)
public abstract class SpawnFinderMixin {
    @ModifyArg(method = "getSpawnPositionAndFitness",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_SQUARE_LONG, ordinal = 0))
    private long exotic_worlds$distanceFromTheSpawnPointX(long blockX) {
        return blockX - SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "getSpawnPositionAndFitness",
            at = @At(value = "INVOKE", target = InjectionTargets.MTH_SQUARE_LONG, ordinal = 1))
    private long exotic_worlds$distanceFromTheSpawnPointZ(long blockZ) {
        return blockZ - SpawnSearch.bound().blockZ();
    }
}
