package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.ServerLevel;

@Mixin(ChunkTracker.class)
public class ChunkTrackerMixin implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @ModifyExpressionValue(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_PACK),
            expect = 2)
    private long exotic_worlds$physicalNeighborKey(long key) {
        return this.exotic_worlds$bound.fold().foldChunkKey(key);
    }
}
