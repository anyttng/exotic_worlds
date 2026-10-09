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
    private static final String GENERATOR =
            "Lcom/seibel/distanthorizons/api/interfaces/override/worldGenerator/IDhApiWorldGenerator;";

    private static final String MODE_POOL_CONSUMER =
            "Lcom/seibel/distanthorizons/api/enums/worldGeneration/EDhApiDistantGeneratorMode;"
                    + "Ljava/util/concurrent/ExecutorService;Ljava/util/function/Consumer;)"
                    + "Ljava/util/concurrent/CompletableFuture;";

    private static final int CHUNKS_POOL_INDEX = 5;

    private static final int LOD_POOL_INDEX = 7;

    @Shadow
    @Final
    private IDhServerLevel level;

    @ModifyArg(method = "startVanillaChunkGenerationEvent", index = CHUNKS_POOL_INDEX,
            at = @At(value = "INVOKE", target = GENERATOR + "generateChunks(IIIB" + MODE_POOL_CONSUMER))
    private ExecutorService toroidal$foldVanillaChunkPool(ExecutorService pool) {
        return this.toroidal$bindLevelFold(pool);
    }

    @ModifyArg(method = "startApiChunkGenerationEvent", index = CHUNKS_POOL_INDEX,
            at = @At(value = "INVOKE", target = GENERATOR + "generateApiChunks(IIIB" + MODE_POOL_CONSUMER))
    private ExecutorService toroidal$foldApiChunkPool(ExecutorService pool) {
        return this.toroidal$bindLevelFold(pool);
    }

    @ModifyArg(method = "startApiDataSourceGenerationEvent", index = LOD_POOL_INDEX,
            at = @At(value = "INVOKE", target = GENERATOR + "generateLod(IIIIB"
                    + "Lcom/seibel/distanthorizons/api/objects/data/IDhApiFullDataSource;" + MODE_POOL_CONSUMER))
    private ExecutorService toroidal$foldLodPool(ExecutorService pool) {
        return this.toroidal$bindLevelFold(pool);
    }

    @Unique
    private ExecutorService toroidal$bindLevelFold(ExecutorService pool) {
        WorldFold fold = DhShapes.serverFoldOf(this.level);
        return fold == null ? pool : new FoldBindingExecutor(pool, fold);
    }

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
