package com.exoticworlds.bridge.v2.geometry;

import com.exoticworlds.api.v2.geometry.Orientation;
import com.exoticworlds.api.v2.geometry.SeamShift;
import com.exoticworlds.core.DeckTransformation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class GeometryShiftView implements SeamShift {
    private final DeckTransformation transformation;

    GeometryShiftView(DeckTransformation transformation) {
        this.transformation = transformation;
    }

    @Override
    public boolean isIdentity() {
        return this.transformation.isIdentity();
    }

    @Override
    public Orientation orientation() {
        return OrientationView.of(this.transformation.orientation());
    }

    @Override
    public Vec3 apply(Vec3 pos) {
        return this.transformation.apply(pos);
    }

    @Override
    public BlockPos apply(BlockPos pos) {
        return this.transformation.apply(pos);
    }

    @Override
    public ChunkPos apply(ChunkPos chunk) {
        return this.transformation.apply(chunk);
    }

    @Override
    public AABB apply(AABB box) {
        return this.transformation.apply(box);
    }
}
