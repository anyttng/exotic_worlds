package com.exoticworlds.accessors;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.engine.gen.AddedStructureStarts;

public interface AddedStartsHolder {
    @Nullable AddedStructureStarts toroidal$addedStarts();

    void toroidal$addedStarts(AddedStructureStarts starts);
}
