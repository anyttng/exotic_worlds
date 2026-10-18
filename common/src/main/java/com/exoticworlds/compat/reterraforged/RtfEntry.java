package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

// Where the game hands ReTerraForged a block position: folding it into the bounds here gives every copy of a column
// the same input, so the copies agree bit for bit; the lattices closing on the lap is what joins the seam itself.
public final class RtfEntry {
    public static @Nullable WorldFold generationFold() {
        WorldFold fold = lapFold();
        return fold.isWrapped() ? fold : null;
    }

    public static WorldFold lapFold() {
        return RtfLap.active() ? GenerationTransformerContext.context().transformer() : WorldFolds.NOOP;
    }

    public static int foldX(WorldFold fold, int x, int z) {
        return fold.blockLattice().foldX(x, z);
    }

    public static float foldX(WorldFold fold, float x, float z) {
        return (float) fold.blockLattice().foldX((double) x, (double) z);
    }

    public static int foldZ(WorldFold fold, int z) {
        return fold.blockLattice().foldZ(z);
    }

    public static float foldZ(WorldFold fold, float z) {
        return (float) fold.blockLattice().foldZ((double) z);
    }

    private RtfEntry() {
    }
}
