package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.seibel.distanthorizons.core.level.IDhLevel;

public interface DhRepoLevel {
    static @Nullable DhLattice latticeOf(Object repo) {
        return ((DhRepoLevel) repo).exotic_worlds$lattice();
    }

    default void exotic_worlds$bindLevel(IDhLevel level) {
    }

    default @Nullable DhLattice exotic_worlds$lattice() {
        return null;
    }
}
