package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

@Mixin(TheEndBiomeSource.class)
public class TheEndBiomeSourceMixin {
    @WrapMethod(method = InjectionTargets.BIOME_SOURCE_GET_NOISE_BIOME)
    private Holder<Biome> exotic_worlds$loopedNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
            Operation<Holder<Biome>> original) {
        long folded = FoldedQuart.fold(GenerationFoldContext.context().fold(), quartX, quartY, quartZ);
        return original.call(FoldedQuart.x(folded), quartY, FoldedQuart.z(folded), sampler);
    }
}
