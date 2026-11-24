package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.FoldedCompileContext;

public interface FoldedCompileSource {
    @Nullable FoldedCompileContext exotic_worlds$getFoldedCompile();

    void exotic_worlds$setFoldedCompile(FoldedCompileContext context);
}
