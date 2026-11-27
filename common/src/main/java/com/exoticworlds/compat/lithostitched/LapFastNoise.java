package com.exoticworlds.compat.lithostitched;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.worldgen.LapFrame;
import com.exoticworlds.compat.lithostitched.mixin.FastNoiseConfigAccessor;
import com.exoticworlds.compat.lithostitched.mixin.FastNoiseLiteAccessor;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WrapDomain;

import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FNL;
import dev.worldgen.lithostitched.api.worldgen.densityfunction.fastnoise.FastNoiseConfig;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.CellularNoiseType;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.PerlinNoiseType;
import dev.worldgen.lithostitched.impl.worldgen.fastnoise.SimplexNoiseType;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.synth.Noise;

public final class LapFastNoise implements Noise {
    private static final double NO_XZ_SCALE = 0.0;

    private static final float UNIT_FREQUENCY = 1.0F;

    private static final int SINGLE_OCTAVE = 1;

    private static final Set<Class<? extends FastNoiseConfig>> KNOWN_CONFIGS = Set.of(PerlinNoiseType.class,
            SimplexNoiseType.class, CellularNoiseType.class);

    private static final Set<FNL.NoiseType> FOLDED_TYPES = Set.of(FNL.NoiseType.Perlin,
            FNL.NoiseType.OpenSimplex2S, FNL.NoiseType.Cellular);

    private static final Set<FNL.FractalType> SUMMED_FRACTALS = Set.of(FNL.FractalType.FBm,
            FNL.FractalType.Ridged, FNL.FractalType.PingPong);

    private final FastNoiseConfig config;

    private final FNL noise;

    private final FastNoiseLap lap;

    private final TranslationLattice lattice;

    private final double xzScale;

    private final double shear;

    private LapFastNoise(FastNoiseConfig config, FNL noise, FastNoiseLap lap, TranslationLattice lattice,
            double xzScale) {
        this.config = config;
        this.noise = noise;
        this.lap = lap;
        this.lattice = lattice;
        this.xzScale = xzScale;
        this.shear = new LapFrame(lattice).getShear();
    }

    public static boolean isSamplingXz(double xzScale) {
        return xzScale != NO_XZ_SCALE;
    }

    public static @Nullable Refusal refusal(FastNoiseConfig config) {
        FastNoiseLiteAccessor source = getSource(config);
        return refusal(config.getClass(), source.exotic_worlds$getNoiseType(), source.exotic_worlds$getRotationType3d());
    }

    static @Nullable Refusal refusal(Class<?> configClass, FNL.NoiseType noiseType, FNL.RotationType3D rotation) {
        if (!KNOWN_CONFIGS.contains(configClass)) {
            return new Refusal(Reason.UNKNOWN_CONFIG, configClass.getName());
        }

        if (!FOLDED_TYPES.contains(noiseType)) {
            return new Refusal(Reason.NOISE_TYPE, noiseType.name());
        }

        return rotation == FNL.RotationType3D.None ? null : new Refusal(Reason.ROTATION, rotation.name());
    }

    public static LapFastNoise of(FastNoiseConfig config, WorldFold fold, double xzScale) {
        FastNoiseLiteAccessor source = getSource(config);
        TranslationLattice lattice = fold.getBlockLattice();
        FastNoiseLap lap = FastNoiseLap.of(
                new FastNoiseLap.Laps(lapOf(lattice.getX()), lapOf(lattice.getZ()), xzScale),
                new FastNoiseLap.Field(source.exotic_worlds$getSeed(), source.exotic_worlds$getFrequency(),
                        source.exotic_worlds$getLacunarity(), octavesOf(source),
                        source.exotic_worlds$getNoiseType() == FNL.NoiseType.OpenSimplex2S),
                new FastNoiseLap.Primes(FastNoiseLiteAccessor.exotic_worlds$getPrimeX(),
                        FastNoiseLiteAccessor.exotic_worlds$getPrimeY(), FastNoiseLiteAccessor.exotic_worlds$getPrimeZ()));
        return new LapFastNoise(config, closedCopy(source, lap), lap, lattice, xzScale);
    }

    private static FastNoiseLiteAccessor getSource(FastNoiseConfig config) {
        return (FastNoiseLiteAccessor) ((FastNoiseConfigAccessor) config).exotic_worlds$getFastNoiseLite();
    }

    @Override
    public Interval range() {
        return this.config.range();
    }

    @Override
    public float get(double x, double y) {
        return this.config.get(x, y);
    }

    @Override
    public float get(double x, double y, double z) {
        double blockZ = z / this.xzScale;
        double foldedZ = this.lattice.foldZ(blockZ);
        double frameX = this.lattice.foldX(x / this.xzScale, blockZ) - foldedZ * this.shear;
        return this.noise.GetNoise(frameX * this.lap.getBlockScale(), y * this.lap.getInputScale(),
                foldedZ * this.lap.getBlockScale());
    }

    private static FNL closedCopy(FastNoiseLiteAccessor source, FastNoiseLap lap) {
        FNL copy = new FNL();
        copy.SetSeed(source.exotic_worlds$getSeed());
        copy.SetFrequency(UNIT_FREQUENCY);
        copy.SetNoiseType(source.exotic_worlds$getNoiseType());
        copy.SetFractalType(source.exotic_worlds$getFractalType());
        copy.SetFractalOctaves(source.exotic_worlds$getOctaves());
        copy.SetFractalLacunarity(source.exotic_worlds$getLacunarity());
        copy.SetFractalGain(source.exotic_worlds$getGain());
        copy.SetFractalWeightedStrength(source.exotic_worlds$getWeightedStrength());
        copy.SetFractalPingPongStrength(source.exotic_worlds$getPingPongStrength());
        copy.SetCellularDistanceFunction(source.exotic_worlds$getCellularDistanceFunction());
        copy.SetCellularReturnType(source.exotic_worlds$getCellularReturnType());
        copy.SetCellularJitter(source.exotic_worlds$getCellularJitterModifier());
        ((FastNoiseLapHolder) copy).exotic_worlds$setLap(lap);
        return copy;
    }

    private static int octavesOf(FastNoiseLiteAccessor source) {
        return SUMMED_FRACTALS.contains(source.exotic_worlds$getFractalType()) ? source.exotic_worlds$getOctaves()
                : SINGLE_OCTAVE;
    }

    private static int lapOf(WrapDomain domain) {
        return domain.isLooped() ? domain.domainLength : FastNoiseLap.OPEN;
    }

    public enum Reason {
        UNKNOWN_CONFIG("unknown_config"),
        NOISE_TYPE("noise_type"),
        ROTATION("rotation");

        private final String id;

        Reason(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    public record Refusal(Reason reason, String value) {
    }
}
