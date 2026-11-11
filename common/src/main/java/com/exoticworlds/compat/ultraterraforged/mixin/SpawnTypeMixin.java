package com.exoticworlds.compat.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.gen.SpawnSearch;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.BlockPos;

@Mixin(targets = {
        "com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn.SpawnType$1",
        "com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn.SpawnType$2",
        "com.pandaismyname1.ultraterraforged.world.worldgen.biome.spawn.SpawnType$3"
}, remap = false)
public abstract class SpawnTypeMixin {
    @ModifyReturnValue(method = "getSearchCenter", at = @At("RETURN"))
    private BlockPos exotic_worlds$centerAtTheSpawnPoint(BlockPos center) {
        SpawnPoint point = SpawnSearch.bound();
        return point.isCustom() ? new BlockPos(point.blockX(), center.getY(), point.blockZ()) : center;
    }
}
