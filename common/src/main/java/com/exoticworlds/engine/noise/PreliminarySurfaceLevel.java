package com.exoticworlds.engine.noise;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseSettings;

// On 1.21.1 the router carries the pre-jaggedness density where 26.x carries a find_top_surface node, so the column
// search NoiseChunk does there is done here instead.
public record PreliminarySurfaceLevel(DensityFunction density, NoiseSettings bounds) implements DensityFunction {
    private static final double SOLID_THRESHOLD = 0.390625;

    public PreliminarySurfaceLevel {
        density = ColumnMemo.overColumnMarkers(density);
    }

    @Override
    public double compute(FunctionContext context) {
        int blockX = context.blockX();
        int blockZ = context.blockZ();
        int floor = this.bounds.minY();
        int step = this.bounds.getCellHeight();

        for (int y = floor + this.bounds.height(); y >= floor; y -= step) {
            if (this.density.compute(new SinglePointContext(blockX, y, blockZ)) > SOLID_THRESHOLD) {
                return y;
            }
        }

        return floor;
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        provider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PreliminarySurfaceLevel(this.density.mapAll(visitor), this.bounds));
    }

    @Override
    public double minValue() {
        return this.bounds.minY();
    }

    @Override
    public double maxValue() {
        return this.bounds.minY() + this.bounds.height();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("Calling .codec() on PreliminarySurfaceLevel");
    }
}
