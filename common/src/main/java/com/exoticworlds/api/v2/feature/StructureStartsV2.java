package com.exoticworlds.api.v2.feature;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** One structure set of the dimension, with the starts the game's own placement gives it inside the loop. */
public interface StructureStartsV2 extends MomentV2 {
    /** The structure set. */
    Holder<StructureSet> getStructureSet();

    /** Every start the set's own placement puts inside the loop. */
    List<Pick> getPicks();

    /**
     * Whether {@code structure} can start in {@code chunk}, which takes a biome that allows it and room for the
     * structure.
     */
    boolean canStart(Holder<Structure> structure, ChunkPos chunk);

    /**
     * One start the set's own placement gives.
     *
     * @param chunk the start's chunk
     * @param structure the structure that starts there, empty where the placement picks a chunk no structure of the
     *        set can use
     */
    record Pick(ChunkPos chunk, Optional<Holder<Structure>> structure) {
    }

    /**
     * One start a feature adds.
     *
     * @param structure a structure of this set
     * @param chunk the chunk it starts in, inside the loop. A start past the edge is dropped and logged
     */
    record Added(Holder<Structure> structure, ChunkPos chunk) {
    }
}
