package com.exoticworlds.compat.wover;

import com.exoticworlds.core.fold.WorldFold;

public interface LapMapHolder<T> {
    LapMap<T> exotic_worlds$lapMap(WorldFold fold);
}
