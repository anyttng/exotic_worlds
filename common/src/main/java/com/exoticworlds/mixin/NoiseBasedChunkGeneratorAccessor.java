package com.exoticworlds.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseBasedChunkGeneratorAccessor {
    @Accessor("globalFluidPicker")
    Supplier<Aquifer.FluidPicker> exotic_worlds$globalFluidPicker();

    @Invoker("doCreateBiomes")
    void exotic_worlds$doCreateBiomes(Blender blender, RandomState randomState, StructureManager structureManager,
            ChunkAccess protoChunk);

    @Invoker("doFill")
    ChunkAccess exotic_worlds$doFill(Blender blender, StructureManager structureManager, RandomState randomState,
            ChunkAccess centerChunk, int cellMinY, int cellCountY);
}
