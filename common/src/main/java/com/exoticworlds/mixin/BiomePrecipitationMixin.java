package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
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

        if (GenerationFoldContext.context().wrappedFold() != null) {
            return original.call(pos, seaLevel);
        }

        WorldFold bounds = WorldLoopAttachments.wrappedClientBoundsFoldOf(minecraft.level);
        if (bounds == null) {
            return original.call(pos, seaLevel);
        }

        return GenerationFoldContext.withFold(bounds, () -> original.call(pos, seaLevel));
    }
}
