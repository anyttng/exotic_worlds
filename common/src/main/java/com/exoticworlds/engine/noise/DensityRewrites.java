package com.exoticworlds.engine.noise;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.StartupRegistry;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;

public final class DensityRewrites {

    @FunctionalInterface
    public interface Rewrite {
        DensityFunction rewrite(DensityFunction function, WorldFold fold, NoiseRouter router);
    }

    private static final StartupRegistry<ResourceKey<DensityFunction>, Rewrite> REWRITES =
            new StartupRegistry<>("Density function rewrites");

    public static void register(ResourceKey<DensityFunction> key, Rewrite rewrite) {
        REWRITES.register(key, rewrite);
    }

    public static NoiseRouter apply(NoiseRouter router, WorldFold fold) {
        Map<ResourceKey<DensityFunction>, Rewrite> rewrites = REWRITES.entries();
        if (rewrites.isEmpty()) {
            return router;
        }

        KeyedWalk walk = new KeyedWalk(key -> {
            Rewrite rewrite = rewrites.get(key);
            return rewrite == null ? null : function -> rewrite.rewrite(function, fold, router);
        });
        return new NoiseRouter(
                walk.apply(router.barrierNoise()),
                walk.apply(router.fluidLevelFloodednessNoise()),
                walk.apply(router.fluidLevelSpreadNoise()),
                walk.apply(router.lavaNoise()),
                walk.apply(router.temperature()),
                walk.apply(router.vegetation()),
                walk.apply(router.continents()),
                walk.apply(router.erosion()),
                walk.apply(router.depth()),
                walk.apply(router.ridges()),
                walk.apply(router.preliminarySurfaceLevel()),
                walk.apply(router.finalDensity()),
                walk.apply(router.veinToggle()),
                walk.apply(router.veinRidged()),
                walk.apply(router.veinGap()));
    }

    public static DensityFunction replaceKeyed(DensityFunction function, ResourceKey<DensityFunction> key,
            DensityFunction replacement) {
        return new KeyedWalk(found -> found.equals(key) ? ignored -> replacement : null).apply(function);
    }

    private static final class KeyedWalk implements DensityFunction.Visitor {
        private final Function<ResourceKey<DensityFunction>, @Nullable Function<DensityFunction, DensityFunction>> lookup;

        private int replaced;

        KeyedWalk(Function<ResourceKey<DensityFunction>, @Nullable Function<DensityFunction, DensityFunction>> lookup) {
            this.lookup = lookup;
        }

        @Override
        public DensityFunction apply(DensityFunction function) {
            if (function instanceof DensityFunctions.HolderHolder holder) {
                Holder<DensityFunction> referenced = holder.function();
                Optional<ResourceKey<DensityFunction>> key = referenced.unwrapKey();
                Function<DensityFunction, DensityFunction> rewrite = key.map(this.lookup).orElse(null);
                if (rewrite != null) {
                    this.replaced++;
                    return rewrite.apply(referenced.value());
                }

                DensityFunction value = referenced.value();
                DensityFunction walked = apply(value);
                return walked == value ? holder : new DensityFunctions.HolderHolder(Holder.direct(walked));
            }

            int before = this.replaced;
            DensityFunction mapped = function.mapChildren(this);
            return this.replaced == before ? function : mapped;
        }
    }

    private DensityRewrites() {
    }
}
