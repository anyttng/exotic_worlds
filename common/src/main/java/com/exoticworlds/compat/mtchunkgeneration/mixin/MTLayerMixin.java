package com.exoticworlds.compat.mtchunkgeneration.mixin;

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.BinderOrder;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import dev.theagameplayer.mtchunkgeneration.world.level.levelgen.MTLayer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

@Mixin(MTLayer.class)
public class MTLayerMixin {
    @Shadow
    @Final
    private NoiseBasedChunkGenerator generator;

    @WrapMethod(method = "doCreateBiomes", order = BinderOrder.FOLD)
    private void toroidal$bindWhileCreatingBiomes(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk,
            Operation<Void> original) {
        GenerationTransformerContext.runWithTransformer(ShapedChunkGenerator.transformerOf(this.generator),
                () -> original.call(blender, randomState, structureManager, chunk));
    }

    @WrapOperation(
            method = {"doFill", "doCreateBiomes"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/IntStream;forEach(Ljava/util/function/IntConsumer;)V"),
            order = BinderOrder.FOLD)
    private void toroidal$bindEachSlice(IntStream slices, IntConsumer slice, Operation<Void> original) {
        WorldFold fold = ShapedChunkGenerator.transformerOf(this.generator);
        original.call(slices, (IntConsumer) index -> GenerationTransformerContext.runWithTransformer(fold,
                () -> slice.accept(index)));
    }
}
