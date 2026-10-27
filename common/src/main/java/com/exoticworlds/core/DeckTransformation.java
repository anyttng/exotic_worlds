package com.exoticworlds.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record DeckTransformation(SeamTransform blockTransform) {
    public static final DeckTransformation IDENTITY = new DeckTransformation(SeamTransform.IDENTITY);

    public boolean isIdentity() {
        return this.blockTransform.isIdentity();
    }

    public FoldOrientation orientation() {
        return this.blockTransform.orientation();
    }

    // mc/1.21: calls the inverse, unused on main.
    public DeckTransformation inverse() {
        if (isIdentity()) {
            return this;
        }

        return new DeckTransformation(this.blockTransform.inverse());
    }

    public Vec3 apply(Vec3 pos) {
        return this.blockTransform.apply(pos);
    }

    public BlockPos apply(BlockPos pos) {
        return this.blockTransform.apply(pos);
    }

    public ChunkPos apply(ChunkPos chunk) {
        if (isIdentity()) {
            return chunk;
        }

        return new ChunkPos(
                SectionPos.blockToSectionCoord(this.blockTransform.applyCellX(chunk.getMinBlockX())),
                SectionPos.blockToSectionCoord(this.blockTransform.applyCellZ(chunk.getMinBlockZ())));
    }

    public AABB apply(AABB box) {
        return this.blockTransform.apply(box);
    }

    public BoundingBox apply(BoundingBox box) {
        return this.blockTransform.apply(box);
    }
}
