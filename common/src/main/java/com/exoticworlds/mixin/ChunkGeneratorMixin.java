package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.ShapeStamp;
import com.exoticworlds.core.CarriedShape;

import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements ShapeStamp {
    @Unique
    private volatile @Nullable CarriedShape exotic_worlds$carriedShape;

    @Override
    public @Nullable CarriedShape exotic_worlds$carriedShape() {
        return this.exotic_worlds$carriedShape;
    }

    @Override
    public void exotic_worlds$stamp(CarriedShape carried) {
        this.exotic_worlds$carriedShape = carried;
    }

    @Override
    public void exotic_worlds$clearStamp() {
        this.exotic_worlds$carriedShape = null;
    }
}
