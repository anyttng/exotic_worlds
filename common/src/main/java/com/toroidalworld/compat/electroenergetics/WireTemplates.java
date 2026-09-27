package com.toroidalworld.compat.electroenergetics;

import org.jspecify.annotations.Nullable;

import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.infrastructure.detached_nodes.DetachedNodeHelper;
import com.toroidalworld.core.WorldLoopAttachments;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class WireTemplates {
    public static BlockPos inCaptureBox(Level level, Vec3i origin, Vec3i size, Vec3i pos) {
        BlockPos block = pos instanceof BlockPos blockPos ? blockPos : new BlockPos(pos);
        if (DetachedNodeHelper.isDetached(block)) {
            return block;
        }

        BlockPos centre = new BlockPos(origin).offset(size.getX() / 2, size.getY() / 2, size.getZ() / 2);
        return WorldLoopAttachments.transformerOfReader(level).nearestCopy(centre, block);
    }

    public static InWorldNode inCaptureBox(Level level, Vec3i origin, Vec3i size, InWorldNode node) {
        return WireNodes.moved(node, pos -> inCaptureBox(level, origin, size, pos));
    }

    public static BlockPos placed(ServerLevelAccessor accessor, BlockPos pos) {
        ServerLevel level = WorldLoopAttachments.serverLevelOf(accessor);
        return level == null ? pos : WireSpan.block(level, pos);
    }

    public static Vec3 besideInSchematic(SchematicLevel schematic, Vec3 anchorEnd, Vec3 end) {
        ServerLevel level = WorldLoopAttachments.serverLevelOf(schematic);
        return level == null ? end : WireSpan.seat(level, anchorEnd, end);
    }

    public static Vec3 inSchematicFrame(SchematicLevel schematic, BlockPos anchor, Vec3 pos) {
        ServerLevel level = WorldLoopAttachments.serverLevelOf(schematic);
        return level == null
                ? pos
                : WireSpan.seat(level, Vec3.atCenterOf(anchor.offset(schematic.getBounds().getCenter())), pos);
    }

    public static @Nullable BlockPos canonical(SchematicLevel schematic, @Nullable BlockPos pos) {
        ServerLevel level = WorldLoopAttachments.serverLevelOf(schematic);
        return level == null || pos == null ? pos : WireSpan.block(level, pos);
    }

    private WireTemplates() {
    }
}
