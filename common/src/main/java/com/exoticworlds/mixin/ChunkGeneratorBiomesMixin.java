package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.gen.ShapedChunkGenerator;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorBiomesMixin {
    @WrapMethod(method = "doCreateBiomes", order = BinderOrder.FOLD)
    private void exotic_worlds$bindWhileCreatingBiomes(Blender blender, RandomState randomState, ChunkAccess protoChunk,
            Operation<Void> original) {
        WorldFold fold = ShapedChunkGenerator.getFold((ChunkGenerator) (Object) this);
        try (GenerationFoldContext.Context.FoldScope _ = GenerationFoldContext.getContext().withFold(fold)) {
            original.call(blender, randomState, protoChunk);
        }
    }
}
