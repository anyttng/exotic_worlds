package com.exoticworlds.mixin;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {
    @Invoker("getStructureGeneratingAt")
    static @Nullable Pair<BlockPos, Holder<Structure>> toroidal$structureGeneratingAt(
            Set<Holder<Structure>> structures,
            LevelReader level,
            StructureManager structureManager,
            boolean createReference,
            StructurePlacement placement,
            ChunkPos candidate) {
        throw new AssertionError();
    }

    @Invoker("tryGenerateStructure")
    boolean toroidal$tryGenerateStructure(
            StructureSet.StructureSelectionEntry selected,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess centerChunk,
            ChunkPos sourceChunkPos,
            SectionPos sectionPos);
}
