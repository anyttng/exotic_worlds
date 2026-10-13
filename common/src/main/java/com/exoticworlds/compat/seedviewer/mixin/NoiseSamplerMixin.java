package com.exoticworlds.compat.seedviewer.mixin;

import java.util.EnumMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.CreationShape;
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
    private WorldFold toroidal$fold = WorldFolds.NOOP;

    @Unique
    private Map<NoiseMapType, FoldedSampleCache<Float>> toroidal$caches = Map.of();

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_RANDOM_STATE,
            at = @At("RETURN"))
    private void toroidal$takeTheRouterBuildsFold(CallbackInfo callback) {
        toroidal$bind(CreationShape.routerBuildFold());
    }

    @Inject(method = SeedViewerInjectionTargets.CONSTRUCTOR + SeedViewerInjectionTargets.FROM_LEVEL,
            at = @At("RETURN"))
    private void toroidal$takeTheLevelsFold(ServerLevel level, CallbackInfo callback) {
        toroidal$bind(WorldLoopAttachments.transformerOf(level));
    }

    @Override
    public WorldFold toroidal$fold() {
        return this.toroidal$fold;
    }

    @Override
    public FoldedSampleCache<Float> toroidal$cache(NoiseMapType type) {
        return this.toroidal$caches.get(type);
    }

    @Unique
    private void toroidal$bind(WorldFold fold) {
        Map<NoiseMapType, FoldedSampleCache<Float>> caches = new EnumMap<>(NoiseMapType.class);
        if (fold.isWrapped()) {
            for (NoiseMapType type : NoiseMapType.values()) {
                caches.put(type, FoldedSampleCache.perBlock(fold));
            }
        }

        this.toroidal$fold = fold;
        this.toroidal$caches = caches;
    }
}
