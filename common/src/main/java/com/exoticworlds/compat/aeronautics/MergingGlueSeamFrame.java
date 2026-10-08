package com.exoticworlds.compat.aeronautics;

import com.exoticworlds.compat.sable.SeamFrame;
import com.exoticworlds.core.JomlVectors;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MergingGlueSeamFrame {

    public static void control(BlockEntity controller, BlockEntity partner, Runnable original) {
        Level level = controller.getLevel();
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOfReader(level);
        SubLevel own = Sable.HELPER.getContaining(controller);
        SubLevel other = Sable.HELPER.getContaining(partner);
        if (fold == null || own == null || other == null) {
            original.run();
            return;
        }

        Vec3 anchor = positionOf(own);
        SeamFrame.run(level, () -> anchor, original);
    }

    private static Vec3 positionOf(SubLevel subLevel) {
        return JomlVectors.read(subLevel.logicalPose().position());
    }

    private MergingGlueSeamFrame() {
    }
}
