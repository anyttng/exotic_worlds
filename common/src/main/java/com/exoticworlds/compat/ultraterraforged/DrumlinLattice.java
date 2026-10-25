package com.exoticworlds.compat.ultraterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.terraforged.LapWave;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.Landform;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;

import net.minecraft.core.Direction;

public record DrumlinLattice(float alongX, float alongZ, float cellLength, float rowShift, float rowWidth,
        int rowPeriod, int rowStep, int columns) {
    public static final float CELL_LENGTH = 90.0F;

    public static final float CELL_WIDTH = 40.0F;

    public static final float ROW_SHIFT = 45.0F;

    public static final int UNFOLDED = 0;

    private static final float DRUMLIN_CHANCE = 0.75F;

    private static final float JITTER = 0.3F;

    private static final float CENTER = 0.5F;

    private static final float LENGTH_MIN = 0.3F;

    private static final float LENGTH_MAX = 0.5F;

    private static final float WIDTH_MIN = 0.25F;

    private static final float WIDTH_MAX = 0.4F;

    private static final float UPSTREAM = 0.65F;

    private static final float SIZE_MIN = 0.55F;

    private static final int PRESENCE = 10;

    private static final int OFFSET_ALONG = 11;

    private static final int OFFSET_ACROSS = 12;

    private static final int LENGTH = 13;

    private static final int WIDTH = 14;

    private static final int SIZE = 15;

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

    public float drumlin(int seed, float x, float z) {
        float u = x * this.alongX + z * this.alongZ;
        float v = z * this.alongX - x * this.alongZ;
        int row = NoiseUtil.floor(v / this.rowWidth);
        float best = 0.0F;
        for (int r = row - 1; r <= row + 1; r++) {
            float shift = r * this.rowShift;
            int column = NoiseUtil.floor((u - shift) / this.cellLength);
            for (int c = column - 1; c <= column + 1; c++) {
                int cellColumn = foldColumn(c, r);
                int cellRow = foldRow(r);
                if (Landform.random(seed, cellColumn, cellRow, PRESENCE) >= DRUMLIN_CHANCE) {
                    continue;
                }

                float offsetAlong = Landform.random(seed, cellColumn, cellRow, OFFSET_ALONG) - CENTER;
                float offsetAcross = Landform.random(seed, cellColumn, cellRow, OFFSET_ACROSS) - CENTER;
                float centerU = shift + (c + CENTER) * this.cellLength + offsetAlong * this.cellLength * JITTER;
                float centerV = (r + CENTER) * this.rowWidth + offsetAcross * this.rowWidth * JITTER;
                float lengthShare = Landform.random(seed, cellColumn, cellRow, LENGTH);
                float widthShare = Landform.random(seed, cellColumn, cellRow, WIDTH);
                float length = NoiseUtil.lerp(LENGTH_MIN, LENGTH_MAX, lengthShare) * this.cellLength;
                float width = NoiseUtil.lerp(WIDTH_MIN, WIDTH_MAX, widthShare) * this.rowWidth;
                float du = u - centerU;
                float alongFlow = du / (du < 0.0F ? length * UPSTREAM : length);
                float acrossFlow = (v - centerV) / width;
                float distance = alongFlow * alongFlow + acrossFlow * acrossFlow;
                if (distance < 1.0F) {
                    float size = NoiseUtil.lerp(SIZE_MIN, 1.0F, Landform.random(seed, cellColumn, cellRow, SIZE));
                    float t = 1.0F - distance;
                    best = Math.max(best, size * t * t * (3.0F - 2.0F * t));
                }
            }
        }

        return best;
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
