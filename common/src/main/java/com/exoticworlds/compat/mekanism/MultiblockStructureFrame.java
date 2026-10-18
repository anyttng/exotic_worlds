package com.exoticworlds.compat.mekanism;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface MultiblockStructureFrame {
    @Nullable
    BlockPos exotic_worlds$anchor();

    @Nullable
    Level exotic_worlds$level();
}
