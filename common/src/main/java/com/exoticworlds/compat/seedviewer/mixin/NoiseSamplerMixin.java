package com.exoticworlds.compat.seedviewer.mixin;

import java.util.EnumMap;
import java.util.Map;

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

import net.acenia.seedviewer.client.worldgen.NoiseMapType;
import net.acenia.seedviewer.client.worldgen.NoiseSampler;
import net.minecraft.server.level.ServerLevel;

@Mixin(NoiseSampler.class)
public class NoiseSamplerMixin implements FoldedNoiseSampler {
    @Unique
    private WorldFold exotic_worlds$fold = WorldFolds.NOOP;

    @Unique
    private Map<NoiseMapType, FoldedSampleCache<Float>> exotic_worlds$caches = Map.of();

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_RANDOM_STATE,
            at = @At("RETURN"))
    private void exotic_worlds$takeTheRouterBuildsFold(CallbackInfo callback) {
        exotic_worlds$bind(CreationShape.routerBuildFold());
    }

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_LEVEL,
            at = @At("RETURN"))
    private void exotic_worlds$takeTheLevelsFold(ServerLevel level, CallbackInfo callback) {
        exotic_worlds$bind(WorldLoopAttachments.foldOf(level));
    }

    @Override
    public WorldFold exotic_worlds$fold() {
        return this.exotic_worlds$fold;
    }

    @Override
    public FoldedSampleCache<Float> exotic_worlds$cache(NoiseMapType type) {
        return this.exotic_worlds$caches.get(type);
    }

    @Unique
    private void exotic_worlds$bind(WorldFold fold) {
        Map<NoiseMapType, FoldedSampleCache<Float>> caches = new EnumMap<>(NoiseMapType.class);
        if (fold.isWrapped()) {
            for (NoiseMapType type : NoiseMapType.values()) {
                caches.put(type, FoldedSampleCache.perBlock(fold));
            }
        }

        this.exotic_worlds$fold = fold;
        this.exotic_worlds$caches = caches;
    }
}
