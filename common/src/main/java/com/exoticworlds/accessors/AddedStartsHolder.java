package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.structure.AddedStructureStarts;

public interface AddedStartsHolder {
    @Nullable AddedStructureStarts exotic_worlds$getAddedStarts();

    void exotic_worlds$setAddedStarts(AddedStructureStarts starts);
}
