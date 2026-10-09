package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhKeys;
import com.exoticworlds.compat.distanthorizons.DhRowCopies;
import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(ChunkWrapper.class)
public class ChunkWrapperMixin {
    @Shadow
    @Final
    private ILevelWrapper wrappedLevel;

    @Unique
    private ChunkPos toroidal$foldedPos;

    @WrapOperation(
            method = "<init>(Lnet/minecraft/world/level/chunk/ChunkAccess;"
                    + "Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos toroidal$foldedChunkPos(ChunkAccess chunk, Operation<ChunkPos> original) {
        ChunkPos folded = this.toroidal$foldedPos;
        if (folded == null) {
            ChunkPos copy = DhRowCopies.pendingCopy();
            folded = copy != null
                    ? copy
                    : DhKeys.foldChunk(DhShapes.clientFrame(this.wrappedLevel), original.call(chunk));
            this.toroidal$foldedPos = folded;
        }

        return folded;
    }
}
