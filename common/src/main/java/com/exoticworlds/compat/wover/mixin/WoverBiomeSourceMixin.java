package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.generator.impl.biomesource.end.WoverEndBiomeSource;
import org.betterx.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.InjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

@Mixin({WoverNetherBiomeSource.class, WoverEndBiomeSource.class})
public class WoverBiomeSourceMixin {
    // The Fabric jar spells this override in intermediary, and the remapper cannot resolve an override in a foreign class.
    @WrapMethod(method = {InjectionTargets.BIOME_SOURCE_GET_NOISE_BIOME,
            "method_38109(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"})
    private Holder<Biome> toroidal$foldedNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
            Operation<Holder<Biome>> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(quartX, quartY, quartZ, sampler);
        }

        long folded = FoldedQuart.fold(transformer, quartX, quartY, quartZ);
        return original.call(FoldedQuart.x(folded), quartY, FoldedQuart.z(folded), sampler);
    }
}
