package com.exoticworlds.bridge.v1.gen;

import com.exoticworlds.api.v1.gen.TerrainSnapshot;
import com.exoticworlds.shape.feature.floatingcrumbs.TerrainMask;

record TerrainSnapshotView(TerrainMask mask) implements TerrainSnapshot {
}
