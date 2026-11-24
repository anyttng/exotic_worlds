package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

@Mixin(ServerLevel.class)
public class ServerLevelUncachedBiomeMixin {
    @WrapMethod(method = "getUncachedNoiseBiome")
    private Holder<Biome> exotic_worlds$sampleThisWorldsBiomeField(int quartX, int quartY, int quartZ,
            Operation<Holder<Biome>> original) {
        WorldFold fold = WorldLoopAttachments.getFold((ServerLevel) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold)) {
            return original.call(quartX, quartY, quartZ);
        }
    }
}
