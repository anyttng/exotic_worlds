package com.exoticworlds.compat.apprenticecodex;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

record BlockTarget(BlockPos hit, Direction hitFace, Vec3 hitLocation, BlockPos place, Direction placeFacing) {
    BlockTarget toServer(WorldFold fold) {
        DeckTransformation shift = fold.foldTransformation(Vec3.atCenterOf(this.hit));
        if (shift.isIdentity()) {
            return this;
        }

        FoldOrientation orientation = shift.orientation();
        return new BlockTarget(shift.apply(this.hit), orientation.applyToFace(this.hitFace),
                shift.apply(this.hitLocation), shift.apply(this.place), orientation.applyToFace(this.placeFacing));
    }
}
