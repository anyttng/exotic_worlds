package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.AddedStructureStarts;

public interface AddedStartsHolder {
    @Nullable AddedStructureStarts exotic_worlds$addedStarts();

    void exotic_worlds$addedStarts(AddedStructureStarts starts);
}
