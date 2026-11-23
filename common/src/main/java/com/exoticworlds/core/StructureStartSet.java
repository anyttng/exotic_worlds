package com.exoticworlds.core;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public interface StructureStartSet {
    WorldFold fold();

    ShapeOptions options();

    Holder<StructureSet> structureSet();

    List<StructurePick> picks();

    boolean canStart(Holder<Structure> structure, ChunkPos chunk);
}
