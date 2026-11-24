package com.exoticworlds.compat.eminus;

import org.jspecify.annotations.Nullable;

import com.eminus.cell.cache.CellHandle;

public interface MeshServiceCopies {
    CopyBuilds<CellHandle> exotic_worlds$getCopies();

    boolean exotic_worlds$tryJoin(long key, @Nullable CellHandle held, int references, long request, float priority,
            boolean isFill);
}
