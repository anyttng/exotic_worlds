package com.exoticworlds.shape.cylinder;

import java.util.List;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.dimension.LoopParameters;
import com.exoticworlds.shape.dimension.LoopSizeLines;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.OptionPart;

import net.minecraft.core.Direction;

public final class CylinderParameters extends LoopParameters {
    private static final String SIZE_KEY = "cylinder_size";
    private static final int SIZE_POSITION = 0;
    private static final String AXIS_KEY = "axis";
    private static final String WIDTH_KEY = "size";

    public static final ShapeOption<CylinderSize> SIZE = new ShapeOption<>(SIZE_KEY, SIZE_POSITION,
            CylinderSize.CODEC, new CylinderSize(Direction.Axis.X, WorldLoopSizes.DEFAULT_WIDTH_CHUNKS));

    private static final DimensionOption<CylinderSize> SIZE_PARAMETER = DimensionOption.of(SIZE,
            LoopSizeLines.CAPTION_KEY, List.of(
                    new OptionPart.Choice<>(AXIS_KEY, List.of(Direction.Axis.X, Direction.Axis.Z),
                            Direction.Axis::getName, LoopSizeLines::axisName, CylinderSize::axis,
                            (size, axis) -> new CylinderSize(axis, size.widthChunks()), size -> true),
                    width(WIDTH_KEY, CylinderSize::widthChunks,
                            (size, widthChunks) -> new CylinderSize(size.axis(), widthChunks))));

    private static final List<DimensionOption<?>> ALL = List.of(SIZE_PARAMETER);

    CylinderParameters() {
    }

    @Override
    public List<DimensionOption<?>> getAll() {
        return ALL;
    }

    @Override
    public ShapeOptions defaultOptions(StemTerrain stem) {
        return ShapeOptions.DEFAULT.with(SIZE,
                new CylinderSize(Direction.Axis.X, WorldLoopSizes.getDefaultWidthChunks(stem)));
    }
}
