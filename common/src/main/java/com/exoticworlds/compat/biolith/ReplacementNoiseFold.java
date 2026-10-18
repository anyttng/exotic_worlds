package com.exoticworlds.compat.biolith;

import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.LevelClimateCompression;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.LapFloor;
import com.exoticworlds.engine.noise.NoiseFrame;
import com.exoticworlds.engine.noise.PeriodicLattice;
import com.exoticworlds.engine.noise.PeriodicNoiseSampler;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.synth.GradientNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class ReplacementNoiseFold {
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

    private volatile @Nullable Lattices lattices;

    private volatile @Nullable Compression compression;

    public ReplacementNoiseFold(double... weights) {
        this(weights, IntStream.range(0, weights.length).toArray(), IntStream.range(0, weights.length).toArray());
    }

    public ReplacementNoiseFold(double[] weights, int[] horizontalScaleSlots, int[] verticalScaleSlots) {
        this.weights = weights.clone();
        this.horizontalScaleSlots = horizontalScaleSlots.clone();
        this.verticalScaleSlots = verticalScaleSlots.clone();
    }

    public double sum(WorldFold fold, long seed, double[] scaleQuarts, int quartX, int quartZ,
            DensityFunction temperature) {
        return sum(fold, seed, scaleQuarts, quartX, FLAT_QUART_Y, quartZ, OPEN_SIMPLEX_FLAT_AMPLITUDE, temperature);
    }

    public double sum(WorldFold fold, long seed, double[] scaleQuarts, int quartX, int quartY, int quartZ,
            DensityFunction temperature) {
        return sum(fold, seed, scaleQuarts, quartX, quartY, quartZ, OPEN_SIMPLEX_VOLUME_AMPLITUDE, temperature);
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

    private double sum(WorldFold fold, long seed, double[] scaleQuarts, int quartX, int quartY, int quartZ,
            double amplitude, DensityFunction temperature) {
        double factor = compression(fold, temperature);
        Lattices resolved = latticesFor(fold, factor, seed, scaleQuarts);
        double x = QuartPos.toBlock(quartX);
        double y = QuartPos.toBlock(quartY);
        double z = QuartPos.toBlock(quartZ);
        double sum = 0.0;
        for (int octave = 0; octave < this.weights.length; octave++) {
            PeriodicLattice lattice = resolved.lattices()[octave];
            double octaveY = GradientNoise.wrap(y * resolved.verticalScales()[octave]);
            sum += this.weights[octave] * PeriodicNoiseSampler.sample(lattice, x, octaveY, z, octaveY);
        }

        return sum * amplitude;
    }

    private Lattices latticesFor(WorldFold fold, double factor, long seed, double[] scaleQuarts) {
        Lattices held = this.lattices;
        if (held != null && held.seed() == seed && held.factor() == factor && held.scaleQuarts() == scaleQuarts
                && held.fold().equals(fold)) {
            return held;
        }

        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed));
        LapFloor floor = LapFloor.of(fold);
        PeriodicLattice[] octaveLattices = new PeriodicLattice[this.weights.length];
        double[] verticalScales = new double[this.weights.length];
        for (int octave = 0; octave < octaveLattices.length; octave++) {
            PerlinNoise noise = new PerlinNoise(random);
            double horizontalQuarts = scaleQuarts[this.horizontalScaleSlots[octave]] * OPEN_SIMPLEX_ROW_RATE / factor;
            double scale = 1.0 / (horizontalQuarts * BLOCKS_PER_QUART);
            double verticalQuarts = scaleQuarts[this.verticalScaleSlots[octave]] * OPEN_SIMPLEX_COLUMN_RATE / factor;
            verticalScales[octave] = 1.0 / (verticalQuarts * BLOCKS_PER_QUART);
            octaveLattices[octave] = PeriodicNoiseSampler.lattice(noise.perms, noise.offsetX, noise.offsetY,
                    noise.offsetZ, PeriodicNoiseSampler.NO_FUDGE, fold, UNDAMPED_FRAME, scale, floor);
        }

        Lattices built = new Lattices(fold, factor, seed, scaleQuarts, octaveLattices, verticalScales);
        this.lattices = built;
        return built;
    }

    @SuppressWarnings("ArrayRecordComponent")
    private record Lattices(WorldFold fold, double factor, long seed, double[] scaleQuarts,
            PeriodicLattice[] lattices, double[] verticalScales) {
    }

    private record Compression(WorldFold fold, DensityFunction temperature, double factor) {
    }
}
