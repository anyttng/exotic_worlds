package com.exoticworlds.engine.noise.check;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.cuboid.CuboidAtlas;
import com.exoticworlds.core.fold.cuboid.CuboidEdge;
import com.exoticworlds.core.fold.WorldFold;

import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

sealed interface SeamPassage permits SeamCauseSearch.Lap, EdgePassage {
    static List<SeamPassage> of(WorldFold fold) {
        CuboidAtlas atlas = fold.getCuboidAtlas();
        return atlas != null
                ? List.copyOf(EdgePassage.of(fold, atlas))
                : List.copyOf(SeamCauseSearch.Lap.of(fold.getBlockLattice()));
    }

    List<SeamCauseSearch.Point> samples();

    SeamCauseSearch.Point away(SeamCauseSearch.Point point);

    <T> T reading(SeamCauseSearch.Point at, Supplier<T> read);

    @Nullable CuboidEdge edge();

    default boolean isDifferentAt(DensitySampler sampler, SeamCauseSearch.Point point) {
        SeamCauseSearch.Point away = away(point);
        float here = reading(point,
                () -> sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, point.x(), point.y(), point.z()));
        float there = reading(away,
                () -> sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, away.x(), away.y(), away.z()));
        return Float.floatToIntBits(here) != Float.floatToIntBits(there);
    }
}
