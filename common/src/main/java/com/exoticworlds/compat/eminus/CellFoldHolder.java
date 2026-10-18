package com.exoticworlds.compat.eminus;

public interface CellFoldHolder {
    CellFold exotic_worlds$fold();

    void exotic_worlds$setFold(CellFold fold);

    static CellFold foldOf(Object holder) {
        return holder instanceof CellFoldHolder folded ? folded.exotic_worlds$fold() : CellFold.NONE;
    }

    static void setFold(Object holder, CellFold fold) {
        if (holder instanceof CellFoldHolder folded) {
            folded.exotic_worlds$setFold(fold);
        }
    }
}
