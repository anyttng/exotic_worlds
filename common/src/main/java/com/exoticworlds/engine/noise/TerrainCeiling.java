package com.exoticworlds.engine.noise;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DensityFunctionNodes;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.op.BinaryFunction;

public final class TerrainCeiling {
    private static final String VANILLA_NAMESPACE = "minecraft";

    private static final Map<String, Double> LADDER_BASE_BLOCKS = Map.of(
            "overworld/jaggedness", 40.0,
            "overworld_large_biomes/jaggedness", 55.0);

    private static final double BLOCKS_PER_DENSITY_UNIT = 128.0;

    private static final float RAMP_BLOCKS = 16.0F;

    private static final float PENALTY = 0.25F;

    private static final int CELL_WIDTH = 4;

    private static final int CELL_HEIGHT = 8;

    @SuppressWarnings("deprecation")
    public static NoiseGeneratorSettings withCeiling(NoiseGeneratorSettings settings) {
        NoiseRouter ceilinged = withCeiling(settings.noiseRouter());
        return ceilinged == settings.noiseRouter()
                ? settings
                : new NoiseGeneratorSettings(
                        settings.noiseSettings(),
                        settings.defaultBlock(),
                        settings.defaultFluid(),
                        ceilinged,
                        settings.materialRule(),
                        settings.spawnTarget(),
                        settings.seaLevel(),
                        settings.disableMobGeneration(),
                        settings.aquifers(),
                        settings.useLegacyRandomSource(),
                        settings.debugFunctions());
    }

    public static @Nullable DensityFunction ceiling(NoiseGeneratorSettings settings) {
        return ceilingOf(settings.noiseRouter());
    }

    private static @Nullable DensityFunction ceilingOf(NoiseRouter source) {
        BinaryFunction jaggedness = jaggednessProduct(source.finalDensity());
        if (jaggedness == null) {
            return null;
        }

        DensityFunction spline = jaggednessSpline(jaggedness);
        Double base = baseOf(spline);
        if (base == null) {
            return null;
        }

        DensityFunction noise = spline == jaggedness.left() ? jaggedness.right() : jaggedness.left();
        double lift = BLOCKS_PER_DENSITY_UNIT * noise.range().max();
        if (!Double.isFinite(lift)) {
            return null;
        }

        DensityFunction nonNegativeSpline = spline.range().min() < 0.0F
                ? DensityFunctions.max(spline, DensityFunctions.zero())
                : spline;
        DensityFunction headroom = DensityFunctions.mul(DensityFunctions.constant((float) lift), nonNegativeSpline);
        return DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.cache(source.chunkSurfaceLevel()),
                        DensityFunctions.constant(base.floatValue())),
                headroom);
    }

    static NoiseRouter withCeiling(NoiseRouter source) {
        DensityFunction ceiling = ceilingOf(source);
        if (ceiling == null) {
            return source;
        }

        DensityFunction aboveCeiling = DensityFunctions.sub(worldY(), ceiling);
        DensityFunction ramp = DensityFunctions
                .mul(DensityFunctions.interpolated(aboveCeiling, CELL_WIDTH, CELL_HEIGHT),
                        DensityFunctions.constant(1.0F / RAMP_BLOCKS))
                .clamp(0.0F, 1.0F);
        return withFinalDensity(source, DensityFunctions.add(source.finalDensity(),
                DensityFunctions.mul(ramp, DensityFunctions.constant(-PENALTY))));
    }

    private static NoiseRouter withFinalDensity(NoiseRouter source, DensityFunction finalDensity) {
        return new NoiseRouter(
                source.temperature(),
                source.vegetation(),
                source.continents(),
                source.erosion(),
                source.depth(),
                source.ridges(),
                source.chunkSurfaceLevel(),
                finalDensity);
    }

    private static DensityFunction worldY() {
        int belowBottom = DimensionType.MIN_Y * 2;
        int aboveTop = DimensionType.MAX_Y * 2;
        return DensityFunctions.yClampedGradient(belowBottom, aboveTop, belowBottom, aboveTop);
    }

    private static @Nullable BinaryFunction jaggednessProduct(DensityFunction root) {
        return (BinaryFunction) DensityFunctionNodes.first(root, node -> node instanceof BinaryFunction candidate
                && candidate.type() == BinaryFunction.Type.MUL
                && (isJaggedness(candidate.left()) || isJaggedness(candidate.right())));
    }

    private static DensityFunction jaggednessSpline(BinaryFunction product) {
        return isJaggedness(product.left()) ? product.left() : product.right();
    }

    private static boolean isJaggedness(DensityFunction function) {
        return baseOf(function) != null;
    }

    private static @Nullable Double baseOf(DensityFunction function) {
        if (!(function instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder))) {
            return null;
        }

        ResourceKey<DensityFunction> key = holder.unwrapKey().orElse(null);
        return key != null && key.identifier().getNamespace().equals(VANILLA_NAMESPACE)
                ? LADDER_BASE_BLOCKS.get(key.identifier().getPath())
                : null;
    }

    private TerrainCeiling() {
    }
}
