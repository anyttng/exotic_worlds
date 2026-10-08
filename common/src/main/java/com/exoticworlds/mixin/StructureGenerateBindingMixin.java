package com.exoticworlds.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@Mixin(Structure.class)
public class StructureGenerateBindingMixin {
    @WrapMethod(method = "generate", order = BinderOrder.FOLD)
    private StructureStart toroidal$generateOnThisWorldsNoise(
            RegistryAccess registryAccess,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkPos sourceChunkPos,
            int references,
            LevelHeightAccessor heightAccessor,
            Predicate<Holder<Biome>> validBiome,
            Operation<StructureStart> original) {
        return GenerationTransformerContext.withTransformer(
                ShapedChunkGenerator.transformerOf(chunkGenerator),
                () -> original.call(registryAccess, chunkGenerator, biomeSource, randomState,
                        structureTemplateManager, seed, sourceChunkPos, references, heightAccessor, validBiome));
    }
}
