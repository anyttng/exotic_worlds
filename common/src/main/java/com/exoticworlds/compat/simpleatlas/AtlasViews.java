package com.exoticworlds.compat.simpleatlas;

import org.jspecify.annotations.Nullable;

public interface AtlasViews {
    @Nullable AtlasView toroidal$view();

    @Nullable AtlasView toroidal$singleWorld();
}
