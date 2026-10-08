package com.exoticworlds.compat.seedviewer.mixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.seedviewer.CreationShape;
import com.exoticworlds.compat.seedviewer.FoldedNoiseSampler;
import com.exoticworlds.compat.seedviewer.FoldedSampleCache;
import com.exoticworlds.compat.seedviewer.SeedViewerInjectionTargets;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;

import net.acenia.seedviewer.client.worldgen.NoiseSampler;
import net.minecraft.server.level.ServerLevel;

@Mixin(NoiseSampler.class)
public class NoiseSamplerMixin implements FoldedNoiseSampler {
    @Unique
    private WorldFold toroidal$fold = WorldFolds.NOOP;

    @Unique
    private final Map<String, FoldedSampleCache<Float>> toroidal$caches = new ConcurrentHashMap<>();

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_RANDOM_STATE,
            at = @At("RETURN"))
    private void toroidal$takeTheRouterBuildsFold(CallbackInfo callback) {
        this.toroidal$fold = CreationShape.routerBuildFold();
    }

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_LEVEL,
            at = @At("RETURN"))
    private void toroidal$takeTheLevelsFold(ServerLevel level, CallbackInfo callback) {
        this.toroidal$fold = WorldLoopAttachments.transformerOf(level);
    }

    @Override
    public WorldFold toroidal$fold() {
        return this.toroidal$fold;
    }

    @Override
    public @Nullable FoldedSampleCache<Float> toroidal$cache(String layer) {
        return this.toroidal$fold.isWrapped()
                ? this.toroidal$caches.computeIfAbsent(layer, name -> FoldedSampleCache.perBlock(this.toroidal$fold))
                : null;
    }
}
