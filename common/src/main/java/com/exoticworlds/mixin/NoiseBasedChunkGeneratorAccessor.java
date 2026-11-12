package com.exoticworlds.mixin;

import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

@Mixin(NoiseBasedChunkGenerator.class)
public interface NoiseBasedChunkGeneratorAccessor {
    @Accessor("globalFluidPicker")
    Supplier<Aquifer.FluidPicker> exotic_worlds$globalFluidPicker();

    @Invoker("chunkVolume")
    static DensityVolume exotic_worlds$chunkVolume(ChunkAccess chunk, NoiseSettings noiseSettings) {
        throw new AssertionError();
    }

    @Invoker("doFill")
    void exotic_worlds$doFill(NoiseChunk noiseChunk, ChunkAccess chunk);

    @Invoker("buildSurface")
    void exotic_worlds$buildSurface(ChunkAccess protoChunk, NoiseChunk noiseChunk, RandomState randomState,
            BiomeManager biomeManager, @Nullable Set<Holder<Biome>> possibleBiomes, MaterialRule materialRule);

    @Invoker("generateCarvers")
    void exotic_worlds$generateCarvers(ChunkAccess chunk, Blender blender, NoiseChunk noiseChunk,
            RandomState randomState, BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion,
            MaterialRule materialRule);
}
