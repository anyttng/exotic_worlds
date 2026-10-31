package com.exoticworlds.shape.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.option.WorldOption;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.climate.CompactBiomes;
import com.exoticworlds.shape.torus.GuaranteedLand;
import com.exoticworlds.shape.torus.GuaranteedNetherComplexes;
import com.exoticworlds.shape.torus.TorusSettings;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

public record CustomSettings(DimensionShape overworld, DimensionShape nether, DimensionShape end,
        Map<ResourceKey<LevelStem>, DimensionShape> modded) {
    public static final GenerationOptions DEFAULT_OPTIONS = TorusSettings.DEFAULT.generationOptions();

    public static final CustomSettings DEFAULT = new CustomSettings(
            defaultTorus(VanillaDimension.OVERWORLD),
            defaultTorus(VanillaDimension.NETHER),
            defaultTorus(VanillaDimension.END));

    public CustomSettings {
        modded = Map.copyOf(modded);
    }

    public CustomSettings(DimensionShape overworld, DimensionShape nether, DimensionShape end) {
        this(overworld, nether, end, Map.of());
    }

    public DimensionShape get(VanillaDimension dimension) {
        return switch (dimension) {
            case OVERWORLD -> this.overworld;
            case NETHER -> this.nether;
            case END -> this.end;
        };
    }

    public CustomSettings with(VanillaDimension dimension, DimensionShape shape) {
        return switch (dimension) {
            case OVERWORLD -> new CustomSettings(shape, this.nether, this.end, this.modded);
            case NETHER -> new CustomSettings(this.overworld, shape, this.end, this.modded);
            case END -> new CustomSettings(this.overworld, this.nether, shape, this.modded);
        };
    }

    public @Nullable DimensionShape get(ModDimension dimension) {
        return this.modded.get(dimension.stem());
    }

    public CustomSettings with(ModDimension dimension, @Nullable DimensionShape shape) {
        Map<ResourceKey<LevelStem>, DimensionShape> rows = new HashMap<>(this.modded);
        if (shape == null) {
            rows.remove(dimension.stem());
        } else {
            rows.put(dimension.stem(), shape);
        }

        return new CustomSettings(this.overworld, this.nether, this.end, rows);
    }

    public static List<WorldOption<?>> offeredOptions(CustomStem dimension, StemKind kind) {
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
