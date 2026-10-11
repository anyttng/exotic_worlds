package com.exoticworlds.engine.level;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFolds;
import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;

public final class BoundLevelFold {
    private @Nullable ServerLevel level;

    private @Nullable WorldFold fold;

    public void bind(ServerLevel level) {
        this.level = level;
        this.fold = null;
    }

    public WorldFold fold() {
        WorldFold fold = this.fold;
        if (fold != null) {
            return fold;
        }

        ServerLevel level = this.level;
        if (level == null) {
            return WorldFolds.NOOP;
        }

        fold = WorldLoopAttachments.transformerOf(level);
        this.fold = fold;
        return fold;
    }

    public @Nullable WorldFold wrapped() {
        WorldFold fold = fold();
        return fold.isWrapped() ? fold : null;
    }
}
