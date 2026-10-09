package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhLattice;
import com.exoticworlds.compat.distanthorizons.DhRowCopies;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
        if (lattice == null || !(chunk instanceof ChunkWrapperAccessor light)) {
            return;
        }

        DhChunkPos pos = chunk.getChunkPos();
        for (ChunkPos copyPos : DhRowCopies.copiesOf(lattice, pos.getX(), pos.getZ())) {
            IChunkWrapper copy = DhRowCopies.buildAt(copyPos, chunk::copy);
            ChunkWrapperAccessor copyLight = (ChunkWrapperAccessor) copy;
            copyLight.toroidal$setBlockLightStorage(light.toroidal$blockLightStorage());
            copyLight.toroidal$setSkyLightStorage(light.toroidal$skyLightStorage());
            copy.setIsDhBlockLightCorrect(chunk.isDhBlockLightingCorrect());
            copy.setIsDhSkyLightCorrect(chunk.isDhSkyLightCorrect());
            original.call(level, copy, chunkHash);
        }
    }
}
