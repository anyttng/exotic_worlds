package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.DoubleStack;
import com.exoticworlds.engine.noise.ClimateScaleCompression;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import net.minecraft.core.Direction;

public final class RtfLap {
    public static final double OPEN = 0.0;

    public static final int NO_PERIOD = 0;

    public static final double NO_SKEW = 0.0;

    public static final int NO_SKEW_CELLS = 0;

    private static final int TORUS_FLOOR_CELLS = 2;

    private static final int CYLINDER_FLOOR_CELLS = 1;

    private static final double CEIL_SLACK = 1.0E-9;

    private static final double TWO_PI = Math.PI * 2.0;

    public static final float UNSHEARED = 0.0F;

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
        private double skew = NO_SKEW;
        private double entrySkew = NO_SKEW;
        private boolean torus;
        private int xPeriod = NO_PERIOD;
        private int zPeriod = NO_PERIOD;
        private int skewCells = NO_SKEW_CELLS;
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

        public double skew() {
            return this.skew;
        }

        public int xPeriod() {
            return this.xPeriod;
        }

        public int zPeriod() {
            return this.zPeriod;
        }

        public int skewCells() {
            return this.skewCells;
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
            TranslationLattice lattice = fold == null ? null : fold.blockLattice();
            this.xLap = lattice == null ? OPEN : lapOf(lattice.x());
            this.zLap = lattice == null ? OPEN : lapOf(lattice.z());
            this.skew = lattice == null ? NO_SKEW : lattice.skew();
            this.entrySkew = this.skew;
            this.torus = this.xLap != OPEN && this.zLap != OPEN;
            this.xPeriod = NO_PERIOD;
            this.zPeriod = NO_PERIOD;
            this.skewCells = NO_SKEW_CELLS;
            this.compression = ClimateScaleCompression.NO_COMPRESSION;
        }

        public double compression() {
            return this.compression;
        }

        public Scope compress(double factor) {
            push();
            this.xLap *= factor;
            this.zLap *= factor;
            this.skew *= factor;
            this.entrySkew *= factor;
            this.compression = factor;
            return this.scope;
        }

        public Scope expand() {
            push();
            this.xLap /= this.compression;
            this.zLap /= this.compression;
            this.skew /= this.compression;
            this.entrySkew /= this.compression;
            this.compression = ClimateScaleCompression.NO_COMPRESSION;
            return this.scope;
        }

        public Scope scale(double xScale, double zScale) {
            push();
            this.xLap *= xScale;
            this.zLap *= zScale;
            this.skew *= xScale;
            this.entrySkew *= xScale;
            return this.scope;
        }

        public Scope open() {
            push();
            this.xLap = OPEN;
            this.zLap = OPEN;
            this.skew = NO_SKEW;
            this.entrySkew = NO_SKEW;
            this.xPeriod = NO_PERIOD;
            this.zPeriod = NO_PERIOD;
            this.skewCells = NO_SKEW_CELLS;
            return this.scope;
        }

        // The scales are read back at once, before anything the lattice samples can open an octave of its own.
        public Scope octave(double frequency) {
            int xCells = cells(Direction.Axis.X, frequency);
            int zCells = cells(Direction.Axis.Z, frequency);
            this.xScale = snapped(Direction.Axis.X, xCells, (float) frequency);
            this.zScale = snapped(Direction.Axis.Z, zCells, (float) frequency);
            int wholeCells = (int) Math.round(this.skew * this.xScale);
            push();
            this.xPeriod = xCells;
            this.zPeriod = zCells;
            this.skewCells = wholeCells;
            this.entrySkew = this.skew;
            this.skew = wholeCells == NO_SKEW_CELLS ? NO_SKEW : wholeCells / (double) this.xScale;
            return this.scope;
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

        public float angularShear(float angularX) {
            if (this.skew == NO_SKEW) {
                return UNSHEARED;
            }

            double phase = angularX * this.skew;
            return (float) ((TWO_PI * Math.rint(phase / TWO_PI) - phase) / this.zLap);
        }

        public int foldX(int cellX, int cellZ) {
            if (this.xPeriod == NO_PERIOD) {
                return cellX;
            }

            int laps = this.skewCells == NO_SKEW_CELLS || this.zPeriod == NO_PERIOD
                    ? 0
                    : Math.floorDiv(cellZ, this.zPeriod);
            return Math.floorMod(cellX - laps * this.skewCells, this.xPeriod);
        }

        public int foldZ(int cellZ) {
            return this.zPeriod == NO_PERIOD ? cellZ : Math.floorMod(cellZ, this.zPeriod);
        }

        // ReTerraForged floors and rounds a negative lattice coordinate away from zero, so a copy one lap to the
        // negative side would land on another cell than its twin; whole laps move every coordinate onto one side.
        public float shiftX(float x, float z) {
            return wrapX(this.skew == NO_SKEW ? x : x - zLaps(z) * this.skew);
        }

        public float shiftZ(float z) {
            return this.zLap == OPEN ? z : (float) (z - this.zLap * Math.floor(z / this.zLap));
        }

        public float latticeX(float x, float z) {
            if (this.entrySkew == NO_SKEW && !sheared()) {
                return wrapX(x);
            }

            double laps = zLaps(z);
            return wrapX(x - laps * this.entrySkew
                    + (this.skew - this.entrySkew) * (z - laps * this.zLap) / this.zLap);
        }

        public boolean sheared() {
            return this.skew != this.entrySkew;
        }

        public float outerX(float x, float z) {
            return sheared() ? (float) (x - (this.skew - this.entrySkew) * z / this.zLap) : x;
        }

        public double seatX(double x, double z, double anchorX, double anchorZ) {
            double seated = x + zLapsToward(z, anchorZ) * this.skew;
            return this.xLap == OPEN ? seated : seated + this.xLap * Math.rint((anchorX - seated) / this.xLap);
        }

        public double seatZ(double z, double anchorZ) {
            return z + zLapsToward(z, anchorZ) * this.zLap;
        }

        private double zLaps(double z) {
            return this.zLap == OPEN ? 0.0 : Math.floor(z / this.zLap);
        }

        private double zLapsToward(double z, double anchorZ) {
            return this.zLap == OPEN ? 0.0 : Math.rint((anchorZ - z) / this.zLap);
        }

        private float wrapX(double x) {
            return this.xLap == OPEN ? (float) x : (float) (x - this.xLap * Math.floor(x / this.xLap));
        }

        private void push() {
            this.saved.push(this.xLap);
            this.saved.push(this.zLap);
            this.saved.push(this.skew);
            this.saved.push(this.entrySkew);
            this.saved.push(this.torus ? 1.0 : 0.0);
            this.saved.push(this.xPeriod);
            this.saved.push(this.zPeriod);
            this.saved.push(this.skewCells);
            this.saved.push(this.compression);
        }

        private void pop() {
            this.compression = this.saved.pop();
            this.skewCells = (int) this.saved.pop();
            this.zPeriod = (int) this.saved.pop();
            this.xPeriod = (int) this.saved.pop();
            this.torus = this.saved.pop() != 0.0;
            this.entrySkew = this.saved.pop();
            this.skew = this.saved.pop();
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

    public static int wrapLatticeX(int x, int z) {
        return active ? FRAME.get().foldX(x, z) : x;
    }

    public static int wrapLatticeZ(int z) {
        return active ? FRAME.get().foldZ(z) : z;
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
