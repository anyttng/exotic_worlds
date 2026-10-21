package com.exoticworlds.compat.immersiveoverlays;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;

public final class CompassOverlayFold {
    public static BlockPos shown(WorldFold fold, BlockPos player) {
        return fold.fold(player);
    }

    public static BlockPos anchor(WorldFold fold, BlockPos player, BlockPos anchor) {
        return fold.nearestCopy(shown(fold, player), anchor);
    }

    private CompassOverlayFold() {
    }
}
