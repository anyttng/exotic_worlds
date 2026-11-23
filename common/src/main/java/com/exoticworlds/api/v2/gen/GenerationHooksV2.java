package com.exoticworlds.api.v2.gen;

import java.util.List;

import com.exoticworlds.api.v2.geometry.WorldGeometryV2;
import com.exoticworlds.api.v2.option.ShapeOptionsV2;
import com.exoticworlds.bridge.v2.gen.GenerationBridge;

import net.minecraft.world.level.levelgen.RandomState;

/**
 * The moments world generation offers a feature, and the door a mod hooks them at. A hook registers at startup,
 * before the registration boundary closes at {@code MinecraftServer.runServer}.
 *
 * <p>Each moment has one table shared with {@link com.exoticworlds.api.v1.gen.GenerationHooks}: the hooks of both
 * versions run in one order of their keys, and a key registered through both throws on the second registration.</p>
 */
public final class GenerationHooksV2 {

    /**
     * A hook run once per {@link RandomState} built for a folding level, as it finishes building: the moment its
     * noise router exists and nothing has sampled it yet, so rewriting a noise in that router still reaches every
     * chunk. The geometry and the options are the ones the world was created with.
     *
     * <p><strong>A hook gates itself.</strong> Every registered hook runs for every folding level of every world,
     * whatever shape made it and whichever mod declared that shape: a hook is not scoped to the shape it was
     * registered beside, and a level is one of several a world builds, so it runs once per dimension too. Read
     * {@code geometry} and {@code options} and return early unless both are what this hook is for.</p>
     */
    @FunctionalInterface
    public interface RandomStateHook {
        void run(RandomState randomState, WorldGeometryV2 geometry, ShapeOptionsV2 options, int seaLevel);
    }

    /**
     * Registers {@code hook} at the {@link RandomState} moment under {@code key}, which orders the hooks against one
     * another and must be unique across every mod: prefix it with your mod id where a clash is possible.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or a hook of either version is
     *         already registered under {@code key} at this moment
     */
    public static void atRandomState(String key, RandomStateHook hook) {
        GenerationBridge.atRandomState(key, hook);
    }

    /**
     * A hook asked once per structure set of a folding level, before the first structure start of that level is
     * generated or searched for, for the starts it adds to what the set's own placement picks. An added start is
     * generated in its chunk and found by {@code /locate} and every other structure search.
     *
     * <p><strong>A hook gates itself</strong>, as a {@link RandomStateHook} does: it runs for every set of every
     * folding level. Return an empty list unless the set, the geometry and the options are what this hook is for, and
     * before calling {@link StructureStartsV2#picks()}, which costs a structure check per picked chunk.</p>
     */
    @FunctionalInterface
    public interface StructureStartsHook {
        List<StructureStartsV2.Added> starts(StructureStartsV2 set);
    }

    /**
     * Registers {@code hook} at the structure-start moment under {@code key}, which orders the hooks against one
     * another and must be unique across every mod: prefix it with your mod id where a clash is possible.
     *
     * @throws IllegalStateException if the registration boundary has already closed, or a hook of either version is
     *         already registered under {@code key} at this moment
     */
    public static void atStructureStarts(String key, StructureStartsHook hook) {
        GenerationBridge.atStructureStarts(key, hook);
    }

    private GenerationHooksV2() {
    }
}
