package com.exoticworlds.engine.fold;

import com.exoticworlds.accessors.RelocatableBlockEntity;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockEntitySeat {
    public static void intoBounds(WorldFold fold, BlockEntity blockEntity) {
        if (!fold.isWrapped()) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        if (fold.isOver(pos)) {
            ((RelocatableBlockEntity) blockEntity).toroidal$relocate(fold.fold(pos));
        }
    }

    private BlockEntitySeat() {
    }
}
