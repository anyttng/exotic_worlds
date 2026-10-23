package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.noise.FoldedCompileContext;

public interface FoldedCompileSource {
    @Nullable FoldedCompileContext exotic_worlds$foldedCompile();

    void exotic_worlds$foldedCompile(FoldedCompileContext context);
}
