package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;

@Mixin(TheEndBiomeSource.class)
public class TheEndBiomeSourceMixin {
    @WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private Holder<Biome> toroidal$loopedNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
            Operation<Holder<Biome>> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(quartX, quartY, quartZ, sampler);
        }

        long folded = FoldedQuart.fold(transformer, quartX, quartY, quartZ);
        return original.call(FoldedQuart.x(folded), quartY, FoldedQuart.z(folded), sampler);
    }
}
