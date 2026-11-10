package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.SpawnPoint;
import com.exoticworlds.engine.gen.SpawnSearch;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorOriginMixin {
    @ModifyReturnValue(method = "getOrigin", at = @At("RETURN"))
    private ChunkPos exotic_worlds$originAtTheSpawnPoint(ChunkPos origin) {
        SpawnPoint point = SpawnSearch.bound();
        return point.equals(SpawnPoint.ZERO) ? origin
                : new ChunkPos(origin.x() + point.xChunk(), origin.z() + point.zChunk());
    }
}
