package com.exoticworlds.compat.biolith;

import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.LevelClimateCompression;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;
import com.exoticworlds.engine.noise.LapFloor;
import com.exoticworlds.engine.noise.NoiseFrame;
import com.exoticworlds.engine.noise.PeriodicNoiseSampler;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class ReplacementNoiseFold {
    public static final double NOT_FOLDED = Double.NaN;

    private static final NoiseFrame UNDAMPED_FRAME = NoiseFrame.UNDECLARED;

    private static final double OPEN_SIMPLEX_FLAT_AMPLITUDE = 1.971;

    private static final double OPEN_SIMPLEX_VOLUME_AMPLITUDE = 1.489;

    private static final double OPEN_SIMPLEX_ROW_RATE = 0.667;

    private static final double OPEN_SIMPLEX_COLUMN_RATE = 0.707;

    private static final int FLAT_QUART_Y = 0;

    private static final double BLOCKS_PER_QUART = 4.0;

    private final double[] weights;

    private final int[] horizontalScaleSlots;

    private final int[] verticalScaleSlots;

    private volatile @Nullable Octaves octaves;

    private volatile @Nullable Compression compression;

    public ReplacementNoiseFold(double... weights) {
        this(weights, IntStream.range(0, weights.length).toArray(), IntStream.range(0, weights.length).toArray());
    }

    public ReplacementNoiseFold(double[] weights, int[] horizontalScaleSlots, int[] verticalScaleSlots) {
        this.weights = weights.clone();
        this.horizontalScaleSlots = horizontalScaleSlots.clone();
        this.verticalScaleSlots = verticalScaleSlots.clone();
    }

    public static boolean folded(double sum) {
        return !Double.isNaN(sum);
    }

    public double sum(long seed, double[] scaleQuarts, int quartX, int quartZ, DensityFunction temperature) {
        return sum(seed, scaleQuarts, quartX, FLAT_QUART_Y, quartZ, OPEN_SIMPLEX_FLAT_AMPLITUDE, temperature);
    }

    public double sum(long seed, double[] scaleQuarts, int quartX, int quartY, int quartZ,
            DensityFunction temperature) {
        return sum(seed, scaleQuarts, quartX, quartY, quartZ, OPEN_SIMPLEX_VOLUME_AMPLITUDE, temperature);
    }

    double compression(WorldFold fold, DensityFunction temperature) {
        Compression held = this.compression;
        if (held != null && held.temperature() == temperature && held.fold().equals(fold)) {
            return held.factor();
        }

        double factor = LevelClimateCompression.factor(fold, temperature);
        this.compression = new Compression(fold, temperature, factor);
        return factor;
    }

    private double sum(long seed, double[] scaleQuarts, int quartX, int quartY, int quartZ, double amplitude,
            DensityFunction temperature) {
        WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
        if (fold == null) {
            return NOT_FOLDED;
        }

        double factor = compression(fold, temperature);
        Octaves resolved = octavesFor(seed);
        double x = QuartPos.toBlock(quartX);
        double y = QuartPos.toBlock(quartY);
        double z = QuartPos.toBlock(quartZ);
        double sum = 0.0;
        for (int octave = 0; octave < this.weights.length; octave++) {
            PerlinNoise noise = resolved.noises()[octave];
            double horizontalQuarts = scaleQuarts[this.horizontalScaleSlots[octave]] * OPEN_SIMPLEX_ROW_RATE / factor;
            double scale = 1.0 / (horizontalQuarts * BLOCKS_PER_QUART);
            double verticalQuarts = scaleQuarts[this.verticalScaleSlots[octave]] * OPEN_SIMPLEX_COLUMN_RATE / factor;
            double verticalScale = 1.0 / (verticalQuarts * BLOCKS_PER_QUART);
            sum += this.weights[octave] * PeriodicNoiseSampler.sample(noise.perms, noise.offsetX, noise.offsetY,
                    noise.offsetZ, fold, UNDAMPED_FRAME, scale, x, GradientNoise.wrap(y * verticalScale), z,
                    LapFloor.of(fold));
        }

        return sum * amplitude;
    }

    private Octaves octavesFor(long seed) {
        Octaves held = this.octaves;
        if (held != null && held.seed() == seed) {
            return held;
        }

        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed));
        PerlinNoise[] noises = new PerlinNoise[this.weights.length];
        for (int octave = 0; octave < noises.length; octave++) {
            noises[octave] = new PerlinNoise(random);
        }

        Octaves built = new Octaves(seed, noises);
        this.octaves = built;
        return built;
    }

    private record Octaves(long seed, PerlinNoise[] noises) {
    }

    private record Compression(WorldFold fold, DensityFunction temperature, double factor) {
    }
}
