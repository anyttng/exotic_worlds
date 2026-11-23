package com.exoticworlds.core;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

public record AddedStart(Holder<Structure> structure, ChunkPos chunk) {
}
