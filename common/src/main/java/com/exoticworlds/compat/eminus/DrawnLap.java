package com.exoticworlds.compat.eminus;

import java.util.ArrayList;
import java.util.List;

import com.eminus.cell.CellFrame;
import com.eminus.cell.CellKey;
import com.eminus.cell.DetailLevel;
import com.eminus.mesh.MeshSummary;
import com.eminus.render.tree.RenderList;

public final class DrawnLap {
    private static final double NO_MARGIN = 0.0;

    private RenderList walked = RenderList.EMPTY;
    private RenderList drawn = RenderList.EMPTY;
    private double eyeX = Double.NaN;
    private double eyeZ = Double.NaN;

    @SuppressWarnings("ReferenceEquality")
    public RenderList of(CellFold fold, RenderList list, double eyeX, double eyeZ) {
        if (!fold.repeats()) {
            return list;
        }

        if (list == this.walked && eyeX == this.eyeX && eyeZ == this.eyeZ) {
            return this.drawn;
        }

        List<MeshSummary> kept = new ArrayList<>(list.meshes().size());
        for (MeshSummary mesh : list.meshes()) {
            long key = mesh.key();
            int side = DetailLevel.blocksPerCell(CellKey.level(key));
            int minX = CellFrame.originXOf(key);
            int minZ = CellFrame.originZOf(key);
            if (fold.meetsLap(minX, minZ, minX + side, minZ + side, eyeX, eyeZ, NO_MARGIN)) {
                kept.add(mesh);
            }
        }

        this.walked = list;
        this.eyeX = eyeX;
        this.eyeZ = eyeZ;
        this.drawn = kept.size() == list.meshes().size()
                ? list
                : new RenderList(List.copyOf(kept), list.borders(), list.walkedWith());
        return this.drawn;
    }
}
