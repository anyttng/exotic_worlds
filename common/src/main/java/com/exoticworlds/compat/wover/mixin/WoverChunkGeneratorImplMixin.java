package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.generator.impl.chunkgenerator.WoverChunkGeneratorImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.gen.ShapedDimensions;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;

@Mixin(WoverChunkGeneratorImpl.class)
public class WoverChunkGeneratorImplMixin {
    @ModifyVariable(method = "replaceGenerator", at = @At("HEAD"), argsOnly = true)
    private static ChunkGenerator toroidal$keepReplacedShape(ChunkGenerator generator,
            @Local(argsOnly = true, ordinal = 0) ResourceKey<LevelStem> dimensionKey,
            @Local(argsOnly = true) WoverChunkGeneratorImpl.StemGetter getter) {
        LevelStem replaced = getter.get(dimensionKey);
        CarriedShape carried = replaced == null ? null : ShapedChunkGenerator.carriedShapeOf(replaced.generator());
        if (carried == null || ShapedChunkGenerator.carriedShapeOf(generator) != null) {
            return generator;
        }

        ChunkGenerator shaped = ShapedDimensions.withStoredShape(generator, carried);
        return shaped == null ? generator : shaped;
    }
}
