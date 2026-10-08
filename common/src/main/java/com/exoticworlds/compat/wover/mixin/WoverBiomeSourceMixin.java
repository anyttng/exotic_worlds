package com.exoticworlds.compat.wover.mixin;

import org.betterx.wover.generator.impl.biomesource.end.WoverEndBiomeSource;
import org.betterx.wover.generator.impl.biomesource.nether.WoverNetherBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.biome.BiomeResolver;

@Mixin({WoverNetherBiomeSource.class, WoverEndBiomeSource.class})
public class WoverBiomeSourceMixin {
    @ModifyReturnValue(method = "createResolver", at = @At("RETURN"))
    private BiomeResolver toroidal$foldedResolver(BiomeResolver resolver) {
        return (quartX, quartY, quartZ) -> {
            WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
            if (transformer == null) {
                return resolver.getNoiseBiome(quartX, quartY, quartZ);
            }

            long folded = FoldedQuart.fold(transformer, quartX, quartY, quartZ);
            return resolver.getNoiseBiome(FoldedQuart.x(folded), quartY, FoldedQuart.z(folded));
        };
    }
}
