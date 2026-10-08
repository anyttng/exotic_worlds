package com.exoticworlds.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.exoticworlds.compat.distanthorizons.SeamTarget;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;

@Mixin(WorldGenerationQueue.class)
public class WorldGenerationQueueMixin {
    @Shadow
    @Final
    private IDhServerLevel level;

    @WrapMethod(method = "startAndSetTargetPos")
    private void toroidal$measureThroughTheSeam(DhBlockPos2D targetPos, Operation<Void> original) {
        original.call(SeamTarget.of(DhShapes.of(this.level), targetPos));
    }

    @WrapOperation(method = "lambda$tryStartNextWorldGenTask$0", at = @At(value = "INVOKE",
            target = "Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;chebyshevDist(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;)I"))
    private static int toroidal$seamChebyshevDist(DhBlockPos2D centre, DhBlockPos2D target,
            Operation<Integer> original) {
        return target instanceof SeamTarget seam ? seam.chebyshevDistFrom(centre) : original.call(centre, target);
    }
}
