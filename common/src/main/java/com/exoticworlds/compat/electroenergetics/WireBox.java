package com.exoticworlds.compat.electroenergetics;

import net.minecraft.world.level.ChunkPos;

public interface WireBox {
    ChunkPos toroidal$centre();

    int toroidal$radius();
}
