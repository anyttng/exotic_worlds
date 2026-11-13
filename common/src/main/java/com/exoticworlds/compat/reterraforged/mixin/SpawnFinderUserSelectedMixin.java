package com.exoticworlds.compat.reterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.bawnorton.mixinsquared.TargetHandler;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.reterraforged.ReTerraForgedInjectionTargets;
import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.gen.SpawnSearch;

import net.minecraft.world.level.biome.Climate;

@Mixin(value = Climate.class, priority = 1200)
public class SpawnFinderUserSelectedMixin {
    private static final int USER_SELECTED_BRANCH = 0;

    @ModifyArg(method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_INIT, ordinal = USER_SELECTED_BRANCH),
            index = 0)
    @TargetHandler(mixin = ReTerraForgedInjectionTargets.SPAWN_FINDER_MIXIN,
            name = ReTerraForgedInjectionTargets.FIND_SPAWN_POSITION)
    private static int exotic_worlds$customPointX(int spawnX) {
        SpawnPoint point = SpawnSearch.bound();
        return point.isCustom() ? point.blockX() : spawnX;
    }

    @ModifyArg(method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_INIT, ordinal = USER_SELECTED_BRANCH),
            index = 2)
    @TargetHandler(mixin = ReTerraForgedInjectionTargets.SPAWN_FINDER_MIXIN,
            name = ReTerraForgedInjectionTargets.FIND_SPAWN_POSITION)
    private static int exotic_worlds$customPointZ(int spawnZ) {
        SpawnPoint point = SpawnSearch.bound();
        return point.isCustom() ? point.blockZ() : spawnZ;
    }
}
