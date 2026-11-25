package com.exoticworlds.core.feature;

import java.util.List;

import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.gen.StructureStartSet;

public interface StructureStartsFeature extends GenerationFeature {
    List<AddedStart> startsIn(StructureStartSet set);
}
