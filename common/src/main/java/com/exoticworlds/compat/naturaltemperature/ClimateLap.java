package com.exoticworlds.compat.naturaltemperature;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.MapMaker;
import com.exoticworlds.compat.worldgen.FoldCompression;
import com.exoticworlds.compat.worldgen.LapCells;
import com.exoticworlds.compat.worldgen.LapFrame;
import com.exoticworlds.core.fold.TranslationLattice;
import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldLoopAttachments;
import com.exoticworlds.core.fold.WrapDomain;
import com.exoticworlds.engine.level.CurrentServer;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.exoticworlds.engine.noise.NoiseConstants;
import com.exoticworlds.engine.noise.periodic.PeriodicNoiseSampler;

import naturaltemperature.ModConfig;
import naturaltemperature.NaturalTemperature;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
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

    public static @Nullable ClimateLap getBound() {
        return of(GenerationFoldContext.getContext().getWrappedFold());
    }

    public static @Nullable ClimateLap ofTheOverworld() {
        return of(WorldLoopAttachments.getWrappedFold(CurrentServer.get(), Level.OVERWORLD));
    }

    private static @Nullable ClimateLap of(@Nullable WorldFold fold) {
        return fold == null ? null
                : BY_FOLD.computeIfAbsent(fold, key -> new ClimateLap(key.getBlockLattice(), FoldCompression.of(key)));
    }

    public int x(int blockX, int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return x(blockX, blockZ, mode(config), config.equatorial_distance, config.looping_world);
    }

    public int x(int blockX, int blockZ, int mode, double equatorialDistanceBlocks, boolean isLoopingWorld) {
        WrapDomain x = this.lattice.getX();
        if (!x.isLooped() || !isRadial(mode)) {
            return signedX(blockX, blockZ, mode, equatorialDistanceBlocks, isLoopingWorld);
        }

        double scale = xScale(mode, equatorialDistanceBlocks, isLoopingWorld);
        return (int) Math.round(mirrored(frameX(blockX, blockZ), x) * scale);
    }

    public int signedX(int blockX, int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return signedX(blockX, blockZ, mode(config), config.equatorial_distance, config.looping_world);
    }

    int signedX(int blockX, int blockZ, int mode, double equatorialDistanceBlocks, boolean isLoopingWorld) {
        double scale = xScale(mode, equatorialDistanceBlocks, isLoopingWorld);
        WrapDomain x = this.lattice.getX();
        return (int) Math.round((x.isLooped() ? centered(frameX(blockX, blockZ), x) : blockX) * scale);
    }

    private double frameX(int blockX, int blockZ) {
        WrapDomain z = this.lattice.getZ();
        return this.frame.frameX(blockX, z.isLooped() ? blockZ - center(z) : blockZ);
    }

    public double carriedSide(int blockX, int blockZ, int handedX, double wave) {
        return wave + signedX(blockX, blockZ) - handedX;
    }

    double xScale(int mode, double equatorialDistanceBlocks, boolean isLoopingWorld) {
        WrapDomain z = this.lattice.getZ();
        if (!z.isLooped() || isRadial(mode) || equatorialDistanceBlocks <= 0.0 || !isLoopingWorld) {
            return this.compression;
        }

        double halfTurn = z.domainLength / (2.0 * bands(equatorialDistanceBlocks));
        return POLE_TO_EQUATOR_SPANS * equatorialDistanceBlocks / halfTurn;
    }

    public int z(int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        return z(blockZ, mode(config), config.equatorial_distance, config.equator_offset,
                config.looping_world);
    }

    public int z(int blockZ, int mode, double equatorialDistanceBlocks, double equatorOffsetBlocks, boolean isLoopingWorld) {
        WrapDomain z = this.lattice.getZ();
        if (!z.isLooped()) {
            return compressed(blockZ);
        }

        if (isRadial(mode)) {
            return (int) Math.round(mirrored(blockZ, z) * this.compression);
        }

        if (equatorialDistanceBlocks <= 0.0) {
            return blockZ;
        }

        if (!isLoopingWorld) {
            return (int) -Math.round(mirrored(blockZ, z) * this.compression);
        }

        return (int) Math.round(tent(blockZ, equatorialDistanceBlocks, equatorOffsetBlocks));
    }

    public double carriedWave(int blockZ, int handedZ, double wave, double distortion) {
        ModConfig config = NaturalTemperature.CONFIG;
        return carriedWave(blockZ, handedZ, wave, distortion, mode(config), config.equatorial_distance,
                config.equator_offset, config.looping_world);
    }

    double carriedWave(int blockZ, int handedZ, double wave, double distortion, int mode, double equatorialDistanceBlocks,
            double equatorOffsetBlocks, boolean isLoopingWorld) {
        if (!this.lattice.getZ().isLooped() || isRadial(mode) || equatorialDistanceBlocks <= 0.0) {
            return wave;
        }

        double displaced = blockZ + (distortion + wave) / xScale(mode, equatorialDistanceBlocks, isLoopingWorld);
        return latitude(displaced, equatorialDistanceBlocks, equatorOffsetBlocks, isLoopingWorld) - handedZ - distortion;
    }

    double latitude(double blockZ, double equatorialDistanceBlocks, double equatorOffsetBlocks, boolean isLoopingWorld) {
        return isLoopingWorld
                ? tent(blockZ, equatorialDistanceBlocks, equatorOffsetBlocks)
                : -mirrored(blockZ, this.lattice.getZ()) * this.compression;
    }

    private double tent(double blockZ, double equatorialDistanceBlocks, double equatorOffsetBlocks) {
        WrapDomain z = this.lattice.getZ();
        double turns = (blockZ - z.lowerBound) / z.domainLength * bands(equatorialDistanceBlocks);
        double towardEquator = 1.0 - Math.abs(2.0 * (turns - Math.floor(turns)) - 1.0);
        return equatorialDistanceBlocks * (2.0 * towardEquator - 1.0) - equatorOffsetBlocks;
    }

    public boolean isXWaveClosed() {
        return this.lattice.getX().isLooped();
    }

    public boolean isZWaveClosed() {
        return this.lattice.getZ().isLooped();
    }

    public double xWave(WaveCells cells, long seed, double handedX, double amplitude, double baseRate) {
        ModConfig config = NaturalTemperature.CONFIG;
        double scale = xScale(mode(config), config.equatorial_distance, config.looping_world);
        return wave(cells, seed, handedX, this.lattice.getX().domainLength * scale, amplitude,
                config.wave_frequency * baseRate);
    }

    public double zWave(WaveCells cells, long seed, int blockZ, double amplitude, double baseRate) {
        ModConfig config = NaturalTemperature.CONFIG;
        double scale = xScale(mode(config), config.equatorial_distance, config.looping_world);
        WrapDomain z = this.lattice.getZ();
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

    public double lapBandWidthBlocks(double widthBlocks) {
        ModConfig config = NaturalTemperature.CONFIG;
        return lapBandWidthBlocks(widthBlocks, mode(config), config.equatorial_distance, config.looping_world);
    }

    double lapBandWidthBlocks(double widthBlocks, int mode, double equatorialDistanceBlocks, boolean isLoopingWorld) {
        if (!this.lattice.getX().isLooped() || widthBlocks <= 0.0) {
            return widthBlocks;
        }

        double lap = this.lattice.getX().domainLength * xScale(mode, equatorialDistanceBlocks, isLoopingWorld);
        return lap / (HUMIDITY_BAND_SPANS * LapCells.whole(lap / (HUMIDITY_BAND_SPANS * widthBlocks), ONE_TURN));
    }

    public double noise(NormalNoise noise, double cellsPerBlock, double handedX, double handedZ, int blockZ) {
        ModConfig config = NaturalTemperature.CONFIG;
        double scale = xScale(mode(config), config.equatorial_distance, config.looping_world);
        return noise(noise, cellsPerBlock, handedX, handedZ, blockZ, scale);
    }

    double noise(NormalNoise noise, double cellsPerBlock, double handedX, double handedZ, int blockZ, double scale) {
        double first = octaves(noise.first, cellsPerBlock, handedX, handedZ, blockZ, scale);
        double second = octaves(noise.second, cellsPerBlock * NoiseConstants.SECOND_LAYER_DETUNE, handedX, handedZ,
                blockZ, scale);
        return (first + second) * noise.valueFactor;
    }

    private double octaves(PerlinNoise stack, double cellsPerBlock, double handedX, double handedZ, int blockZ,
            double scale) {
        double value = 0.0;
        double frequency = stack.lowestFreqInputFactor;
        double amplitude = stack.lowestFreqValueFactor;
        for (int index = 0; index < stack.amplitudes.size(); index++) {
            ImprovedNoise octave = stack.getOctaveNoise(index);
            if (octave != null) {
                double cells = cellsPerBlock * frequency;
                double shrink = shrink(cells, scale);
                value += stack.amplitudes.getDouble(index) * amplitude / shrink * noise(octave.p, octave.xo,
                        octave.yo, octave.zo, cells, handedX, handedZ, blockZ, scale, shrink);
            }

            frequency *= 2.0;
            amplitude /= 2.0;
        }

        return value;
    }

    double shrink(double cellsPerBlock, double scale) {
        WrapDomain x = this.lattice.getX();
        WrapDomain z = this.lattice.getZ();
        double xCells = x.isLooped() ? x.domainLength * scale * cellsPerBlock : OPEN;
        double zCells = z.isLooped() ? z.domainLength * scale * cellsPerBlock : OPEN;
        return Math.max(shrinkOf(xCells), shrinkOf(zCells));
    }

    double noise(byte[] permutations, double offsetX, double offsetY, double offsetZ, double cellsPerBlock,
            double handedX, double handedZ, int blockZ, double scale, double shrink) {
        WrapDomain x = this.lattice.getX();
        WrapDomain z = this.lattice.getZ();
        double xCells = x.isLooped() ? x.domainLength * scale * cellsPerBlock : OPEN;
        double zCells = z.isLooped() ? z.domainLength * scale * cellsPerBlock : OPEN;
        double alongZ = z.isLooped() ? centered(blockZ, z) * scale : handedZ;
        double latticeX = handedX * cellsPerBlock * shrinkOn(xCells, shrink) + offsetX;
        double latticeY = alongZ * cellsPerBlock * shrinkOn(zCells, shrink) + offsetY;
        return PeriodicNoiseSampler.sampleLattice(permutations, latticeX, latticeY, offsetZ, periodOf(xCells, shrink),
                periodOf(zCells, shrink), PeriodicNoiseSampler.UNBOUNDED_PERIOD);
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

    public int bands(double equatorialDistanceBlocks) {
        double halfBand = POLE_TO_EQUATOR_SPANS * equatorialDistanceBlocks / this.compression;
        int bands = Math.max(1, (int) Math.ceil(this.lattice.getZ().domainLength / (2.0 * halfBand)));
        return bands % 2 == 0 ? bands + 1 : bands;
    }

    private int compressed(int block) {
        return (int) Math.round(block * this.compression);
    }

    static int mode(ModConfig config) {
        int mode = config.generation_mode;
        return mode < CUSTOM_ONE ? mode : mode(mode, getCustomBody(config));
    }

    private static @Nullable String getCustomBody(ModConfig config) {
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

    static boolean isRadial(int mode) {
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
