package com.exoticworlds.compat.eminus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

@Pseudo
@Mixin(targets = "com.eminus.beam.BeamSet", remap = false)
public class BeamSetMixin implements CellFoldHolder {
    @Unique
    private volatile CellFold exotic_worlds$fold = CellFold.NONE;

    @Override
    public CellFold exotic_worlds$fold() {
        return exotic_worlds$fold;
    }

    @Override
    public void exotic_worlds$setFold(CellFold fold) {
        exotic_worlds$fold = fold;
    }

    @ModifyVariable(method = {"load", "put", "remove", "holds"}, at = @At("HEAD"), argsOnly = true)
    private long exotic_worlds$storeTheFoldedBlock(long pos) {
        return exotic_worlds$fold.foldBlock(pos);
    }

    @WrapMethod(method = "inChunk")
    private LongList exotic_worlds$inTheLapOfTheChunk(int chunkX, int chunkZ, Operation<LongList> original) {
        CellFold fold = exotic_worlds$fold;
        double centerX = SectionPos.sectionToBlockCoord(chunkX, SectionPos.SECTION_HALF_SIZE);
        double centerZ = SectionPos.sectionToBlockCoord(chunkZ, SectionPos.SECTION_HALF_SIZE);
        LongList found = new LongArrayList();
        for (long folded : fold.foldedChunks(chunkX, chunkZ)) {
            LongList stored = original.call(ChunkPos.getX(folded), ChunkPos.getZ(folded));
            for (int index = 0; index < stored.size(); index++) {
                long copy = fold.copyNear(stored.getLong(index), centerX, centerZ);
                if (SectionPos.blockToSectionCoord(BlockPos.getX(copy)) == chunkX
                        && SectionPos.blockToSectionCoord(BlockPos.getZ(copy)) == chunkZ) {
                    found.add(copy);
                }
            }
        }

        return found;
    }
}
