package com.exoticworlds.compat.wover;

import com.exoticworlds.core.WorldFold;

public interface LapMapHolder<T> {
    LapMap<T> toroidal$lapMap(WorldFold fold);
}
