package com.exoticworlds.core.feature;

import net.minecraft.core.BlockPos;

public interface ChunkWriteListener {
    void beforeBlockWrite(BlockPos pos);
}
