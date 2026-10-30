package com.exoticworlds.compat.create;

import org.jspecify.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedBoxQuery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class CreateSchematicFold {
    public static ItemStack anchoredNear(@Nullable Level level, BlockPos reference, ItemStack blueprint) {
        return anchoredNear(WorldLoopAttachments.wrappedFoldOfReader(level),
                AllDataComponents.SCHEMATIC_ANCHOR, reference, blueprint);
    }

    static ItemStack anchoredNear(@Nullable WorldFold fold, DataComponentType<BlockPos> anchorComponent,
            BlockPos reference, ItemStack blueprint) {
        BlockPos anchor = blueprint.get(anchorComponent);
        if (anchor == null) {
            return blueprint;
        }

        BlockPos folded = CreateSeamFold.nearest(fold, reference, anchor);
        if (folded.equals(anchor)) {
            return blueprint;
        }

        ItemStack rebased = blueprint.copy();
        rebased.set(anchorComponent, folded);
        return rebased;
    }

    public static BlockPos visitedInSchematicFrame(@Nullable Level level, ItemStack blueprint, BlockPos visited) {
        return visitedInSchematicFrame(WorldLoopAttachments.wrappedFoldOfReader(level),
                AllDataComponents.SCHEMATIC_ANCHOR, blueprint, visited);
    }

    static BlockPos visitedInSchematicFrame(@Nullable WorldFold fold,
            DataComponentType<BlockPos> anchorComponent, ItemStack blueprint, BlockPos visited) {
        BlockPos anchor = blueprint.get(anchorComponent);
        if (anchor == null) {
            return visited;
        }

        return CreateSeamFold.nearest(fold, anchor, visited);
    }

    public static AABB glueInScanFrame(@Nullable Level level, AABB scanBox, AABB glueBox) {
        return FoldedBoxQuery.toward(WorldLoopAttachments.wrappedFoldOfReader(level), scanBox.getCenter(),
                glueBox);
    }

    public static BlockPos scannedControllerNear(@Nullable Level level, BlockPos lastKnown, BlockPos controller) {
        return CreateSeamFold.nearestCopy(level, lastKnown, controller);
    }

    public static boolean regionExceedsWorld(@Nullable Level level, BlockPos first, BlockPos second) {
        return regionExceedsWorld(WorldLoopAttachments.wrappedFoldOfReader(level), first, second);
    }

    static boolean regionExceedsWorld(@Nullable WorldFold fold, BlockPos first, BlockPos second) {
        if (fold == null) {
            return false;
        }

        return fold.foldsOntoItself(BoundingBox.fromCorners(first, second));
    }

    private CreateSchematicFold() {
    }
}
