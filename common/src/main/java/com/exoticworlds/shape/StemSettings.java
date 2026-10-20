package com.exoticworlds.shape;

import com.exoticworlds.api.v1.option.GenerationOptions;
import com.exoticworlds.api.v1.shape.LoopSpans;

public interface StemSettings {

    LoopSpans overworld();

    int netherScale();

    LoopSpans end();

    GenerationOptions generationOptions();
}
