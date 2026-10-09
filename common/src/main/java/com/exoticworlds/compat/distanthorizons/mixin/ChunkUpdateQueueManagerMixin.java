package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhRowCopies;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.api.internal.chunkUpdating.ChunkUpdateQueueManager;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkUpdateQueueManager.class)
public class ChunkUpdateQueueManagerMixin {
    @WrapOperation(
            method = "processChunkUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/level/IDhLevel;"
                            + "updateChunkAsync(Lcom/seibel/distanthorizons/core/wrapperInterfaces/chunk/IChunkWrapper;I)V"))
    private void toroidal$updateEveryRowCopy(IDhLevel level, IChunkWrapper chunk, int chunkHash,
            Operation<Void> original) {
        original.call(level, chunk, chunkHash);
        DhLattice lattice = DhShapes.of(level);
        if (lattice == null || !(chunk instanceof ChunkWrapper wrapper)) {
            return;
        }

        DhChunkPos pos = wrapper.getChunkPos();
        for (ChunkPos copyPos : DhRowCopies.copiesOf(lattice, pos.getX(), pos.getZ())) {
            ChunkWrapper copy = DhRowCopies.buildAt(copyPos, wrapper::copy);
            ChunkWrapperAccessor light = (ChunkWrapperAccessor) wrapper;
            copy.setBlockLightStorage(light.toroidal$blockLightStorage());
            copy.setSkyLightStorage(light.toroidal$skyLightStorage());
            copy.setIsDhBlockLightCorrect(wrapper.isDhBlockLightingCorrect());
            copy.setIsDhSkyLightCorrect(wrapper.isDhSkyLightCorrect());
            original.call(level, copy, chunkHash);
        }
    }
}
