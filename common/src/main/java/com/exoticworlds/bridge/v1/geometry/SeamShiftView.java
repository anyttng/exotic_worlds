package com.exoticworlds.bridge.v1.geometry;

import com.exoticworlds.api.v1.SeamShift;
import com.exoticworlds.api.v1.ToroidalShape.Orientation;
import com.exoticworlds.core.fold.DeckTransformation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SeamShiftView implements SeamShift {
    private final DeckTransformation transformation;

    SeamShiftView(DeckTransformation transformation) {
        this.transformation = transformation;
    }

    @Override
    public boolean isIdentity() {
        return this.transformation.isIdentity();
    }

    @Override
    public Orientation orientation() {
        return ToroidalShapeView.orientationOf(this.transformation.getOrientation());
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
