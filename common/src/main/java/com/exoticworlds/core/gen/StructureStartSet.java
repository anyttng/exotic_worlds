package com.exoticworlds.core.gen;

import java.util.List;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.option.ShapeOptions;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public interface StructureStartSet {
    WorldFold getFold();

    ShapeOptions getOptions();

    ResourceKey<LevelStem> getDimension();

    Holder<StructureSet> getStructureSet();

    List<StructurePick> getPicks();

    boolean canStart(Holder<Structure> structure, ChunkPos chunk);
}
