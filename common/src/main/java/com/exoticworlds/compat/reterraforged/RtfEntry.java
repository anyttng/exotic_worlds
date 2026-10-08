package com.exoticworlds.compat.reterraforged;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.engine.noise.GenerationTransformerContext;

import net.minecraft.core.Direction;

// Where the game hands ReTerraForged a block position: folding it into the bounds here gives every copy of a column
// the same input, so the copies agree bit for bit; the lattices closing on the lap is what joins the seam itself.
public final class RtfEntry {
    public static @Nullable WorldFold generationFold() {
        return RtfLap.active() ? GenerationTransformerContext.context().wrappedTransformer() : null;
    }

    public static int foldBlock(@Nullable WorldFold fold, Direction.Axis axis, int block) {
        return fold == null ? block : fold.blockDomain(axis).wrap(block);
    }

    public static float foldBlock(@Nullable WorldFold fold, Direction.Axis axis, float block) {
        return fold == null ? block : (float) fold.blockDomain(axis).wrap((double) block);
    }

    private RtfEntry() {
    }
}
