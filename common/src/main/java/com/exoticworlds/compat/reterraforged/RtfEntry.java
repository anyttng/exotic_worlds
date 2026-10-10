package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

// Where the game hands ReTerraForged a block position: folding it into the bounds here gives every copy of a column
// the same input, so the copies agree bit for bit; the lattices closing on the lap is what joins the seam itself.
public final class RtfEntry {
    public static @Nullable WorldFold generationFold() {
        return RtfLap.active() ? GenerationTransformerContext.context().wrappedTransformer() : null;
    }

    public static int foldX(@Nullable WorldFold fold, int x, int z) {
        return fold == null ? x : fold.blockLattice().foldX(x, z);
    }

    public static float foldX(@Nullable WorldFold fold, float x, float z) {
        return fold == null ? x : (float) fold.blockLattice().foldX((double) x, (double) z);
    }

    public static int foldZ(@Nullable WorldFold fold, int z) {
        return fold == null ? z : fold.blockLattice().foldZ(z);
    }

    public static float foldZ(@Nullable WorldFold fold, float z) {
        return fold == null ? z : (float) fold.blockLattice().foldZ((double) z);
    }

    private RtfEntry() {
    }
}
