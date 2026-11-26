package com.exoticworlds.shape.torus;

import java.util.List;

import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.dimension.LoopParameters;
import com.exoticworlds.shape.dimension.LoopSizeLines;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.NumberRange;
import com.exoticworlds.shape.option.OptionPart;

public final class TorusParameters extends LoopParameters {
    private static final String SIZE_KEY = "torus_size";
    private static final int SIZE_POSITION = 0;
    private static final String X_WIDTH_KEY = "size_x";
    private static final String Z_WIDTH_KEY = "size_z";
    private static final String SHIFT_KEY = "shift";

    public static final ShapeOption<TorusSize> SIZE = new ShapeOption<>(SIZE_KEY, SIZE_POSITION, TorusSize.CODEC,
            TorusSize.square(WorldLoopSizes.DEFAULT_WIDTH_CHUNKS));

    private static final DimensionOption<TorusSize> SIZE_PARAMETER = DimensionOption.of(SIZE,
            LoopSizeLines.CAPTION_KEY, List.of(
                    new OptionPart.Row<>(List.of(
                            width(X_WIDTH_KEY, TorusSize::xWidthChunks,
                                    (size, widthChunks) -> new TorusSize(widthChunks, size.zWidthChunks(),
                                            size.shiftChunks())),
                            width(Z_WIDTH_KEY, TorusSize::zWidthChunks,
                                    (size, widthChunks) -> new TorusSize(size.xWidthChunks(), widthChunks,
                                            size.shiftChunks())))),
                    new OptionPart.WholeNumber<>(SHIFT_KEY,
                            (stem, size) -> new NumberRange(0, size.xWidthChunks() - 1),
                            TorusSize::shiftChunks,
                            (size, shiftChunks) -> new TorusSize(size.xWidthChunks(), size.zWidthChunks(), shiftChunks),
                            (size, shiftChunks) -> LoopSizeLines.shiftLaps(size.xWidthChunks(), shiftChunks),
                            size -> true)));

    private static final List<DimensionOption<?>> ALL = List.of(SIZE_PARAMETER);

    TorusParameters() {
    }

    @Override
    public List<DimensionOption<?>> getAll() {
        return ALL;
    }

    @Override
    public ShapeOptions defaultOptions(StemTerrain stem) {
        return ShapeOptions.DEFAULT.with(SIZE, TorusSize.square(WorldLoopSizes.getDefaultWidthChunks(stem)));
    }
}
