package com.exoticworlds.shape.option;

import com.exoticworlds.core.feature.DimensionScope;

public record ScopedOption(DimensionOption<?> option, DimensionScope scope) {
}
