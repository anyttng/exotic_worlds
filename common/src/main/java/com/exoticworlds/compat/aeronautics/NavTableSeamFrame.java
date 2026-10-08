package com.exoticworlds.compat.aeronautics;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.NearestCopy;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;

import net.minecraft.world.phys.Vec3;

public final class NavTableSeamFrame {
    public static Vec3 seatTarget(NavTableBlockEntity navTable, Vec3 target) {
        return NearestCopy.toward(WorldLoopAttachments.wrappedTransformerOfReader(navTable.getLevel()),
                navTable.getProjectedSelfPos(), target);
    }

    private NavTableSeamFrame() {
    }
}
