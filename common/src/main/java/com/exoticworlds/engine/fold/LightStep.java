package com.exoticworlds.engine.fold;

import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class LightStep {
    public static FoldOrientation orientationOf(WorldFold fold, long steppedNode, long foldedNode) {
        if (steppedNode == foldedNode || fold.preservesLocalIndices()) {
            return FoldOrientation.IDENTITY;
        }

        return fold.foldOriented(BlockPos.of(steppedNode)).orientation();
    }

    public static Direction inTargetFrame(FoldOrientation orientation, Direction direction) {
        return orientation.applyToFace(direction);
    }

    public static BlockState inSourceFrame(FoldOrientation orientation, BlockState targetState) {
        return orientation.isIdentity() ? targetState : ChunkFrame.toReader(targetState, orientation);
    }

    private LightStep() {
    }
}
