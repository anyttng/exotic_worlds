package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

@Mixin(Biome.class)
public class BiomePrecipitationMixin {
    @WrapMethod(method = "getPrecipitationAt")
    private Biome.Precipitation exotic_worlds$foldForLevellessCaller(
            BlockPos pos, int seaLevel, Operation<Biome.Precipitation> original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            return original.call(pos, seaLevel);
        }

        if (GenerationFoldContext.getContext().getWrappedFold() != null) {
            return original.call(pos, seaLevel);
        }

        WorldFold bounds = WorldLoopAttachments.getWrappedClientBoundsFold(minecraft.level);
        if (bounds == null) {
            return original.call(pos, seaLevel);
        }

        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(bounds)) {
            return original.call(pos, seaLevel);
        }
    }
}
