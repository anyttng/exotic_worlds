package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.accessors.RegionLevelSource;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(PathNavigationRegion.class)
public class PathNavigationRegionMixin implements RegionLevelSource {
    @Shadow
    @Final
    protected Level level;

    @Override
    public Level exotic_worlds$getRegionLevel() {
        return this.level;
    }

    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkSource;getChunkNow(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private @Nullable LevelChunk exotic_worlds$foldSnapshotChunk(ChunkSource chunkSource, int chunkX, int chunkZ,
            Operation<@Nullable LevelChunk> original, @Local(argsOnly = true, ordinal = 0) BlockPos start,
            @Local(argsOnly = true, ordinal = 1) BlockPos end) {
        long centerKey = ChunkPos.pack(SectionPos.blockToSectionCoord((start.getX() + end.getX()) >> 1),
                SectionPos.blockToSectionCoord((start.getZ() + end.getZ()) >> 1));
        long folded = WorldLoopAttachments.getFold(this.level).foldChunkKeyFrom(centerKey, ChunkPos.pack(chunkX, chunkZ));
        return original.call(chunkSource, ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }
}
