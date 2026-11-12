package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class CreateSchematicFold {
    public static AABB glueInScanFrame(@Nullable Level level, AABB scanBox, AABB glueBox) {
        return FoldedBoxQuery.toward(WorldLoopAttachments.wrappedFoldOfReader(level), scanBox.getCenter(),
                glueBox);
    }

    private CreateSchematicFold() {
    }
}
