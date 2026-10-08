package com.exoticworlds.compat.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

@Mixin(targets = "dev.nonamecrackers2.simpleclouds.common.event.TickChunks", remap = false)
public class TickChunksMixin {
    @WrapMethod(method = "rainAndSnowVanillaCompatibility")
    private static void toroidal$bindPrecipitationTransformer(ServerLevel level, ChunkAccess chunk,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                WorldLoopAttachments.noiseTransformerOf(level), () -> original.call(level, chunk));
    }
}
