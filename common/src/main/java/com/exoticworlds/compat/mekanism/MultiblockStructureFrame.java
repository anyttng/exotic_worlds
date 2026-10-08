package com.exoticworlds.compat.mekanism;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface MultiblockStructureFrame {
    @Nullable
    BlockPos toroidal$anchor();

    @Nullable
    Level toroidal$level();
}
