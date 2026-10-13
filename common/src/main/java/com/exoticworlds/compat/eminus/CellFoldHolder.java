package com.exoticworlds.compat.eminus;

public interface CellFoldHolder {
    CellFold toroidal$fold();

    void toroidal$setFold(CellFold fold);

    static CellFold foldOf(Object holder) {
        return holder instanceof CellFoldHolder folded ? folded.toroidal$fold() : CellFold.NONE;
    }

    static void setFold(Object holder, CellFold fold) {
        if (holder instanceof CellFoldHolder folded) {
            folded.toroidal$setFold(fold);
        }
    }
}
