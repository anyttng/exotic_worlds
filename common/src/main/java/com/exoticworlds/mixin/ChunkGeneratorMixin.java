package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.CarriedShape;
import com.exoticworlds.core.DimensionTable;

import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements ShapeStamp {
    @Unique
    private volatile @Nullable CarriedShape exotic_worlds$carriedShape;

    @Unique
    private volatile @Nullable CarriedShape exotic_worlds$bakedShape;

    @Unique
    private volatile DimensionTable exotic_worlds$dimensionTable = DimensionTable.EMPTY;

    @Override
    public @Nullable CarriedShape exotic_worlds$carriedShape() {
        return this.exotic_worlds$carriedShape;
    }

    @Override
    public void exotic_worlds$stamp(CarriedShape carried) {
        this.exotic_worlds$carriedShape = carried;
    }

    @Override
    public @Nullable CarriedShape exotic_worlds$bakedShape() {
        return this.exotic_worlds$bakedShape;
    }

    @Override
    public void exotic_worlds$stampBaked(@Nullable CarriedShape carried) {
        this.exotic_worlds$bakedShape = carried;
    }

    @Override
    public DimensionTable exotic_worlds$dimensionTable() {
        return this.exotic_worlds$dimensionTable;
    }

    @Override
    public void exotic_worlds$stampDimensionTable(DimensionTable table) {
        this.exotic_worlds$dimensionTable = table;
    }

    @Override
    public void exotic_worlds$clearStamp() {
        this.exotic_worlds$carriedShape = null;
        this.exotic_worlds$bakedShape = null;
        this.exotic_worlds$dimensionTable = DimensionTable.EMPTY;
    }
}
