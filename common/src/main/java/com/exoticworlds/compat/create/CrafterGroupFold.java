package com.exoticworlds.compat.create;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.zurrtum.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.exoticworlds.compat.create.mixin.ConnectedInputAccessor;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.fold.FoldedCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public final class CrafterGroupFold {
    public static BlockPos foldStoredDelta(Level level, BlockPos owner, BlockPos rawDelta) {
        return foldStoredDelta(WorldLoopAttachments.wrappedFoldOfReader(level), owner, rawDelta);
    }

    static BlockPos foldStoredDelta(@Nullable WorldFold fold, BlockPos owner, BlockPos rawDelta) {
        return CreateSeamFold.delta(fold, owner, owner.offset(rawDelta), rawDelta);
    }

    public static void normalizeGroup(ServerLevel level, MechanicalCrafterBlockEntity controller) {
        normalizeOwn(level, controller);
        ConnectedInputAccessor input = (ConnectedInputAccessor) controller.getInput();
        if (!input.exotic_worlds$isController()) {
            return;
        }

        BlockPos controllerPos = controller.getBlockPos();
        for (BlockPos delta : List.copyOf(input.exotic_worlds$data())) {
            if (delta.equals(BlockPos.ZERO)) {
                continue;
            }

            BlockPos memberPos = CreateSeamFold.canonical(level, controllerPos.offset(delta));
            if (level.getBlockEntity(memberPos) instanceof MechanicalCrafterBlockEntity member) {
                normalizeOwn(level, member);
            }
        }
    }

    public static void normalizeOwn(ServerLevel level, MechanicalCrafterBlockEntity crafter) {
        List<BlockPos> data = ((ConnectedInputAccessor) crafter.getInput()).exotic_worlds$data();
        BlockPos owner = crafter.getBlockPos();
        for (int index = 0; index < data.size(); index++) {
            BlockPos raw = data.get(index);
            BlockPos folded = foldStoredDelta(level, owner, raw);
            if (!folded.equals(raw)) {
                data.set(index, folded);
            }
        }
    }

    public static Set<BlockPos> canonicalMembers(@Nullable BlockGetter level, Set<BlockPos> raw) {
        WorldFold fold = level instanceof ServerLevel server ? WorldLoopAttachments.wrappedFoldOfReader(server) : null;
        return fold == null ? raw : FoldedCopies.of(raw, fold::fold);
    }

    private CrafterGroupFold() {
    }
}
