package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.DoubleStack;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import net.minecraft.core.Direction;

public final class RtfLap {
    public static final double OPEN = 0.0;

    public static final int NO_PERIOD = 0;

    private static final int TORUS_FLOOR_CELLS = 2;

    private static final int CYLINDER_FLOOR_CELLS = 1;

    private static final double CEIL_SLACK = 1.0E-9;

    private static final double TWO_PI = Math.PI * 2.0;

    // Read before every lattice hash, so a world that never wraps ReTerraForged pays no thread-local lookup.
    private static volatile boolean active;

    private static final ThreadLocal<Frame> FRAME = ThreadLocal.withInitial(Frame::new);

    public static void activate() {
        active = true;
    }

    public static boolean active() {
        return active;
    }

    public static Frame frame() {
        return FRAME.get();
    }

    public static @Nullable Frame boundFrame() {
        if (!active) {
            return null;
        }

        Frame frame = FRAME.get();
        frame.followGeneration();
        return frame.bound() ? frame : null;
    }

    public static final class Frame {
        private double xLap = OPEN;
        private double zLap = OPEN;
        private boolean torus;
        private int xPeriod = NO_PERIOD;
        private int zPeriod = NO_PERIOD;
        private float xScale;
        private float zScale;
        private double compression = ClimateScaleCompression.NO_COMPRESSION;
        private final DoubleStack saved = new DoubleStack();
        private final Scope scope = new Scope();
        private @Nullable WorldFold followed;

        public boolean bound() {
            return this.xLap != OPEN || this.zLap != OPEN;
        }

        public double lap(Direction.Axis axis) {
            return axis == Direction.Axis.X ? this.xLap : this.zLap;
        }

        public int xPeriod() {
            return this.xPeriod;
        }

        public int zPeriod() {
            return this.zPeriod;
        }

        public Scope bind(WorldFold fold) {
            push();
            seat(fold);
            return this.scope;
        }

        void followGeneration() {
            if (!this.saved.isEmpty()) {
                return;
            }

            WorldFold fold = GenerationTransformerContext.context().wrappedTransformer();
            if (fold != this.followed) {
                this.followed = fold;
                seat(fold);
            }
        }

        private void seat(@Nullable WorldFold fold) {
            this.xLap = fold == null ? OPEN : lapOf(fold.blockDomain(Direction.Axis.X));
            this.zLap = fold == null ? OPEN : lapOf(fold.blockDomain(Direction.Axis.Z));
            this.torus = this.xLap != OPEN && this.zLap != OPEN;
            this.xPeriod = NO_PERIOD;
            this.zPeriod = NO_PERIOD;
            this.compression = ClimateScaleCompression.NO_COMPRESSION;
        }

        public double compression() {
            return this.compression;
        }

        public Scope compress(double factor) {
            push();
            this.xLap *= factor;
            this.zLap *= factor;
            this.compression = factor;
            return this.scope;
        }

        public Scope expand() {
            push();
            this.xLap /= this.compression;
            this.zLap /= this.compression;
            this.compression = ClimateScaleCompression.NO_COMPRESSION;
            return this.scope;
        }

        public Scope scale(double xScale, double zScale) {
            push();
            this.xLap *= xScale;
            this.zLap *= zScale;
            return this.scope;
        }

        public Scope open() {
            push();
            this.xLap = OPEN;
            this.zLap = OPEN;
            this.xPeriod = NO_PERIOD;
            this.zPeriod = NO_PERIOD;
            return this.scope;
        }

        public Scope lattice(int xCells, int zCells) {
            push();
            this.xPeriod = xCells;
            this.zPeriod = zCells;
            return this.scope;
        }

        // The scales are read back at once, before anything the lattice samples can open an octave of its own.
        public Scope octave(double frequency) {
            int xCells = cells(Direction.Axis.X, frequency);
            int zCells = cells(Direction.Axis.Z, frequency);
            this.xScale = snapped(Direction.Axis.X, xCells, (float) frequency);
            this.zScale = snapped(Direction.Axis.Z, zCells, (float) frequency);
            return lattice(xCells, zCells);
        }

        public float xScale() {
            return this.xScale;
        }

        public float zScale() {
            return this.zScale;
        }

        public int cells(Direction.Axis axis, double frequency) {
            return cellsOver(lap(axis), frequency, this.torus ? TORUS_FLOOR_CELLS : CYLINDER_FLOOR_CELLS);
        }

        public float snapped(Direction.Axis axis, int cells, float frequency) {
            double lap = lap(axis);
            return lap == OPEN ? frequency : (float) (cells / lap);
        }

        public float snappedFrequency(Direction.Axis axis, float frequency) {
            return snapped(axis, cells(axis, frequency), frequency);
        }

        public float snappedAngular(Direction.Axis axis, float frequency) {
            double lap = lap(axis);
            if (lap == OPEN) {
                return frequency;
            }

            int turns = cellsOver(lap, frequency / TWO_PI, CYLINDER_FLOOR_CELLS);
            return (float) (turns * TWO_PI / lap);
        }

        public int fold(Direction.Axis axis, int cell) {
            int period = axis == Direction.Axis.X ? this.xPeriod : this.zPeriod;
            return period == NO_PERIOD ? cell : Math.floorMod(cell, period);
        }

        // ReTerraForged floors and rounds a negative lattice coordinate away from zero, so a copy one lap to the
        // negative side would land on another cell than its twin; whole laps move every coordinate onto one side.
        public float shift(Direction.Axis axis, float coord) {
            double lap = lap(axis);
            return lap == OPEN ? coord : (float) (coord - lap * Math.floor(coord / lap));
        }

        public double seat(Direction.Axis axis, double coord, double anchor) {
            double lap = lap(axis);
            return lap == OPEN ? coord : coord + lap * Math.rint((anchor - coord) / lap);
        }

        private void push() {
            this.saved.push(this.xLap);
            this.saved.push(this.zLap);
            this.saved.push(this.torus ? 1.0 : 0.0);
            this.saved.push(this.xPeriod);
            this.saved.push(this.zPeriod);
            this.saved.push(this.compression);
        }

        private void pop() {
            this.compression = this.saved.pop();
            this.zPeriod = (int) this.saved.pop();
            this.xPeriod = (int) this.saved.pop();
            this.torus = this.saved.pop() != 0.0;
            this.zLap = this.saved.pop();
            this.xLap = this.saved.pop();
        }

        public final class Scope implements AutoCloseable {
            private Scope() {
            }

            @Override
            public void close() {
                pop();
            }
        }
    }

    public static int wrapLatticeX(int x) {
        return active ? FRAME.get().fold(Direction.Axis.X, x) : x;
    }

    public static int wrapLatticeZ(int z) {
        return active ? FRAME.get().fold(Direction.Axis.Z, z) : z;
    }

    static int cellsOver(double lap, double frequency, int floorCells) {
        if (lap == OPEN) {
            return NO_PERIOD;
        }

        return Math.max(floorCells, (int) Math.ceil(lap * Math.abs(frequency) - CEIL_SLACK));
    }

    private static double lapOf(WrapDomain domain) {
        return domain.loops() ? domain.domainLength : OPEN;
    }

    private RtfLap() {
    }
}
