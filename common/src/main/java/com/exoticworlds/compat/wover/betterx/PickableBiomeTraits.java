package com.exoticworlds.compat.wover.betterx;

import org.betterx.wover.generator.api.biomesource.WoverBiomePicker;
import org.betterx.wover.generator.api.biomesource.WoverBiomePicker.PickableBiome;
import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.wover.LapPicker;
import com.exoticworlds.compat.wover.PickedBiomeTraits;

public final class PickableBiomeTraits implements PickedBiomeTraits<PickableBiome> {
    public static final PickableBiomeTraits INSTANCE = new PickableBiomeTraits();

    public static LapPicker<PickableBiome> toLapPicker(WoverBiomePicker picker) {
        return new LapPicker<>(picker::getBiome, PickableBiome::getSubBiome);
    }

    @Override
    public @Nullable PickableBiome getEdge(PickableBiome biome) {
        return biome.getEdge();
    }

    @Override
    public int getEdgeSize(PickableBiome biome) {
        return biome.edgeSize;
    }

    @Override
    public @Nullable PickableBiome getParent(PickableBiome biome) {
        return biome.getParentBiome();
    }

    @Override
    public boolean isSame(PickableBiome biome, PickableBiome other) {
        return biome.isSame(other);
    }

    @Override
    public boolean isVertical(PickableBiome biome) {
        return biome.isVertical;
    }

    private PickableBiomeTraits() {
    }
}
