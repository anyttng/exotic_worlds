package com.exoticworlds.compat.lithostitched;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.LapFrame;
import com.exoticworlds.compat.lithostitched.mixin.FastNoiseConfigAccessor;
import com.exoticworlds.compat.lithostitched.mixin.FastNoiseLiteAccessor;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WrapDomain;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;
import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.CellularNoiseType;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.PerlinNoiseType;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.SimplexNoiseType;

public final class LapFastNoise {
    private static final double NO_XZ_SCALE = 0.0;

    private static final float UNIT_FREQUENCY = 1.0F;

    private static final int SINGLE_OCTAVE = 1;

    private static final Set<Class<? extends FastNoiseConfig>> KNOWN_CONFIGS = Set.of(PerlinNoiseType.class,
            SimplexNoiseType.class, CellularNoiseType.class);

    private static final Set<FNL.NoiseType> FOLDED_TYPES = Set.of(FNL.NoiseType.Perlin,
            FNL.NoiseType.OpenSimplex2S, FNL.NoiseType.Cellular);

    private static final Set<FNL.FractalType> SUMMED_FRACTALS = Set.of(FNL.FractalType.FBm,
            FNL.FractalType.Ridged, FNL.FractalType.PingPong);

    private final FNL noise;

    private final FastNoiseLap lap;

    private final TranslationLattice lattice;

    private final double xzScale;

    private final double shear;

    private LapFastNoise(FNL noise, FastNoiseLap lap, TranslationLattice lattice, double xzScale) {
        this.noise = noise;
        this.lap = lap;
        this.lattice = lattice;
        this.xzScale = xzScale;
        this.shear = new LapFrame(lattice).shear();
    }

    public static @Nullable LapFastNoise of(FastNoiseConfig config, WorldFold fold, double xzScale) {
        if (!KNOWN_CONFIGS.contains(config.getClass()) || xzScale == NO_XZ_SCALE) {
            return null;
        }

        FastNoiseLiteAccessor source = (FastNoiseLiteAccessor) ((FastNoiseConfigAccessor) config)
                .exotic_worlds$fastNoiseLite();
        if (!FOLDED_TYPES.contains(source.exotic_worlds$noiseType())
                || source.exotic_worlds$rotationType3d() != FNL.RotationType3D.None) {
            return null;
        }

        TranslationLattice lattice = fold.blockLattice();
        FastNoiseLap lap = FastNoiseLap.of(
                new FastNoiseLap.Laps(lapOf(lattice.x()), lapOf(lattice.z()), xzScale),
                new FastNoiseLap.Field(source.exotic_worlds$seed(), source.exotic_worlds$frequency(),
                        source.exotic_worlds$lacunarity(), octavesOf(source),
                        source.exotic_worlds$noiseType() == FNL.NoiseType.OpenSimplex2S),
                new FastNoiseLap.Primes(FastNoiseLiteAccessor.exotic_worlds$primeX(),
                        FastNoiseLiteAccessor.exotic_worlds$primeY(), FastNoiseLiteAccessor.exotic_worlds$primeZ()));
        return new LapFastNoise(closedCopy(source, lap), lap, lattice, xzScale);
    }

    public float get(double x, double y, double z) {
        double blockZ = z / this.xzScale;
        double foldedZ = this.lattice.foldZ(blockZ);
        double frameX = this.lattice.foldX(x / this.xzScale, blockZ) - foldedZ * this.shear;
        return this.noise.GetNoise(frameX * this.lap.blockScale(), y * this.lap.inputScale(),
                foldedZ * this.lap.blockScale());
    }

    private static FNL closedCopy(FastNoiseLiteAccessor source, FastNoiseLap lap) {
        FNL copy = new FNL();
        copy.SetSeed(source.exotic_worlds$seed());
        copy.SetFrequency(UNIT_FREQUENCY);
        copy.SetNoiseType(source.exotic_worlds$noiseType());
        copy.SetFractalType(source.exotic_worlds$fractalType());
        copy.SetFractalOctaves(source.exotic_worlds$octaves());
        copy.SetFractalLacunarity(source.exotic_worlds$lacunarity());
        copy.SetFractalGain(source.exotic_worlds$gain());
        copy.SetFractalWeightedStrength(source.exotic_worlds$weightedStrength());
        copy.SetFractalPingPongStrength(source.exotic_worlds$pingPongStrength());
        copy.SetCellularDistanceFunction(source.exotic_worlds$cellularDistanceFunction());
        copy.SetCellularReturnType(source.exotic_worlds$cellularReturnType());
        copy.SetCellularJitter(source.exotic_worlds$cellularJitterModifier());
        ((FastNoiseLapHolder) copy).exotic_worlds$setLap(lap);
        return copy;
    }

    private static int octavesOf(FastNoiseLiteAccessor source) {
        return SUMMED_FRACTALS.contains(source.exotic_worlds$fractalType()) ? source.exotic_worlds$octaves()
                : SINGLE_OCTAVE;
    }

    private static int lapOf(WrapDomain domain) {
        return domain.loops() ? domain.domainLength : FastNoiseLap.OPEN;
    }
}
