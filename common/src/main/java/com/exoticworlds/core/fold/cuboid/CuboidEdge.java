package com.exoticworlds.core.fold.cuboid;

import com.exoticworlds.core.fold.SeamTransform;

import net.minecraft.core.Direction;

public record CuboidEdge(CuboidFace.Side first, Direction firstSide, CuboidFace.Side second, Direction secondSide,
        SeamTransform transform) {
}
