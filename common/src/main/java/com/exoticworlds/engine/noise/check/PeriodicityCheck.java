package com.exoticworlds.engine.noise.check;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.exoticworlds.accessors.BiomeZoomOwner;
import com.exoticworlds.accessors.PeriodicityMark;
import com.exoticworlds.core.ShapedChunkGenerator;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedQuart;
import com.exoticworlds.engine.noise.GenerationFoldContext;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

public final class PeriodicityCheck {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int DENSITY_COLUMN_STEP = 8;

    private static final String LIST_SEPARATOR = ",";
    private static final String FACE_SEPARATOR = "-";

    private record Probe(ServerLevel level, ChunkGenerator generator, RandomState randomState,
            Climate.Sampler sampler, BiomeResolver biomes, WorldFold fold, List<SeamPassage> passages) {
        private List<SeamCauseSearch.Lap> laps() {
            return this.passages.stream()
                    .filter(SeamCauseSearch.Lap.class::isInstance)
                    .map(SeamCauseSearch.Lap.class::cast)
                    .toList();
        }

        private int samples() {
            return this.passages.stream().mapToInt(passage -> passage.samples().size()).sum();
        }
    }

    public static SeamVerdict verdictOf(ServerLevel level) {
        return cached(((PeriodicityMark) level).exotic_worlds$seamVerdict(), () -> check(level));
    }

    public static SeamVerdict fullVerdictOf(ServerLevel level) {
        return cached(((PeriodicityMark) level).exotic_worlds$fullSeamVerdict(), () -> fullCheck(level));
    }

    static boolean fullVerdictKnown(ServerLevel level) {
        return ((PeriodicityMark) level).exotic_worlds$fullSeamVerdict().get() != null;
    }

    public static List<SeamVerdict> verdictsOf(MinecraftServer server) {
        return wrappedLevels(server).stream().map(PeriodicityCheck::verdictOf).toList();
    }

    public static List<SeamVerdict> fullVerdictsOf(MinecraftServer server) {
        return wrappedLevels(server).stream().map(PeriodicityCheck::fullVerdictOf).toList();
    }

    static List<ServerLevel> wrappedLevels(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (WorldLoopAttachments.foldOf(level).isWrapped()) {
                levels.add(level);
            }
        }

