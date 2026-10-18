package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.accessors.TransformerCache;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.level.BoundLevelFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkTracker.class)
public class ChunkTrackerMixin implements LevelBindable, TransformerCache {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @WrapOperation(
            method = {"checkNeighborsAfterUpdate", "getComputedLevel"},
            at = @At(value = "INVOKE", target = InjectionTargets.CHUNK_POS_AS_LONG),
            expect = 2)
    private long exotic_worlds$physicalNeighborKey(int chunkX, int chunkZ, Operation<Long> original) {
        long folded = this.exotic_worlds$bound.fold().foldChunkKey(ChunkPos.asLong(chunkX, chunkZ));
        return original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }

    @Override
    public WorldFold exotic_worlds$transformer() {
        return this.exotic_worlds$bound.fold();
    }
}
