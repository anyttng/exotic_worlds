package com.exoticworlds.compat.eminus;

import org.jspecify.annotations.Nullable;

import com.eminus.cell.cache.CellHandle;

public interface MeshServiceCopies {
    CopyBuilds<CellHandle> exotic_worlds$copies();

    boolean exotic_worlds$joined(long key, @Nullable CellHandle held, int references, long request, float priority,
            boolean fills);
}
