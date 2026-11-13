package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.engine.gen.SpawnSearch;

import net.minecraft.world.level.biome.Climate;

@Mixin(value = Climate.class, priority = 1200)
public class SpawnFinderOriginMixin {
    private static final int WORLD_ORIGIN_BRANCH = 1;

    @ModifyArg(method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_INIT, ordinal = WORLD_ORIGIN_BRANCH),
            index = 0)
    @TargetHandler(mixin = ReTerraForgedInjectionTargets.SPAWN_FINDER_MIXIN,
            name = ReTerraForgedInjectionTargets.FIND_SPAWN_POSITION)
    private static int exotic_worlds$originAtTheSpawnPointX(int originX) {
        return originX + SpawnSearch.bound().blockX();
    }

    @ModifyArg(method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_INIT, ordinal = WORLD_ORIGIN_BRANCH),
            index = 2)
    @TargetHandler(mixin = ReTerraForgedInjectionTargets.SPAWN_FINDER_MIXIN,
            name = ReTerraForgedInjectionTargets.FIND_SPAWN_POSITION)
    private static int exotic_worlds$originAtTheSpawnPointZ(int originZ) {
        return originZ + SpawnSearch.bound().blockZ();
    }
}
