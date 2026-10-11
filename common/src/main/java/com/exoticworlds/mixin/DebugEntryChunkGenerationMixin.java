package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.client.gui.components.debug.DebugEntryChunkGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(DebugEntryChunkGeneration.class)
public class DebugEntryChunkGenerationMixin {
    @WrapMethod(method = "update")
    private void toroidal$sampleThisWorldsField(@Nullable LevelChunk serverChunk, BlockPos feetPos,
            ServerLevel serverLevel, Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(WorldLoopAttachments.transformerOf(serverLevel),
                () -> original.call(serverChunk, feetPos, serverLevel));
    }
}
