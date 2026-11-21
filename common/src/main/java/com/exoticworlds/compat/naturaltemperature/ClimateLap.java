package com.exoticworlds.compat.naturaltemperature;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.MapMaker;
import com.exoticworlds.compat.worldgen.FoldCompression;
import com.exoticworlds.compat.worldgen.LapCells;
import com.exoticworlds.compat.worldgen.LapFrame;
import com.exoticworlds.core.TranslationLattice;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.core.WrapDomain;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.periodic.PeriodicNoiseSampler;
import com.naturaltemperature.ModConfig;
import com.naturaltemperature.NaturalTemperature;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class ClimateLap {
    private static final int CIRCULAR = 3;

    private static final int FIXED_CIRCULAR = 4;

    private static final int CIRCULAR_SIX = 7;

    private static final int FIXED_CIRCULAR_SIX = 8;

    private static final int SQUARE = 9;

    private static final int FIXED_SQUARE = 10;

    private static final int CUSTOM_ONE = 101;

    private static final int CUSTOM_TWO = 102;

    private static final int CUSTOM_THREE = 103;

    private static final int CUSTOM_FOUR = 104;

    private static final int CUSTOM_FIVE = 105;

    private static final String CIRCULAR_BODY = "circular";

    private static final String CIRCULAR_SIX_BODY = "circular6";

    private static final String SQUARE_BODY = "square6";

    private static final double POLE_TO_EQUATOR_SPANS = 2.0;

    private static final double HUMIDITY_BAND_SPANS = 4.0;

    private static final long ONE_TURN = 1L;

    private static final double OPEN = 0.0;

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
        return of(GenerationFoldContext.context().wrappedFold());
    }

    public static @Nullable ClimateLap ofTheOverworld() {
        return of(WorldLoopAttachments.wrappedFoldOf(CurrentServer.get(), Level.OVERWORLD));
    }

    private static @Nullable ClimateLap of(@Nullable WorldFold fold) {
        return fold == null ? null
                : BY_FOLD.computeIfAbsent(fold, key -> new ClimateLap(key.blockLattice(), FoldCompression.of(key)));
    }

    public int x(int blockX, int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return x(blockX, blockZ, mode(config), config.equatorial_distance, config.looping_world);
    }

    public int x(int blockX, int blockZ, int mode, double equatorialDistance, boolean loopingWorld) {
        WrapDomain x = this.lattice.x();
        if (!x.loops() || !radial(mode)) {
            return signedX(blockX, blockZ, mode, equatorialDistance, loopingWorld);
        }

        double scale = xScale(mode, equatorialDistance, loopingWorld);
        return (int) Math.round(mirrored(frameX(blockX, blockZ), x) * scale);
    }

    public int signedX(int blockX, int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return signedX(blockX, blockZ, mode(config), config.equatorial_distance, config.looping_world);
    }

    int signedX(int blockX, int blockZ, int mode, double equatorialDistance, boolean loopingWorld) {
        double scale = xScale(mode, equatorialDistance, loopingWorld);
        WrapDomain x = this.lattice.x();
        return (int) Math.round((x.loops() ? centered(frameX(blockX, blockZ), x) : blockX) * scale);
    }

    private double frameX(int blockX, int blockZ) {
        WrapDomain z = this.lattice.z();
        return this.frame.frameX(blockX, z.loops() ? blockZ - center(z) : blockZ);
    }

    public double carriedSide(int blockX, int blockZ, int handedX, double wave) {
        return wave + signedX(blockX, blockZ) - handedX;
    }

    double xScale(int mode, double equatorialDistance, boolean loopingWorld) {
        WrapDomain z = this.lattice.z();
        if (!z.loops() || radial(mode) || equatorialDistance <= 0.0 || !loopingWorld) {
            return this.compression;
        }

        double halfTurn = z.domainLength / (2.0 * bands(equatorialDistance));
        return POLE_TO_EQUATOR_SPANS * equatorialDistance / halfTurn;
    }

    public int z(int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return z(blockZ, mode(config), config.equatorial_distance, config.equator_offset,
                config.looping_world);
    }

    public int z(int blockZ, int mode, double equatorialDistance, double equatorOffset, boolean loopingWorld) {
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

        if (!loopingWorld) {
            return (int) -Math.round(mirrored(blockZ, z) * this.compression);
        }

        return (int) Math.round(tent(blockZ, equatorialDistance, equatorOffset));
    }

    public double carriedWave(int blockZ, int handedZ, double wave, double distortion) {
        ModConfig config = NaturalTemperature.CONFIG;
        return carriedWave(blockZ, handedZ, wave, distortion, mode(config), config.equatorial_distance,
                config.equator_offset, config.looping_world);
    }

    double carriedWave(int blockZ, int handedZ, double wave, double distortion, int mode, double equatorialDistance,
            double equatorOffset, boolean loopingWorld) {
        if (!this.lattice.z().loops() || radial(mode) || equatorialDistance <= 0.0) {
            return wave;
        }

        double displaced = blockZ + (distortion + wave) / xScale(mode, equatorialDistance, loopingWorld);
        return latitude(displaced, equatorialDistance, equatorOffset, loopingWorld) - handedZ - distortion;
    }

    double latitude(double blockZ, double equatorialDistance, double equatorOffset, boolean loopingWorld) {
        return loopingWorld
                ? tent(blockZ, equatorialDistance, equatorOffset)
                : -mirrored(blockZ, this.lattice.z()) * this.compression;
    }

    private double tent(double blockZ, double equatorialDistance, double equatorOffset) {
        WrapDomain z = this.lattice.z();
        double turns = (blockZ - z.lowerBound) / z.domainLength * bands(equatorialDistance);
        double towardEquator = 1.0 - Math.abs(2.0 * (turns - Math.floor(turns)) - 1.0);
        return equatorialDistance * (2.0 * towardEquator - 1.0) - equatorOffset;
    }

    public boolean closesXWave() {
        return this.lattice.x().loops();
    }

    public boolean closesZWave() {
        return this.lattice.z().loops();
    }

    public double xWave(WaveCells cells, long seed, double handedX, double amplitude, double baseRate) {
        ModConfig config = NaturalTemperature.CONFIG;
        double scale = xScale(mode(config), config.equatorial_distance, config.looping_world);
        return wave(cells, seed, handedX, this.lattice.x().domainLength * scale, amplitude,
                config.wave_frequency * baseRate);
    }

    public double zWave(WaveCells cells, long seed, int blockZ, double amplitude, double baseRate) {
        ModConfig config = NaturalTemperature.CONFIG;
        double scale = xScale(mode(config), config.equatorial_distance, config.looping_world);
        WrapDomain z = this.lattice.z();
        return wave(cells, seed, centered(blockZ, z) * scale, z.domainLength * scale, amplitude,
                config.wave_frequency * baseRate);
    }

    static double wave(WaveCells cells, long seed, double position, double lap, double amplitude, double rate) {
        if (amplitude == 0.0 || rate <= 0.0) {
            return 0.0;
        }

        long perLap = LapCells.whole(rate * lap, ONE_TURN);
        double along = position / lap * perLap;
        long cell = (long) Math.floor(along);
        if (along < point(cells, seed, cell, perLap)) {
            cell--;
        }

        double left = point(cells, seed, cell, perLap);
        double right = point(cells, seed, cell + 1, perLap);
        double leftValue = cells.value(seed, Math.floorMod(cell, perLap)) * amplitude;
        double rightValue = cells.value(seed, Math.floorMod(cell + 1, perLap)) * amplitude;
        double share = (along - left) / (right - left);
        return leftValue + (rightValue - leftValue) * share * share * (3.0 - 2.0 * share);
    }

    private static double point(WaveCells cells, long seed, long cell, long perLap) {
        return cell + cells.pointShare(seed, Math.floorMod(cell, perLap));
    }

    public double lapBandWidth(double width) {
        ModConfig config = NaturalTemperature.CONFIG;
        return lapBandWidth(width, mode(config), config.equatorial_distance, config.looping_world);
    }

    double lapBandWidth(double width, int mode, double equatorialDistance, boolean loopingWorld) {
        if (!this.lattice.x().loops() || width <= 0.0) {
            return width;
        }

        double lap = this.lattice.x().domainLength * xScale(mode, equatorialDistance, loopingWorld);
        return lap / (HUMIDITY_BAND_SPANS * LapCells.whole(lap / (HUMIDITY_BAND_SPANS * width), ONE_TURN));
    }

    public double noise(NoiseStack stack, double cellsPerBlock, double handedX, double handedZ, int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        double scale = xScale(mode(config), config.equatorial_distance, config.looping_world);
        return noise(stack, cellsPerBlock, handedX, handedZ, blockZ, scale);
    }

    double noise(NoiseStack stack, double cellsPerBlock, double handedX, double handedZ, int blockZ, double scale) {
        double value = 0.0;
        for (NoiseStack.Layer layer : stack.layers) {
            PerlinNoise octave = (PerlinNoise) layer.noise();
            double cells = cellsPerBlock * layer.frequency();
            double shrink = shrink(cells, scale);
            value += layer.amplitude() / shrink * noise(octave.perms, octave.offsetX, octave.offsetY, octave.offsetZ,
                    cells, handedX, handedZ, blockZ, scale, shrink);
        }

        return value;
    }

    double shrink(double cellsPerBlock, double scale) {
        WrapDomain x = this.lattice.x();
        WrapDomain z = this.lattice.z();
        double xCells = x.loops() ? x.domainLength * scale * cellsPerBlock : OPEN;
        double zCells = z.loops() ? z.domainLength * scale * cellsPerBlock : OPEN;
        return Math.max(shrinkOf(xCells), shrinkOf(zCells));
    }

    double noise(byte[] permutations, double offsetX, double offsetY, double offsetZ, double cellsPerBlock,
            double handedX, double handedZ, int blockZ, double scale, double shrink) {
        WrapDomain x = this.lattice.x();
        WrapDomain z = this.lattice.z();
        double xCells = x.loops() ? x.domainLength * scale * cellsPerBlock : OPEN;
        double zCells = z.loops() ? z.domainLength * scale * cellsPerBlock : OPEN;
        double alongZ = z.loops() ? centered(blockZ, z) * scale : handedZ;
        double latticeX = handedX * cellsPerBlock * shrinkOn(xCells, shrink) + offsetX;
        double latticeZ = alongZ * cellsPerBlock * shrinkOn(zCells, shrink) + offsetZ;
        return PeriodicNoiseSampler.sampleLattice(permutations, latticeX, offsetY, latticeZ, periodOf(xCells, shrink),
                periodOf(zCells, shrink));
    }

    private static double shrinkOf(double cells) {
        return cells == OPEN ? 1.0 : LapCells.whole(cells, ONE_TURN) / cells;
    }

    private static double shrinkOn(double cells, double shrink) {
        return cells == OPEN ? shrink : periodOf(cells, shrink) / cells;
    }

    static long periodOf(double cells, double shrink) {
        return cells == OPEN ? PeriodicNoiseSampler.UNBOUNDED_PERIOD : LapCells.whole(cells * shrink, ONE_TURN);
    }

    public int bands(double equatorialDistance) {
        double halfBand = POLE_TO_EQUATOR_SPANS * equatorialDistance / this.compression;
        int bands = Math.max(1, (int) Math.ceil(this.lattice.z().domainLength / (2.0 * halfBand)));
        return bands % 2 == 0 ? bands + 1 : bands;
    }

    private int compressed(int block) {
        return (int) Math.round(block * this.compression);
    }

    static int mode(ModConfig config) {
        int mode = config.generation_mode;
        return mode < CUSTOM_ONE ? mode : mode(mode, customBody(config));
    }

    private static @Nullable String customBody(ModConfig config) {
        return switch (config.generation_mode) {
            case CUSTOM_ONE -> config.custom_mode_id_1;
            case CUSTOM_TWO -> config.custom_mode_id_2;
            case CUSTOM_THREE -> config.custom_mode_id_3;
            case CUSTOM_FOUR -> config.custom_mode_id_4;
            case CUSTOM_FIVE -> config.custom_mode_id_5;
            default -> null;
        };
    }

    static int mode(int mode, @Nullable String customBody) {
        if (mode < CUSTOM_ONE || mode > CUSTOM_FIVE || customBody == null) {
            return mode;
        }

        return switch (customBody) {
            case CIRCULAR_BODY -> CIRCULAR;
            case CIRCULAR_SIX_BODY -> CIRCULAR_SIX;
            case SQUARE_BODY -> SQUARE;
            default -> mode;
        };
    }

    static boolean radial(int mode) {
        return mode == CIRCULAR || mode == FIXED_CIRCULAR || mode == CIRCULAR_SIX || mode == FIXED_CIRCULAR_SIX
                || mode == SQUARE || mode == FIXED_SQUARE;
    }

    static double mirrored(double coord, WrapDomain domain) {
        return Math.abs(centered(coord, domain));
    }

    static double centered(double coord, WrapDomain domain) {
        double half = domain.domainLength / 2.0;
        double fromCenter = coord - center(domain);
        return fromCenter - Math.floor((fromCenter + half) / domain.domainLength) * domain.domainLength;
    }

    private static double center(WrapDomain domain) {
        return domain.lowerBound + domain.domainLength / 2.0;
    }
}
