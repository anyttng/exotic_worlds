package com.exoticworlds.mixin;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

@Mixin(StructureBlockEntity.class)
public class StructureBlockEntityMixin {
    @ModifyReturnValue(method = "getRelatedCorners", at = @At("RETURN"))
    private Stream<BlockPos> exotic_worlds$cornersInOwnFrame(Stream<BlockPos> corners) {
        BlockEntity self = (BlockEntity) (Object) this;
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(self.getLevel());
        if (fold == null) {
            return corners;
        }

        BlockPos anchor = self.getBlockPos();
        return corners.map(corner -> fold.nearestCopy(anchor, corner));
    }
}
