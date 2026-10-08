package com.exoticworlds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.gen.AddedStructureStarts;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorAddedStartsMixin {
    @Inject(method = "createStructures", at = @At("RETURN"))
    private void toroidal$generateAddedStarts(RegistryAccess registryAccess, ChunkGeneratorStructureState state,
            StructureManager structureManager, ChunkAccess centerChunk,
            StructureTemplateManager structureTemplateManager, ResourceKey<Level> level, CallbackInfo callback) {
        if (SharedConstants.DEBUG_DISABLE_STRUCTURES) {
            return;
        }

        ChunkGenerator generator = (ChunkGenerator) (Object) this;
        CarriedShape carried = ShapedChunkGenerator.carriedShapeOf(generator);
        if (carried == null) {
            return;
        }

        ChunkPos chunk = centerChunk.getPos();
        List<StructureSet.StructureSelectionEntry> added = AddedStructureStarts.of(state,
                ((StructureManagerAccessor) structureManager).toroidal$structureCheck(), carried).at(chunk);
        if (added.isEmpty()) {
            return;
        }

        Climate.Sampler climateSampler =
                state.randomState().createClimateSampler(SamplerContext.builder().enableCaches().build());
        for (StructureSet.StructureSelectionEntry entry : added) {
            StructureStart existing = structureManager.getStartForStructure(entry.structure().value(), centerChunk);
            if (existing != null && existing.isValid()) {
                continue;
            }

            ((ChunkGeneratorAccessor) generator).toroidal$tryGenerateStructure(entry, structureManager, registryAccess,
                    state.randomState(), structureTemplateManager, state.getLevelSeed(), centerChunk, chunk, level,
                    climateSampler);
        }
    }
}
