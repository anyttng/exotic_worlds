package com.exoticworlds.compat.naturaltemperature;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.MapMaker;
import com.exoticworlds.compat.FoldCompression;
import com.exoticworlds.compat.LapFrame;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.GenerationFoldContext;

import naturaltemperature.ModConfig;
import naturaltemperature.NaturalTemperature;

public final class ClimateLap {
    private static final int CIRCULAR = 3;

    private static final int FIXED_CIRCULAR = 4;

    private static final int SQUARE = 9;

    private static final int FIXED_SQUARE = 10;

    private static final double POLE_TO_EQUATOR_SPANS = 2.0;

    private static final Map<WorldFold, ClimateLap> BY_FOLD = new MapMaker().weakKeys().makeMap();

    private final TranslationLattice lattice;

    private final LapFrame frame;

    private final double compression;

    public ClimateLap(TranslationLattice lattice, double compression) {
        this.lattice = lattice;
        this.frame = new LapFrame(lattice);
        this.compression = compression;
    }

    public static @Nullable ClimateLap bound() {
        WorldFold fold = GenerationFoldContext.context().wrappedFold();
        return fold == null ? null
                : BY_FOLD.computeIfAbsent(fold, key -> new ClimateLap(key.blockLattice(), FoldCompression.of(key)));
    }

    public int x(int blockX, int blockZ) {
        WrapDomain x = this.lattice.x();
        if (!x.loops()) {
            return compressed(blockX);
        }

        WrapDomain z = this.lattice.z();
        double fromZCenter = z.loops() ? blockZ - center(z) : blockZ;
        return (int) Math.round(mirrored(this.frame.frameX(blockX, fromZCenter), x) * this.compression);
    }

    public int z(int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return z(blockZ, config.generation_mode, config.equatorial_distance, config.equator_offset);
    }

    public int z(int blockZ, int mode, double equatorialDistance, double equatorOffset) {
        WrapDomain z = this.lattice.z();
        if (!z.loops()) {
            return compressed(blockZ);
        }

        if (radial(mode)) {
            return (int) Math.round(mirrored(blockZ, z) * this.compression);
        }

        if (equatorialDistance <= 0.0) {
            return blockZ;
        }

        double turns = (blockZ - (double) z.lowerBound) / z.domainLength * bands(equatorialDistance);
        double towardEquator = 1.0 - Math.abs(2.0 * (turns - Math.floor(turns)) - 1.0);
        return (int) Math.round(equatorialDistance * (2.0 * towardEquator - 1.0) - equatorOffset);
    }

    public int bands(double equatorialDistance) {
        double halfBand = POLE_TO_EQUATOR_SPANS * equatorialDistance / this.compression;
        int bands = Math.max(1, (int) Math.ceil(this.lattice.z().domainLength / (2.0 * halfBand)));
        return bands % 2 == 0 ? bands + 1 : bands;
    }

    private int compressed(int block) {
        return (int) Math.round(block * this.compression);
    }

    static boolean radial(int mode) {
        return mode == CIRCULAR || mode == FIXED_CIRCULAR || mode == SQUARE || mode == FIXED_SQUARE;
    }

    static double mirrored(double coord, WrapDomain domain) {
        double half = domain.domainLength / 2.0;
        double fromCenter = coord - center(domain);
        return Math.abs(fromCenter - Math.floor((fromCenter + half) / domain.domainLength) * domain.domainLength);
    }

    private static double center(WrapDomain domain) {
        return domain.lowerBound + domain.domainLength / 2.0;
    }
}
