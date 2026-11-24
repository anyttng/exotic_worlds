package com.exoticworlds.engine.level;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;
import com.exoticworlds.core.fold.WorldFolds;
import com.exoticworlds.core.fold.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

public final class BoundLevelFold {
    private @Nullable Level level;

    private @Nullable WorldFold fold;

    public void bind(@Nullable LevelHeightAccessor accessor) {
        this.level = accessor instanceof Level level ? level : null;
        this.fold = null;
    }

    public WorldFold getFold() {
        WorldFold fold = this.fold;
        if (fold != null) {
            return fold;
        }

        Level level = this.level;
        if (level == null) {
            return WorldFolds.NOOP;
        }

        fold = WorldLoopAttachments.getFold(level);
        this.fold = fold;
        return fold;
    }

    public @Nullable WorldFold getWrapped() {
        WorldFold fold = getFold();
        return fold.isWrapped() ? fold : null;
    }
}
