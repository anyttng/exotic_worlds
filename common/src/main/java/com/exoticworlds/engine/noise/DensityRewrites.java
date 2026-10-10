package com.exoticworlds.engine.noise;

import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.StartupRegistry;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
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
                walk.walk(router.barrierNoise()),
                walk.walk(router.fluidLevelFloodednessNoise()),
                walk.walk(router.fluidLevelSpreadNoise()),
                walk.walk(router.lavaNoise()),
                walk.walk(router.temperature()),
                walk.walk(router.vegetation()),
                walk.walk(router.continents()),
                walk.walk(router.erosion()),
                walk.walk(router.depth()),
                walk.walk(router.ridges()),
                walk.walk(router.preliminarySurfaceLevel()),
                walk.walk(router.finalDensity()),
                walk.walk(router.veinToggle()),
                walk.walk(router.veinRidged()),
                walk.walk(router.veinGap()));
    }

    public static DensityFunction replaceKeyed(DensityFunction function, ResourceKey<DensityFunction> key,
            DensityFunction replacement) {
        return new KeyedWalk(found -> found.equals(key) ? ignored -> replacement : null).walk(function);
    }

    public static @Nullable DensityFunction rewriteKeyed(DensityFunction.Visitor visitor,
            Holder<DensityFunction> holder) {
        return visitor instanceof KeyedWalk walk ? walk.rewrite(holder) : null;
    }

    // This game version's mapAll unwraps a holder before the visitor sees it, so the key is read in HolderHolder's own mapAll.
    private static final class KeyedWalk implements DensityFunction.Visitor {
        private final Function<ResourceKey<DensityFunction>, @Nullable Function<DensityFunction, DensityFunction>> lookup;

        private int replaced;

        KeyedWalk(Function<ResourceKey<DensityFunction>, @Nullable Function<DensityFunction, DensityFunction>> lookup) {
            this.lookup = lookup;
        }

        DensityFunction walk(DensityFunction function) {
            int before = this.replaced;
            DensityFunction mapped = function.mapAll(this);
            return this.replaced == before ? function : mapped;
        }

        @Override
        public DensityFunction apply(DensityFunction function) {
            return function;
        }

        @Nullable DensityFunction rewrite(Holder<DensityFunction> holder) {
            Function<DensityFunction, DensityFunction> rewrite = holder.unwrapKey().map(this.lookup).orElse(null);
            if (rewrite == null) {
                return null;
            }

            this.replaced++;
            return rewrite.apply(holder.value());
        }
    }

    private DensityRewrites() {
    }
}
