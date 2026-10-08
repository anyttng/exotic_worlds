package com.exoticworlds.api.v1.gen;

import java.util.List;
import java.util.Optional;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.api.v1.option.GenerationOptions;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * One structure set of a folding level, as a {@link GenerationHooks.StructureStartsHook} reads it: what its own
 * placement puts inside the bounds, and whether a structure could start in a given chunk. Every answer depends on the
 * seed, the shape and the options alone, so a hook that reads only this returns the same starts on every load.
 */
public interface StructureStarts {

    ToroidalShape shape();

    GenerationOptions options();

    Holder<StructureSet> structureSet();

    /**
     * The chunks inside the bounds the set's own placement picks, each with the structure the game's weighted choice
     * starts there, or empty where every structure of the set fails its checks. Only a random-spread placement is
     * enumerated; any other placement answers an empty list.
     *
     * @throws IllegalStateException if either horizontal axis does not loop
     */
    List<Pick> picks();

    /**
     * Whether {@code structure} finds a valid generation point in {@code chunk} — the same test the game runs before
     * it starts a structure, its biome check included. {@code chunk} is inside the bounds.
     */
    boolean canStart(Holder<Structure> structure, ChunkPos chunk);

    /** A chunk the set's placement picks, and the structure that starts there. */
    record Pick(ChunkPos chunk, Optional<Holder<Structure>> structure) {
    }

    /**
     * A start a hook adds: {@code structure}, one of the set's own, generated in {@code chunk}. A start outside the
     * bounds, on a picked chunk, or of a structure the set does not hold is dropped with a warning.
     */
    record Added(Holder<Structure> structure, ChunkPos chunk) {
    }
}
