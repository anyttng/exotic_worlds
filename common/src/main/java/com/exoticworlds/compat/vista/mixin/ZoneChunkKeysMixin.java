package com.exoticworlds.compat.vista.mixin;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.FoldHolder;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData;
import net.minecraft.world.level.ChunkPos;

@Mixin(targets = "net.mehvahdjukaar.vista.common.chunk_tracking.ExtraChunkViewData")
public class ZoneChunkKeysMixin implements FoldHolder {
    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(WorldFold fold) {
        this.exotic_worlds$fold = fold;
    }

    @WrapOperation(method = "rebuildChunkSets", at = @At(value = "INVOKE",
            target = "Lnet/mehvahdjukaar/vista/common/chunk_tracking/ExtraChunkViewData$Zone;chunks()Ljava/util/Set;"))
    private Set<ChunkPos> exotic_worlds$physicalZoneChunks(ExtraChunkViewData.Zone zone,
            Operation<Set<ChunkPos>> original) {
        Set<ChunkPos> physical = new HashSet<>();
        for (ChunkPos chunk : original.call(zone)) {
            physical.add(this.exotic_worlds$fold.fold(chunk));
        }

        return physical;
    }
}
