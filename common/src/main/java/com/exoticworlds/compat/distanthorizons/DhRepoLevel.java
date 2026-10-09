package com.exoticworlds.compat.distanthorizons;

import org.jspecify.annotations.Nullable;

import com.seibel.distanthorizons.core.level.IDhLevel;

public interface DhRepoLevel {
    static @Nullable DhLattice latticeOf(Object repo) {
        return ((DhRepoLevel) repo).toroidal$lattice();
    }

    default void toroidal$bindLevel(IDhLevel level) {
    }

    default @Nullable DhLattice toroidal$lattice() {
        return null;
    }
}
