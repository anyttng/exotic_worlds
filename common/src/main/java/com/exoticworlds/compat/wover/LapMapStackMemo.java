package com.exoticworlds.compat.wover;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;

public final class LapMapStackMemo<T> {
    private volatile @Nullable LapMapStack<T> stack;

    public LapMapStack<T> ensureCovering(WorldFold fold, Function<WorldFold, LapMapStack<T>> build) {
        LapMapStack<T> stack = this.stack;
        if (stack != null && stack.isCovering(fold)) {
            return stack;
        }

        synchronized (this) {
            stack = this.stack;
            if (stack == null || !stack.isCovering(fold)) {
                stack = build.apply(fold);
                this.stack = stack;
            }

            return stack;
        }
    }
}
