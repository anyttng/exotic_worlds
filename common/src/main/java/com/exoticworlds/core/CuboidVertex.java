package com.exoticworlds.core;

import java.util.List;

public record CuboidVertex(CuboidFace.BoxVector point, List<CuboidFace.Side> faces) {
}
