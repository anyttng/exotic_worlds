package com.exoticworlds.compat.mekanism.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.exoticworlds.compat.mekanism.MultiblockFrames;

import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.multiblock.Structure;
import mekanism.common.lib.multiblock.StructureHelper;

@Mixin(value = StructureHelper.class, remap = false)
public class StructureHelperMixin {
    @ModifyReturnValue(
            method = {
                "fetchCuboid(Lmekanism/common/lib/multiblock/Structure;Lmekanism/common/lib/math/voxel/VoxelCuboid;"
                        + "Lmekanism/common/lib/math/voxel/VoxelCuboid;)Lmekanism/common/lib/math/voxel/VoxelCuboid;",
                "fetchCuboid(Lmekanism/common/lib/multiblock/Structure;Lmekanism/common/lib/math/voxel/VoxelCuboid;"
                        + "Lmekanism/common/lib/math/voxel/VoxelCuboid;Ljava/util/Set;I)"
                        + "Lmekanism/common/lib/math/voxel/VoxelCuboid;"
            },
            at = @At("RETURN"),
            require = 2)
    private static @Nullable VoxelCuboid toroidal$seatCanonicalMin(@Nullable VoxelCuboid cuboid,
            @Local(argsOnly = true) Structure structure) {
        return MultiblockFrames.seatCanonicalMin(structure, cuboid);
    }
}
