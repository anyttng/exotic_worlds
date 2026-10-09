package com.exoticworlds.compat.wover;

import java.util.List;
import java.util.function.Predicate;

import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import net.minecraft.util.Mth;

public final class LapMapStack<T> {
    public record Layering(int minValue, int maxValue, int maxIndex, int worldHeight, double layerDistortion) {
    }

    private static final double LAYER_NOISE_RATE = 0.03;

    private static final double ROUND = 0.5;

    private final WorldFold fold;

    private final TranslationLattice lattice;

    private final List<LapMap<T>> layers;

    private final Predicate<T> vertical;

    private final Layering layering;

    private final OpenSimplexStandIn noise;

    private final double xPeriod;

    private final double zPeriod;

    private final LapChunkCache<List<LapChunk<T>>> chunks;

    public LapMapStack(WorldFold fold, List<LapMap<T>> layers, Predicate<T> vertical, Layering layering, long seed) {
        this.fold = fold;
        this.layers = List.copyOf(layers);
        this.vertical = vertical;
        this.layering = layering;
        this.noise = new OpenSimplexStandIn(seed);
        this.lattice = fold.blockLattice();
        this.xPeriod = noisePeriod(this.lattice.x());
        this.zPeriod = noisePeriod(this.lattice.z());
        this.chunks = new LapChunkCache<>(() -> this.layers.getFirst().bounded(), this::build);
        for (int index = 0; index < this.layers.size(); index++) {
            int layer = index;
            this.layers.get(index).shareChunks((kx, kz) -> chunk(layer, kx, kz));
        }
    }

    public boolean covers(WorldFold fold) {
        return this.fold == fold;
    }

    public int layer(double x, double y, double z) {
        if (y < this.layering.minValue()) {
            return 0;
        }

        if (y > this.layering.maxValue()) {
            return this.layering.maxIndex();
        }

        double distortion = this.noise.eval(this.lattice.rectangleX(x, z) * LAYER_NOISE_RATE, z * LAYER_NOISE_RATE,
                this.xPeriod, this.zPeriod)
                * this.layering.layerDistortion();
        int layer = Mth.floor((y + distortion) / this.layering.worldHeight() * this.layering.maxIndex() + ROUND);
        return Mth.clamp(layer, 0, this.layering.maxIndex());
    }

    LapChunk<T> chunk(int layer, int kx, int kz) {
        return this.chunks.get(kx, kz).get(layer);
    }

    private List<LapChunk<T>> build(int kx, int kz) {
        List<LapChunk<T>> built = this.layers.stream().map(map -> map.buildChunk(kx, kz)).toList();
        copyVertical(built, this.vertical);
        return built;
    }

    static <T> void copyVertical(List<LapChunk<T>> chunks, Predicate<T> vertical) {
        LapChunk<T> first = chunks.getFirst();
        for (LapChunk<T> chunk : chunks) {
            if (chunk.sideX() != first.sideX() || chunk.sideZ() != first.sideZ()) {
                throw new IllegalStateException("The layers of one map stack laid different chunk grids");
            }
        }

        for (int x = 0; x < first.sideX(); x++) {
            for (int z = 0; z < first.sideZ(); z++) {
                T found = null;
                for (LapChunk<T> chunk : chunks) {
                    T biome = chunk.get(x, z);
                    if (vertical.test(biome)) {
                        found = biome;
                        break;
                    }
                }

                if (found != null) {
                    for (LapChunk<T> chunk : chunks) {
                        chunk.set(x, z, found);
                    }
                }
            }
        }
    }

    private static double noisePeriod(WrapDomain blocks) {
        return blocks.loops() ? blocks.domainLength * LAYER_NOISE_RATE : OpenSimplexStandIn.UNBOUNDED;
    }
}
