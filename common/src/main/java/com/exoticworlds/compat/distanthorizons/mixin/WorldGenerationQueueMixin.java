package com.exoticworlds.compat.distanthorizons.mixin;

import java.util.concurrent.ExecutorService;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.exoticworlds.compat.distanthorizons.DhShapes;
import com.exoticworlds.compat.distanthorizons.FoldBindingExecutor;
import com.exoticworlds.compat.distanthorizons.SeamTarget;
import com.exoticworlds.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.generation.queues.WorldGenerationQueue;
import com.seibel.distanthorizons.core.level.IDhServerLevel;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;

@Mixin(WorldGenerationQueue.class)
public class WorldGenerationQueueMixin {
    @Unique
    private static final String GENERATOR =
            "Lcom/seibel/distanthorizons/api/interfaces/override/worldGenerator/IDhApiWorldGenerator;";

    @Unique
    private static final String MODE_POOL_CONSUMER =
            "Lcom/seibel/distanthorizons/api/enums/worldGeneration/EDhApiDistantGeneratorMode;"
                    + "Ljava/util/concurrent/ExecutorService;Ljava/util/function/Consumer;)"
                    + "Ljava/util/concurrent/CompletableFuture;";

    @Unique
    private static final int CHUNKS_POOL_INDEX = 5;

    @Unique
    private static final int LOD_POOL_INDEX = 7;

    @Shadow
    @Final
    private IDhServerLevel level;

    @ModifyArg(method = "startVanillaChunkGenerationEvent", index = CHUNKS_POOL_INDEX,
            at = @At(value = "INVOKE", target = GENERATOR + "generateChunks(IIIB" + MODE_POOL_CONSUMER))
    private ExecutorService exotic_worlds$foldVanillaChunkPool(ExecutorService pool) {
        return this.exotic_worlds$bindLevelFold(pool);
    }

    @ModifyArg(method = "startApiChunkGenerationEvent", index = CHUNKS_POOL_INDEX,
            at = @At(value = "INVOKE", target = GENERATOR + "generateApiChunks(IIIB" + MODE_POOL_CONSUMER))
    private ExecutorService exotic_worlds$foldApiChunkPool(ExecutorService pool) {
        return this.exotic_worlds$bindLevelFold(pool);
    }

    @ModifyArg(method = "startApiDataSourceGenerationEvent", index = LOD_POOL_INDEX,
            at = @At(value = "INVOKE", target = GENERATOR + "generateLod(IIIIB"
                    + "Lcom/seibel/distanthorizons/api/objects/data/IDhApiFullDataSource;" + MODE_POOL_CONSUMER))
    private ExecutorService exotic_worlds$foldLodPool(ExecutorService pool) {
        return this.exotic_worlds$bindLevelFold(pool);
    }

    @Unique
    private ExecutorService exotic_worlds$bindLevelFold(ExecutorService pool) {
        WorldFold fold = DhShapes.serverFoldOf(this.level);
        return fold == null ? pool : new FoldBindingExecutor(pool, fold);
    }

    @WrapMethod(method = "startAndSetTargetPos")
    private void exotic_worlds$measureThroughTheSeam(DhBlockPos2D targetPos, Operation<Void> original) {
        original.call(SeamTarget.of(DhShapes.of(this.level), targetPos));
    }

    @WrapOperation(method = "lambda$tryStartNextWorldGenTask$0", at = @At(value = "INVOKE",
            target = "Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;chebyshevDist(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;)I"))
    private static int exotic_worlds$seamChebyshevDist(DhBlockPos2D center, DhBlockPos2D target,
            Operation<Integer> original) {
        return target instanceof SeamTarget seam ? seam.chebyshevDistFrom(center) : original.call(center, target);
    }
}
