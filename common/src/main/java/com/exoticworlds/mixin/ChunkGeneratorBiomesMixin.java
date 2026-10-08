package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorBiomesMixin {
    @WrapMethod(method = "doCreateBiomes", order = BinderOrder.FOLD)
    private void toroidal$bindWhileCreatingBiomes(Blender blender, RandomState randomState, ChunkAccess protoChunk,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(
                ShapedChunkGenerator.transformerOf((ChunkGenerator) (Object) this),
                () -> original.call(blender, randomState, protoChunk));
    }
}
