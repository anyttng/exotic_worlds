package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
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

        long folded = transformer.foldBlockNode(
                BlockPos.asLong(QuartPos.toBlock(quartX), QuartPos.toBlock(quartY), QuartPos.toBlock(quartZ)));
        return original.call(QuartPos.fromBlock(BlockPos.getX(folded)), quartY,
                QuartPos.fromBlock(BlockPos.getZ(folded)), sampler);
    }
}
