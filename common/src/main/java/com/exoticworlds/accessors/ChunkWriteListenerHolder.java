package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.feature.ChunkWriteListener;

public interface ChunkWriteListenerHolder {
    @Nullable ChunkWriteListener exotic_worlds$getWriteListener();

    void exotic_worlds$setWriteListener(ChunkWriteListener listener);
}
