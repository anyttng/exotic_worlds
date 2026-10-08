package com.exoticworlds.compat.mekanism.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.InjectionTargets;
import com.exoticworlds.compat.mekanism.MultiblockFrames;
import com.exoticworlds.compat.mekanism.MultiblockSyncedTag;
import com.exoticworlds.core.WorldLoopAttachments;

import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.prefab.TileEntityMultiblock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = TileEntityMultiblock.class, remap = false)
public class TileEntityMultiblockMixin {
    @WrapOperation(method = "onNeighborChange", at = @At(value = "INVOKE", target = InjectionTargets.SET_CONTAINS))
    private boolean toroidal$internalInFrame(Set<?> internalLocations, Object neighborPos, Operation<Boolean> original,
            @Local MultiblockData multiblock) {
        return original.call(internalLocations, MultiblockFrames.seatOnto((Object) multiblock, neighborPos));
    }

    @ModifyArg(method = "handleUpdateTag",
            at = @At(value = "INVOKE",
                    target = "Lmekanism/common/lib/multiblock/MultiblockData;readUpdateTag"
                            + "(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V"),
            index = 0)
    private CompoundTag toroidal$seatOntoTileCopy(CompoundTag tag) {
        BlockEntity tile = (BlockEntity) (Object) this;
        return MultiblockSyncedTag.seat(WorldLoopAttachments.transformerOfReader(tile.getLevel()),
                tile.getBlockPos(), tag);
    }
}
