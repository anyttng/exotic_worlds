package com.exoticworlds.compat.wover;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.fold.WorldFold;

public final class LapMapMemo<T> {
    private volatile @Nullable LapMap<T> lapMap;

    public @Nullable LapMap<T> getCovering(WorldFold fold) {
        LapMap<T> lapMap = this.lapMap;
        return lapMap != null && lapMap.isCovering(fold) ? lapMap : null;
    }

    public LapMap<T> keep(LapMap<T> lapMap) {
        this.lapMap = lapMap;
        return lapMap;
    }
}
