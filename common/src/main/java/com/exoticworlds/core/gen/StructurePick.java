package com.exoticworlds.core.gen;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

public record StructurePick(ChunkPos chunk, Optional<Holder<Structure>> structure) {
}
