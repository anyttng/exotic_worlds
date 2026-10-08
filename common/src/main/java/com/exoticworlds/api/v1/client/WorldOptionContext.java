package com.exoticworlds.api.v1.client;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;

/**
 * All a {@link WorldOptionControl} may reach of the settings screen around it, so that no control holds the screen
 * itself. A shape's own settings screen implements this and hands it to {@link WorldOptionControls#createAll}.
 */
public interface WorldOptionContext {

    /** The screen the control lives on — what a sub-screen it opens returns to. */
    Screen parent();

    /**
     * The loop width the screen currently states, in chunks — the narrower axis where the two differ — or
     * {@code null} while it states none.
     */
    @Nullable Integer loopChunkWidth();

    /**
     * The loop width the screen currently states along {@code axis}, in chunks, or {@code null} while it states none.
     * A screen that states one width for both axes need not override it.
     */
    default @Nullable Integer loopChunkWidth(Direction.Axis axis) {
        return loopChunkWidth();
    }

    /**
     * The shape the screen currently states — which axes loop, over how many chunks, and the skew — or {@code null}
     * while it states no width. The default reads both axes as looping unskewed over
     * {@link #loopChunkWidth(Direction.Axis)}, so a screen whose shape leaves an axis unbounded or skews overrides it.
     */
    default @Nullable LoopSpans loopSpans() {
        Integer xChunkWidth = loopChunkWidth(Direction.Axis.X);
        Integer zChunkWidth = loopChunkWidth(Direction.Axis.Z);
        return xChunkWidth == null || zChunkWidth == null ? null : LoopSpans.ofWidths(xChunkWidth, zChunkWidth);
    }

    /** The options the screen was opened with. */
    GenerationOptions options();

    /** Tells the screen a control's value changed, so it can re-check whether Done is available. */
    void onChanged();

    /** Asks the screen to lay its widgets out again — for a control whose widget set depends on its own value. */
    void rebuild();
}
