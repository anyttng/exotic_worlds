package com.exoticworlds.engine.noise;

import com.exoticworlds.engine.noise.BlendedNoiseFixture.Params;
import com.exoticworlds.engine.noise.BlendedNoiseFixture.Replica;
import static com.exoticworlds.engine.noise.LayeredNoise.Stats.variance;
import static com.exoticworlds.engine.noise.LayeredNoise.WORLD;
import static com.exoticworlds.engine.noise.LayeredNoise.WORLD_BLOCKS;

final class BlendedFoldCalibration {
    private record BlendedDraw(Replica replica, double windowX, double windowZ) {
        static BlendedDraw next(int seedIndex, java.util.Random windows) {
            Replica replica = Replica.of(0xB1E4DEDL + seedIndex * 7919L);
            return new BlendedDraw(replica, windows.nextDouble() * 1.0E6, windows.nextDouble() * 1.0E6);
        }
    }

    private static final class BlendedTally {
        private final double[] seedMeans;
        private double sum;
        private double squareSum;
        private long above02;
        private long above03;
        private double maxSum;
        private long points;
        private double seedSum;
        private double seedMax = -Double.MAX_VALUE;

        BlendedTally(int seeds) {
            this.seedMeans = new double[seeds];
        }

        void add(double value) {
            this.sum += value;
            this.squareSum += value * value;
            if (value > 0.2) {
                this.above02++;
            }
            if (value > 0.3) {
                this.above03++;
            }
            this.seedSum += value;
            this.seedMax = Math.max(this.seedMax, value);
            this.points++;
        }

        void endSeed(int seed, int samples) {
            this.seedMeans[seed] = this.seedSum / samples;
            this.maxSum += this.seedMax;
            this.seedSum = 0.0;
            this.seedMax = -Double.MAX_VALUE;
        }
    }

    static void append(StringBuilder report) {
        int seeds = 256;
        int grid = 32;
        int[] ySlices = {100, 150};
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("blended-noise fold calibration (full walk, 512-block lap, ").append(seeds)
                .append(" seeds x ").append(grid).append("x").append(grid).append(" grid):\n");
        for (int ySlice : ySlices) {
            BlendedTally vanilla = new BlendedTally(seeds);
            BlendedTally folded = new BlendedTally(seeds);
            for (int s = 0; s < seeds; s++) {
                BlendedDraw draw = BlendedDraw.next(s, windows);
                tallyBlendedSlice(draw, ySlice, grid, vanilla, folded);
                vanilla.endSeed(s, grid * grid);
                folded.endSeed(s, grid * grid);
            }
            report.append(blendedLine("vanilla:", ySlice, vanilla, seeds));
            report.append(blendedLine("folded: ", ySlice, folded, seeds));
        }
        report.append('\n');
    }

    private static void tallyBlendedSlice(BlendedDraw draw, int ySlice, int grid, BlendedTally vanilla,
            BlendedTally folded) {
        for (int i = 0; i < grid; i++) {
            double x = i * (WORLD_BLOCKS / (double) grid);
            for (int j = 0; j < grid; j++) {
                double z = j * (WORLD_BLOCKS / (double) grid);
                vanilla.add(BlendedNoiseFixture.vanilla(draw.replica(), Params.OVERWORLD, draw.windowX() + x, ySlice,
                        draw.windowZ() + z));
                folded.add(BlendedNoiseFixture.folded(draw.replica(), Params.OVERWORLD, WORLD, x - 256.0, ySlice,
                        z - 256.0));
            }
        }
    }

    private static String blendedLine(String label, int ySlice, BlendedTally tally, int seeds) {
        long points = tally.points;
        return String.format(
                "  y=%d %s mean=%+.4f std=%.4f tail02=%.3f%% tail03=%.3f%% mean_spread=%.4f avg_max=%.4f%n",
                ySlice, label, tally.sum / points,
                Math.sqrt(tally.squareSum / points - Math.pow(tally.sum / points, 2)),
                100.0 * tally.above02 / points, 100.0 * tally.above03 / points,
                Math.sqrt(variance(tally.seedMeans)), tally.maxSum / seeds);
    }

    private BlendedFoldCalibration() {
    }
}
