package com.exoticworlds.shape.custom;

import java.util.ArrayList;
import java.util.List;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;
import com.exoticworlds.shape.torus.TorusSettings;

public record CustomSettings(DimensionShape overworld, DimensionShape nether, DimensionShape end) {
    public static final GenerationOptions DEFAULT_OPTIONS = TorusSettings.DEFAULT.generationOptions();

    public static final CustomSettings DEFAULT = new CustomSettings(
            defaultTorus(VanillaDimension.OVERWORLD),
            defaultTorus(VanillaDimension.NETHER),
            defaultTorus(VanillaDimension.END));

    public DimensionShape get(VanillaDimension dimension) {
        return switch (dimension) {
            case OVERWORLD -> this.overworld;
            case NETHER -> this.nether;
            case END -> this.end;
        };
    }

    public CustomSettings with(VanillaDimension dimension, DimensionShape shape) {
        return switch (dimension) {
            case OVERWORLD -> new CustomSettings(shape, this.nether, this.end);
            case NETHER -> new CustomSettings(this.overworld, shape, this.end);
            case END -> new CustomSettings(this.overworld, this.nether, shape);
        };
    }

    public static List<WorldOption<?>> offeredOptions(VanillaDimension dimension, StemKind kind) {
        List<WorldOption<?>> offered = new ArrayList<>();
        if (kind == StemKind.NONE) {
            return offered;
        }

        offered.add(CompactBiomes.OPTION);
        if (kind == StemKind.TORUS && dimension == VanillaDimension.OVERWORLD) {
            offered.add(GuaranteedLand.OPTION);
        }

        if (kind == StemKind.TORUS && dimension == VanillaDimension.NETHER) {
            offered.add(GuaranteedNetherComplexes.OPTION);
        }

        return offered;
    }

    private static DimensionShape defaultTorus(VanillaDimension dimension) {
        return new DimensionShape(LoopSpans.ofWidth(dimension.defaultChunkWidth()), DEFAULT_OPTIONS);
    }
}