        return levels;
    }

    private static SeamVerdict cached(AtomicReference<SeamVerdict> slot, Supplier<SeamVerdict> compute) {
        SeamVerdict known = slot.get();
        if (known != null) {
            return known;
        }

        synchronized (slot) {
            known = slot.get();
            if (known == null) {
                known = compute.get();
                slot.set(known);
            }

            return known;
        }
    }

    private static SeamVerdict check(ServerLevel level) {
        String levelName = level.dimension().identifier().toString();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!ShapedChunkGenerator.tilesAtSeam(generator)) {
            LOGGER.info("[ew/world-loop] periodicity_skipped level={} generator={}",
                    levelName, generator.getClass().getSimpleName());
            return new SeamVerdict(level.dimension(), List.of(), SeamCauseSearch.generatorCause(generator), 0);
        }

        Probe probe = probeOf(level, generator, WorldLoopAttachments.foldOf(level));
        Map<String, SeamVerdict.Break> breaks = new LinkedHashMap<>();
        int brokenSamples = 0;
        for (SeamPassage passage : probe.passages()) {
            Map<SeamField, List<SeamCauseSearch.Point>> broken = new LinkedHashMap<>();
            for (SeamCauseSearch.Point surface : passage.samples()) {
                if (sampleBroken(probe, passage, surface, broken)) {
                    brokenSamples++;
                }
            }

            for (SeamVerdict.Break found : SeamCauseSearch.causesOf(broken, passage, probe.randomState(),
                    generator.getBiomeSource())) {
                breaks.putIfAbsent(found.field(), found);
            }
        }

        SeamVerdict verdict = new SeamVerdict(level.dimension(), List.copyOf(breaks.values()), null, 0);
        report(levelName, probe, verdict, brokenSamples);
        return verdict;
    }

    private static SeamVerdict fullCheck(ServerLevel level) {
        SeamVerdict base = verdictOf(level);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (base.unchecked() != null || !(generator instanceof NoiseBasedChunkGenerator noise)) {
            return base;
        }

        Map<String, SeamVerdict.Break> twinBreaks = new LinkedHashMap<>();
        int twinsRead = readTwins(probeOf(level, generator, WorldLoopAttachments.foldOf(level)), noise, twinBreaks);
        List<SeamVerdict.Break> breaks = new ArrayList<>(base.breaks());
        breaks.addAll(twinBreaks.values());
        return new SeamVerdict(level.dimension(), breaks, null, twinsRead);
    }

    private static Probe probeOf(ServerLevel level, ChunkGenerator generator, WorldFold fold) {
        RandomState randomState = level.getChunkSource().randomState();
        Climate.Sampler sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        BiomeResolver biomes = generator.getBiomeSource().createResolver(sampler);
        return new Probe(level, generator, randomState, sampler, biomes, fold, SeamPassage.of(fold));
    }

    private static int readTwins(Probe probe, NoiseBasedChunkGenerator generator,
            Map<String, SeamVerdict.Break> first) {
        int read = 0;
        for (LapTwinChunks.Inputs inputs : twinInputsOf(probe, generator)) {
            for (int z : SeamCauseSearch.Lap.SAMPLE_Z) {
                for (int x : SeamCauseSearch.Lap.SAMPLE_X) {
                    LapTwinChunks.Reading reading = LapTwinChunks.read(inputs,
                            ChunkPos.containing(new BlockPos(x, 0, z)));
                    for (SeamVerdict.Break found : reading.breaks()) {
                        first.putIfAbsent(found.field(), found);
                    }

                    read += reading.read() ? 1 : 0;
                }
            }
        }

        return read;
    }

    private static List<LapTwinChunks.Inputs> twinInputsOf(Probe probe, NoiseBasedChunkGenerator generator) {
        BiomeManager biomeManager = new BiomeManager(probe.biomes(),
                BiomeManager.obfuscateSeed(probe.level().getSeed()));
        ((BiomeZoomOwner) biomeManager).exotic_worlds$setOwner(probe.level());
        return probe.laps().stream()
                .map(lap -> new LapTwinChunks.Inputs(probe.level(), generator, probe.randomState(), biomeManager,
                        probe.fold(), lap))
                .toList();
    }

    private static boolean sampleBroken(Probe probe, SeamPassage passage, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        boolean column = collectColumn(probe, passage, surface, broken);
        boolean climate = collectClimate(probe, passage, surface, broken);
        boolean biome = collectBiome(probe, passage, surface, broken);
        return column || climate || biome;
    }

    private static boolean collectColumn(Probe probe, SeamPassage passage, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        OptionalInt brokenY = passage.edge() == null
                ? blockColumnBreak(probe, passage, surface)
                : densityColumnBreak(probe, passage, surface);
        if (brokenY.isEmpty()) {
            return false;
        }

        broken.computeIfAbsent(SeamField.COLUMN, _ -> new ArrayList<>())
                .add(new SeamCauseSearch.Point(surface.x(), brokenY.getAsInt(), surface.z()));
        return true;
    }

    private static OptionalInt blockColumnBreak(Probe probe, SeamPassage passage, SeamCauseSearch.Point surface) {
        SeamCauseSearch.Point away = passage.away(surface);
        NoiseColumn here = probe.generator()
                .getBaseColumn(surface.x(), surface.z(), probe.level(), probe.randomState());
        NoiseColumn there = probe.generator()
                .getBaseColumn(away.x(), away.z(), probe.level(), probe.randomState());
        LevelHeightAccessor height = probe.level();
        for (int y = height.getMinY(); y <= height.getMaxY(); y++) {
            if (here.getBlock(y) != there.getBlock(y)) {
                return OptionalInt.of(y);
            }
        }

        return OptionalInt.empty();
    }

    private static OptionalInt densityColumnBreak(Probe probe, SeamPassage passage,
            SeamCauseSearch.Point surface) {
        DensitySampler density = probe.randomState().getSampler(probe.randomState().router.finalDensity());
        LevelHeightAccessor height = probe.level();
        for (int y = height.getMinY(); y <= height.getMaxY(); y += DENSITY_COLUMN_STEP) {
            if (passage.differsAt(density, new SeamCauseSearch.Point(surface.x(), y, surface.z()))) {
                return OptionalInt.of(y);
            }
        }

        return OptionalInt.empty();
    }

    private static boolean collectClimate(Probe probe, SeamPassage passage, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        List<SeamField> fields = climateBreaks(probe.sampler(), probe.fold(), passage, surface);
        for (SeamField field : fields) {
            broken.computeIfAbsent(field, _ -> new ArrayList<>()).add(blockOf(quartOf(surface)));
        }

        return !fields.isEmpty();
    }

    static List<SeamField> climateBreaks(Climate.Sampler sampler, WorldFold fold, SeamPassage passage,
            SeamCauseSearch.Point surface) {
        SeamCauseSearch.Point away = passage.away(surface);
        SeamCauseSearch.Point quart = quartOf(surface);
        SeamCauseSearch.Point quartAway = quartOf(away);
        Climate.TargetPoint here = GenerationFoldContext.withFold(fold, () -> passage.reading(surface,
                () -> ClimateTargets.at(sampler, quart.x(), quart.y(), quart.z())));
        Climate.TargetPoint there = GenerationFoldContext.withFold(fold, () -> passage.reading(away,
                () -> ClimateTargets.at(sampler, quartAway.x(), quartAway.y(), quartAway.z())));
        List<SeamField> fields = new ArrayList<>();
        for (SeamField field : SeamField.values()) {
            if (field.climate() && field.climateOf(here) != field.climateOf(there)) {
                fields.add(field);
            }
        }

        return fields;
    }

    private static boolean collectBiome(Probe probe, SeamPassage passage, SeamCauseSearch.Point surface,
            Map<SeamField, List<SeamCauseSearch.Point>> broken) {
        if (!biomeBreaks(probe.biomes(), probe.fold(), passage, surface)) {
            return false;
        }

        broken.computeIfAbsent(SeamField.BIOME, _ -> new ArrayList<>()).add(blockOf(quartOf(surface)));
        return true;
    }

    static boolean biomeBreaks(BiomeResolver biomes, WorldFold fold, SeamPassage passage,
            SeamCauseSearch.Point surface) {
        SeamCauseSearch.Point quart = quartOf(surface);
        SeamCauseSearch.Point quartAway = passage.edge() == null
                ? quartOf(passage.away(surface))
                : foldedQuart(fold, quart);
        SeamCauseSearch.Point blockAway = blockOf(quartAway);
        Holder<Biome> here = GenerationFoldContext.withFold(fold, () -> passage.reading(surface,
                () -> biomes.getNoiseBiome(quart.x(), quart.y(), quart.z())));
        Holder<Biome> there = GenerationFoldContext.withFold(fold, () -> passage.reading(blockAway,
                () -> biomes.getNoiseBiome(quartAway.x(), quartAway.y(), quartAway.z())));
        return !here.equals(there);
    }

    private static SeamCauseSearch.Point foldedQuart(WorldFold fold, SeamCauseSearch.Point quart) {
        long folded = FoldedQuart.fold(fold, quart.x(), quart.y(), quart.z());
        return new SeamCauseSearch.Point(FoldedQuart.x(folded), quart.y(), FoldedQuart.z(folded));
    }

    private static SeamCauseSearch.Point quartOf(SeamCauseSearch.Point block) {
        return new SeamCauseSearch.Point(QuartPos.fromBlock(block.x()), QuartPos.fromBlock(block.y()),
                QuartPos.fromBlock(block.z()));
    }

    private static SeamCauseSearch.Point blockOf(SeamCauseSearch.Point quart) {
        return new SeamCauseSearch.Point(QuartPos.toBlock(quart.x()), QuartPos.toBlock(quart.y()),
                QuartPos.toBlock(quart.z()));
    }

    private static void report(String levelName, Probe probe, SeamVerdict verdict, int brokenSamples) {
        if (probe.fold().cuboidAtlas() != null) {
            reportEdges(levelName, probe, verdict, brokenSamples);
            return;
        }

        String axisName = probe.laps().stream()
                .map(lap -> lap.axis().getName().toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(LIST_SEPARATOR));
        String widthBlocks = probe.laps().stream()
                .map(lap -> Integer.toString(lap.widthBlocks()))
                .collect(Collectors.joining(LIST_SEPARATOR));
        int samples = probe.samples();
        if (!verdict.broken()) {
            LOGGER.info("[ew/world-loop] periodicity_ok level={} axis={} width_blocks={} samples={}",
                    levelName, axisName, widthBlocks, samples);
            return;
        }

        LOGGER.warn(
                "[ew/world-loop] periodicity_broken level={} axis={} width_blocks={} fields={} causes={} broken_samples={} samples={}",
                levelName, axisName, widthBlocks, verdict.fields(), verdict.causes(), brokenSamples, samples);
    }

    private static void reportEdges(String levelName, Probe probe, SeamVerdict verdict, int brokenSamples) {
        int edges = probe.passages().size();
        if (!verdict.broken()) {
            LOGGER.info("[ew/world-loop] periodicity_ok level={} edges={} samples={}",
                    levelName, edges, probe.samples());
            return;
        }

        String brokenEdges = verdict.breaks().stream()
                .map(SeamVerdict.Break::edge)
                .filter(Objects::nonNull)
                .map(edge -> edge.first() + FACE_SEPARATOR + edge.second())
                .distinct()
                .collect(Collectors.joining(LIST_SEPARATOR));
        LOGGER.warn(
                "[ew/world-loop] periodicity_broken level={} edges={} broken_edges={} fields={} causes={} broken_samples={} samples={}",
                levelName, edges, brokenEdges, verdict.fields(), verdict.causes(), brokenSamples, probe.samples());
    }

    private PeriodicityCheck() {
    }
}
