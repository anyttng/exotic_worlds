package com.exoticworlds.api.v1.gen;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.Broken;
import com.exoticworlds.bridge.v1.gen.ProbeBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * What this mod does to terrain, offered to a mod that reproduces generation outside the chunk map: an LOD generator
 * sampling on its own threads, or a harness measuring many seeds in one server session with no world on disk. Two
 * pieces are offered: the router a seed is given, and the fold bound on the thread that samples it. The crumb passes
 * the chunk map runs are not offered, so a chunk filled outside it keeps the detached masses a generated one loses.
 *
 * <p>Nothing here generates a chunk. The caller drives vanilla's own generator ({@code fillFromNoise},
 * {@code buildSurface}, {@code applyCarvers}) and uses these two to make that run the folded world's rather than
 * an unbounded vanilla one.</p>
 */
public final class GenerationProbe {
    static final String NO_CRUMB_PASSES = "The crumb passes are not offered outside the chunk map, so nothing is"
            + " cleared, recorded or named. Nothing replaces this.";

    private static final long[] NO_POSITIONS = new long[0];

    /**
     * The noise router a folding level would build for {@code seed}, terrain ceiling and fold included, as the chunk
     * map builds the level's own. Registered {@code RandomState} hooks run for it exactly as they do for the level's.
     *
     * <p>The level's own seed is not consulted: this is the router that level's shape and options would give any
     * seed, which is what lets one session measure a thousand of them.</p>
     *
     * @throws IllegalArgumentException if the level's generator is not noise based, so there is no router to build
     */
    public static RandomState randomState(ServerLevel level, long seed) {
        return ProbeBridge.randomState(level, seed);
    }

    /**
     * Runs {@code body} with the level's fold bound on the calling thread, which is what a sampler needs to read the
     * folded field. Generation driven outside the chunk map reaches no hook that would bind it, so a density read
     * without this returns the unbounded vanilla value. The binding is per thread and lasts only for the call.
     */
    public static void withFold(ServerLevel level, Runnable body) {
        ProbeBridge.withFold(level, body);
    }

    /**
     * Does nothing and leaves the chunk as it was.
     *
     * @deprecated the crumb passes are not offered outside the chunk map, and nothing replaces this call.
     */
    @Deprecated
    @Broken(NO_CRUMB_PASSES)
    public static void sweepCrumbs(ServerLevel level, ChunkAccess chunk) {
    }

    /**
     * Answers {@code null} for every chunk.
     *
     * @deprecated the crumb passes are not offered outside the chunk map, and nothing replaces this call.
     */
    @Deprecated
    @Broken(NO_CRUMB_PASSES)
    @SuppressWarnings("InlineMeSuggester")
    public static @Nullable TerrainSnapshot snapshotOf(ChunkAccess chunk) {
        return null;
    }

    /**
     * Names no position, whatever the window holds.
     *
     * @deprecated the crumb passes are not offered outside the chunk map, and nothing replaces this call.
     */
    @Deprecated
    @Broken(NO_CRUMB_PASSES)
    public static long[] borderCrumbs(TerrainSnapshot[] window) {
        return NO_POSITIONS;
    }

    /**
     * Writes nothing and answers no position, whatever the window holds.
     *
     * @deprecated the crumb passes are not offered outside the chunk map, and nothing replaces this call.
     */
    @Deprecated
    @Broken(NO_CRUMB_PASSES)
    public static long[] clearBorderCrumbs(ChunkAccess chunk, TerrainSnapshot[] window, LevelReader reader) {
        return NO_POSITIONS;
    }

    private GenerationProbe() {
    }
}
