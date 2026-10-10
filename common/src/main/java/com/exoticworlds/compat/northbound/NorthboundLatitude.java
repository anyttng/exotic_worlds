package com.exoticworlds.compat.northbound;

import com.exoticworlds.compat.LevelClimateCompression;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.noise.DensityRewrites;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

public final class NorthboundLatitude {
    private static final String NORTHBOUND_NAMESPACE = "northbound";

    private static final ResourceKey<DensityFunction> COORD_Z = densityKey("uni", "coord/z");

    static final double POLE_Z = -4096.0;

    static final double EQUATOR_Z = 4096.0;

    enum Variant {
        DEFAULT("latitude", 1.0),
        LARGE_BIOMES("large_biomes/latitude", 4.0);

        private final String path;

        private final double stretch;

        Variant(String path, double stretch) {
            this.path = path;
            this.stretch = stretch;
        }

        double poleToEquatorBlocks() {
            return (EQUATOR_Z - POLE_Z) * this.stretch;
        }
    }

    public static void register() {
        for (Variant variant : Variant.values()) {
            DensityRewrites.register(densityKey(NORTHBOUND_NAMESPACE, variant.path),
                    (function, fold, router) -> laidOut(function, fold, router, variant));
        }
    }

    private static DensityFunction laidOut(DensityFunction latitude, WorldFold fold, NoiseRouter router,
            Variant variant) {
        TranslationLattice lattice = fold.blockLattice();
        if (!lattice.bothLoop()) {
            return latitude;
        }

        double factor = LevelClimateCompression.factor(fold, router.temperature());
        return DensityRewrites.replaceKeyed(latitude, COORD_Z, coordinate(lattice.z(), variant, factor));
    }

    static LatitudeCoordinate coordinate(WrapDomain z, Variant variant, double factor) {
        return new LatitudeCoordinate(z, bands(z.domainLength, variant.poleToEquatorBlocks() / factor),
                POLE_Z * variant.stretch, EQUATOR_Z * variant.stretch);
    }

    static int bands(int lap, double poleToEquatorCap) {
        int bands = Math.max(1, (int) Math.ceil(lap / (2.0 * poleToEquatorCap)));
        return bands % 2 == 0 ? bands + 1 : bands;
    }

    private static ResourceKey<DensityFunction> densityKey(String namespace, String path) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private NorthboundLatitude() {
    }
}
