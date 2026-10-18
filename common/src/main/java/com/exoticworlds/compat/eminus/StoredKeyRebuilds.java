package com.exoticworlds.compat.eminus;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.eminus.cell.DetailLevel;
import com.eminus.render.tree.NodeTable;
import com.eminus.render.tree.TreeNode;

import net.minecraft.core.SectionPos;

public final class StoredKeyRebuilds {
    private StoredKeyRebuilds() {
    }

    public static @Nullable TreeNode firstNodeRebuildingTheRest(CellFold fold, NodeTable table, long key,
            Consumer<TreeNode> rebuild) {
        List<TreeNode> folded = ((StoredKeyIndex) (Object) table).exotic_worlds$nodesAt(fold.foldKey(key));
        for (int index = 1; index < folded.size(); index++) {
            rebuild.accept(folded.get(index));
        }

        return folded.isEmpty() ? null : folded.getFirst();
    }

    public static void coverLapCopies(CellFold fold, int chunkX, int chunkZ, ChunkCover cover) {
        for (int copy = 1; copy < fold.copies(DetailLevel.MAX); copy++) {
            cover.cover(chunkX + SectionPos.blockToSectionCoord(fold.copyOffsetX(DetailLevel.MAX, copy)),
                    chunkZ + SectionPos.blockToSectionCoord(fold.copyOffsetZ(DetailLevel.MAX, copy)));
        }
    }

    @FunctionalInterface
    public interface ChunkCover {
        void cover(int chunkX, int chunkZ);
    }
}
