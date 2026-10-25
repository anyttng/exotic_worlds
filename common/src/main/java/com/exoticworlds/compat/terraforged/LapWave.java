package com.exoticworlds.compat.terraforged;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;

public record LapWave(double xLap, double zLap, double skew) {
    public static final long NO_TURNS = 0L;

    private static final int REACH = 2;

    private static final int REMEMBERED_WAVES = 4;

    private static final ThreadLocal<Remembered> REMEMBERED = ThreadLocal.withInitial(Remembered::new);

    public record Wave(double x, double z, long xTurns, long zTurns) {
        public double frequency() {
            return Math.hypot(this.x, this.z);
        }
    }

    public static @Nullable LapWave bound() {
        RtfLap.Frame frame = RtfLap.boundFrame();
        return frame == null ? null
                : new LapWave(frame.lap(Direction.Axis.X), frame.lap(Direction.Axis.Z), frame.entrySkew());
    }

    public static float directionOn(Direction.Axis axis, float cos, float sin, float wavelength) {
        LapWave lap = bound();
        if (lap == null) {
            return axis == Direction.Axis.X ? cos : sin;
        }

        Wave wave = REMEMBERED.get().noLonger(lap, cos / wavelength, sin / wavelength);
        return (float) ((axis == Direction.Axis.X ? wave.x() : wave.z()) * wavelength);
    }

    public boolean loops(Direction.Axis axis) {
        return (axis == Direction.Axis.X ? this.xLap : this.zLap) != RtfLap.OPEN;
    }

    public Wave noLonger(double x, double z) {
        double frequency = Math.hypot(x, z);
        return nearest(x, z, wave -> wave.frequency() >= frequency);
    }

    public Wave nearest(double x, double z, Predicate<Wave> accepted) {
        boolean xLoops = loops(Direction.Axis.X);
        boolean zLoops = loops(Direction.Axis.Z);
        long xFirst = xLoops ? (long) Math.floor(x * this.xLap) - REACH + 1 : NO_TURNS;
        long zFirst = zLoops ? (long) Math.floor(x * this.skew + z * this.zLap) - REACH + 1 : NO_TURNS;
        long xLast = xLoops ? xFirst + 2 * REACH - 1 : NO_TURNS;
        long zLast = zLoops ? zFirst + 2 * REACH - 1 : NO_TURNS;
        @Nullable Wave best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (long xTurns = xFirst; xTurns <= xLast; xTurns++) {
            for (long zTurns = zFirst; zTurns <= zLast; zTurns++) {
                double waveX = xLoops ? xTurns / this.xLap : x;
                double waveZ = zLoops ? (zTurns - waveX * this.skew) / this.zLap : z;
                Wave wave = new Wave(waveX, waveZ, xTurns, zTurns);
                double distance = (waveX - x) * (waveX - x) + (waveZ - z) * (waveZ - z);
                if (distance < bestDistance && accepted.test(wave)) {
                    best = wave;
                    bestDistance = distance;
                }
            }
        }

        if (best == null) {
            throw new IllegalStateException("No wave within " + REACH + " turns of (" + x + ", " + z
                    + ") closes on the lap " + this);
        }

        return best;
    }

    private static final class Remembered {
        private final @Nullable LapWave[] laps = new LapWave[REMEMBERED_WAVES];
        private final double[] xs = new double[REMEMBERED_WAVES];
        private final double[] zs = new double[REMEMBERED_WAVES];
        private final Wave[] waves = new Wave[REMEMBERED_WAVES];
        private int next;

        Wave noLonger(LapWave lap, double x, double z) {
            for (int slot = 0; slot < REMEMBERED_WAVES; slot++) {
                if (this.xs[slot] == x && this.zs[slot] == z && lap.equals(this.laps[slot])) {
                    return this.waves[slot];
                }
            }

            int slot = this.next;
            this.next = (slot + 1) % REMEMBERED_WAVES;
            this.laps[slot] = lap;
            this.xs[slot] = x;
            this.zs[slot] = z;
            this.waves[slot] = lap.noLonger(x, z);
            return this.waves[slot];
        }
    }
}
