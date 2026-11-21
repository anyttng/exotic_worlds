package com.exoticworlds.core;

import net.minecraft.core.Direction;

public record CuboidEdge(CuboidFace.Side first, Direction firstSide, CuboidFace.Side second, Direction secondSide,
        SeamTransform transform) {
}
