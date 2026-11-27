package com.exoticworlds.shape.cuboid;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.exoticworlds.core.option.ShapeOption;
import com.exoticworlds.core.option.ShapeOptions;
import com.exoticworlds.core.world.StemTerrain;
import com.exoticworlds.core.world.WorldLoopSizes;
import com.exoticworlds.shape.dimension.LoopSizeLines;
import com.exoticworlds.shape.dimension.ShapeParameters;
import com.exoticworlds.shape.option.DimensionOption;
import com.exoticworlds.shape.option.NumberRange;
import com.exoticworlds.shape.option.OptionPart;

public final class CuboidParameters implements ShapeParameters {
    private static final String SIZE_KEY = "cuboid_size";
    private static final int SIZE_POSITION = 0;
    private static final String A_EDGE_KEY = "size_a";
    private static final String B_EDGE_KEY = "size_b";
    private static final String C_EDGE_KEY = "size_c";
    private static final NumberRange EDGE_RANGE =
            new NumberRange(WorldLoopSizes.MIN_WIDTH_CHUNKS, WorldLoopSizes.MAX_CUBOID_EDGE_CHUNKS);

    public static final ShapeOption<CuboidSize> SIZE = new ShapeOption<>(SIZE_KEY, SIZE_POSITION, CuboidSize.CODEC,
            CuboidSize.cube(WorldLoopSizes.DEFAULT_WIDTH_CHUNKS));

    private static final DimensionOption<CuboidSize> SIZE_PARAMETER = DimensionOption.of(SIZE,
            LoopSizeLines.CAPTION_KEY, List.of(
                    new OptionPart.Row<>(List.of(
                            edge(A_EDGE_KEY, CuboidSize::aChunks,
                                    (size, edgeChunks) -> new CuboidSize(edgeChunks, size.bChunks(), size.cChunks())),
                            edge(B_EDGE_KEY, CuboidSize::bChunks,
                                    (size, edgeChunks) -> new CuboidSize(size.aChunks(), edgeChunks, size.cChunks())),
                            edge(C_EDGE_KEY, CuboidSize::cChunks,
                                    (size, edgeChunks) -> new CuboidSize(size.aChunks(), size.bChunks(), edgeChunks))))));

    private static final List<DimensionOption<?>> ALL = List.of(SIZE_PARAMETER);

    CuboidParameters() {
    }

    private static OptionPart<CuboidSize> edge(String key, Function<CuboidSize, Integer> read,
            BiFunction<CuboidSize, Integer, CuboidSize> write) {
        return new OptionPart.WholeNumber<>(key, (stem, size) -> EDGE_RANGE, read, write,
                (size, edgeChunks) -> LoopSizeLines.widthLine(edgeChunks), size -> true);
    }

    @Override
    public List<DimensionOption<?>> getAll() {
        return ALL;
    }

    @Override
    public ShapeOptions defaultOptions(StemTerrain stem) {
        return ShapeOptions.DEFAULT.with(SIZE, CuboidSize.cube(WorldLoopSizes.DEFAULT_WIDTH_CHUNKS));
    }
}
