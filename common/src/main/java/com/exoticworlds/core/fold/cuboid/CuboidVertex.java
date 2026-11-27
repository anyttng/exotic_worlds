package com.exoticworlds.core.fold.cuboid;

import java.util.List;

public record CuboidVertex(CuboidFace.BoxVector point, List<CuboidFace.Side> faces) {
}
