package com.exoticworlds.compat.wover.ambertation.mixin;

import de.ambertation.wover.generator.impl.biomesource.end.WoverEndBiomeSource;
import de.ambertation.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

@Mixin({WoverNetherBiomeSource.class, WoverEndBiomeSource.class})
public class WoverBiomeSourceMixin {
    @WrapMethod(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;")
    private Holder<Biome> exotic_worlds$foldedQuart(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
            Operation<Holder<Biome>> original) {
        WorldFold fold = GenerationFoldContext.getContext().getWrappedFold();
        if (fold == null) {
            return original.call(quartX, quartY, quartZ, sampler);
        }

        long folded = FoldedQuart.fold(fold, quartX, quartY, quartZ);
        return original.call(FoldedQuart.x(folded), quartY, FoldedQuart.z(folded), sampler);
    }
}
