package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;

public interface FoldSource {
    @Nullable
    WorldFold exotic_worlds$getWrappedFold();
}
