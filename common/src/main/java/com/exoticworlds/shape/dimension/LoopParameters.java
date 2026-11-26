package com.exoticworlds.shape.dimension;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.option.NumberRange;
import com.exoticworlds.shape.option.OptionPart;

public abstract class LoopParameters implements ShapeParameters {

    protected static <T> OptionPart<T> width(String key, Function<T, Integer> read, BiFunction<T, Integer, T> write) {
        return new OptionPart.WholeNumber<>(key,
                (stem, value) -> new NumberRange(WorldLoopSizes.getMinWidthChunks(stem),
                        WorldLoopSizes.MAX_WIDTH_CHUNKS),
                read, write, (value, widthChunks) -> LoopSizeLines.widthLine(widthChunks), value -> true);
    }
}
