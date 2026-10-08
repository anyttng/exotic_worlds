package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

public interface TransformerSource {
    @Nullable
    WorldFold toroidal$wrappedTransformer();
}
