package com.exoticworlds.compat.aeronautics;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

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

    public static @Nullable Object enter(BlockEntity controller, BlockEntity partner) {
        Level level = controller.getLevel();
        WorldFold fold = WorldLoopAttachments.wrappedFoldOfReader(level);
        SubLevel own = Sable.HELPER.getContaining(controller);
        SubLevel other = Sable.HELPER.getContaining(partner);
        if (fold == null || own == null || other == null) {
            return SeamFrame.KEPT;
        }

        return SeamFrame.enter(level, positionOf(own), Function.identity());
    }

    private static Vec3 positionOf(SubLevel subLevel) {
        return JomlVectors.read(subLevel.logicalPose().position());
    }

    private MergingGlueSeamFrame() {
    }
}
