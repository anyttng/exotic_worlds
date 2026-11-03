package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.terraforged.LapWave;

import net.minecraft.core.Direction;

public record DrumlinLattice(float alongX, float alongZ, float cellLength, float rowShift, float rowWidth,
        int rowPeriod, int rowStep, int columns) {
    public static final float CELL_LENGTH = 90.0F;

    public static final float CELL_WIDTH = 40.0F;

    public static final float ROW_SHIFT = 45.0F;

    public static final int UNFOLDED = 0;

    private static final ThreadLocal<Remembered> REMEMBERED = ThreadLocal.withInitial(Remembered::new);

    public static @Nullable DrumlinLattice bound(float cosFlow, float sinFlow) {
        LapWave lap = LapWave.bound();
        return lap == null ? null : REMEMBERED.get().over(lap, cosFlow, sinFlow);
    }

    public static DrumlinLattice over(LapWave lap, float cosFlow, float sinFlow) {
        LapWave.Wave across = lap.noLonger(-sinFlow / CELL_WIDTH, cosFlow / CELL_WIDTH);
        double alongX = cosFlow / CELL_LENGTH + sinFlow * ROW_SHIFT / (CELL_LENGTH * CELL_WIDTH);
        double alongZ = sinFlow / CELL_LENGTH - cosFlow * ROW_SHIFT / (CELL_LENGTH * CELL_WIDTH);
        LapWave.Wave along = lap.nearest(alongX, alongZ,
                wave -> det(wave, across) > 0.0 && across.frequency() / det(wave, across) <= CELL_LENGTH);
        double det = det(along, across);
        double cellX = across.z() / det;
        double cellZ = -across.x() / det;
        double length = Math.hypot(cellX, cellZ);
        double unitX = cellX / length;
        double unitZ = cellZ / length;
        double rowX = -along.z() / det;
        double rowZ = along.x() / det;
        DrumlinLattice lattice = new DrumlinLattice((float) unitX, (float) unitZ, (float) length,
                (float) (rowX * unitX + rowZ * unitZ), (float) (1.0 / across.frequency()), UNFOLDED, UNFOLDED,
                UNFOLDED);
        return lattice.folded(lap, along, across);
    }

    public float along(float x, float z) {
        return x * this.alongX + z * this.alongZ;
    }

    public float across(float x, float z) {
        return z * this.alongX - x * this.alongZ;
    }

    public int foldColumn(int column, int row) {
        long folded = column;
        if (this.rowPeriod != UNFOLDED) {
            folded -= (long) Math.floorDiv(row, this.rowPeriod) * this.rowStep;
        }

        return (int) (this.columns == UNFOLDED ? folded : Math.floorMod(folded, this.columns));
    }

    public int foldRow(int row) {
        return this.rowPeriod == UNFOLDED ? row : Math.floorMod(row, this.rowPeriod);
    }

    private DrumlinLattice folded(LapWave lap, LapWave.Wave along, LapWave.Wave across) {
        boolean xLoops = lap.loops(Direction.Axis.X);
        boolean zLoops = lap.loops(Direction.Axis.Z);
        if (xLoops && zLoops) {
            long[] bezout = bezout(across.xTurns(), across.zTurns());
            long period = bezout[0];
            long step = bezout[1] * along.xTurns() + bezout[2] * along.zTurns();
            long columns = Math.abs(along.xTurns() * across.zTurns() - along.zTurns() * across.xTurns()) / period;
            return withFold(period, Math.floorMod(step, columns), columns);
        }

        long columnTurns = xLoops ? along.xTurns() : along.zTurns();
        long rowTurns = xLoops ? across.xTurns() : across.zTurns();
        if (rowTurns == LapWave.NO_TURNS) {
            return withFold(UNFOLDED, UNFOLDED, Math.abs(columnTurns));
        }

        long sign = Long.signum(rowTurns);
        return withFold(sign * rowTurns, sign * columnTurns, UNFOLDED);
    }

    private DrumlinLattice withFold(long period, long step, long columns) {
        return new DrumlinLattice(this.alongX, this.alongZ, this.cellLength, this.rowShift, this.rowWidth,
                Math.toIntExact(period), Math.toIntExact(step), Math.toIntExact(columns));
    }

    private static double det(LapWave.Wave along, LapWave.Wave across) {
        return along.x() * across.z() - along.z() * across.x();
    }

    private static long[] bezout(long first, long second) {
        long oldRemainder = first;
        long remainder = second;
        long oldA = 1L;
        long a = 0L;
        long oldB = 0L;
        long b = 1L;
        while (remainder != 0L) {
            long quotient = Math.floorDiv(oldRemainder, remainder);
            long next = oldRemainder - quotient * remainder;
            oldRemainder = remainder;
            remainder = next;
            next = oldA - quotient * a;
            oldA = a;
            a = next;
            next = oldB - quotient * b;
            oldB = b;
            b = next;
        }

        long sign = Long.signum(oldRemainder);
        return new long[] {sign * oldRemainder, sign * oldA, sign * oldB};
    }

    private static final class Remembered {
        private @Nullable LapWave lap;
        private float cosFlow;
        private float sinFlow;
        private @Nullable DrumlinLattice lattice;

        DrumlinLattice over(LapWave lap, float cosFlow, float sinFlow) {
            if (this.lattice == null || cosFlow != this.cosFlow || sinFlow != this.sinFlow || !lap.equals(this.lap)) {
                this.lap = lap;
                this.cosFlow = cosFlow;
                this.sinFlow = sinFlow;
                this.lattice = DrumlinLattice.over(lap, cosFlow, sinFlow);
            }

            return this.lattice;
        }
    }
}
