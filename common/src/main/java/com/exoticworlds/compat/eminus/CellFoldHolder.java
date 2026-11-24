package com.exoticworlds.compat.eminus;

public interface CellFoldHolder {
    CellFold exotic_worlds$getFold();

    void exotic_worlds$setFold(CellFold fold);

    static CellFold getFold(Object holder) {
        return holder instanceof CellFoldHolder folded ? folded.exotic_worlds$getFold() : CellFold.NONE;
    }

    static void setFold(Object holder, CellFold fold) {
        if (holder instanceof CellFoldHolder folded) {
            folded.exotic_worlds$setFold(fold);
        }
    }
}
