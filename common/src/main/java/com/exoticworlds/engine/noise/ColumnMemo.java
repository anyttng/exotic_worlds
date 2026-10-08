package com.exoticworlds.engine.noise;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public final class ColumnMemo implements DensityFunction {
    private final DensityFunction input;

    private final boolean readsAtHeightZero;

    private final ThreadLocal<Column> last = ThreadLocal.withInitial(Column::new);

    private ColumnMemo(DensityFunction input, boolean readsAtHeightZero) {
        this.input = input;
        this.readsAtHeightZero = readsAtHeightZero;
    }

    public static DensityFunction overColumnMarkers(DensityFunction density) {
        return density.mapAll(node -> node instanceof DensityFunctions.Marker marker
                && (marker.type() == DensityFunctions.Marker.Type.FlatCache
                        || marker.type() == DensityFunctions.Marker.Type.Cache2D)
                ? new ColumnMemo(marker.wrapped(), marker.type() == DensityFunctions.Marker.Type.FlatCache)
                : node);
    }

    @Override
    public double compute(FunctionContext context) {
        int blockX = context.blockX();
        int blockZ = context.blockZ();
        Column column = this.last.get();
        if (!column.filled || column.blockX != blockX || column.blockZ != blockZ) {
            column.value = this.input.compute(this.readsAtHeightZero
                    ? new SinglePointContext(blockX, 0, blockZ)
                    : context);
            column.blockX = blockX;
            column.blockZ = blockZ;
            column.filled = true;
        }

        return column.value;
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        provider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ColumnMemo(this.input.mapAll(visitor), this.readsAtHeightZero));
    }

    @Override
    public double minValue() {
        return this.input.minValue();
    }

    @Override
    public double maxValue() {
        return this.input.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("Calling .codec() on ColumnMemo");
    }

    private static final class Column {
        private boolean filled;

        private int blockX;

        private int blockZ;

        private double value;
    }
}
