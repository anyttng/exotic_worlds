package com.toroidalworld.compat.electroenergetics;

import java.util.ArrayList;
import java.util.List;

import com.george_vi.electroenergetics.content.railway_electrification.catenary.CatenaryConnection;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.world.level.LevelReader;

public final class CatenaryCopies {
    public static boolean parted(LevelReader level, CatenaryConnection line) {
        return WireCopies.parted(WorldLoopAttachments.transformerOfReader(level), line.pos1(), line.pos2());
    }

    public static CatenaryConnection from(LevelReader level, CatenaryConnection line, int end) {
        WorldFold fold = WorldLoopAttachments.transformerOfReader(level);
        return WireCopies.parted(fold, line.pos1(), line.pos2()) ? seated(fold, line, end) : line;
    }

    public static List<CatenaryConnection> fromEachEnd(LevelReader level, List<CatenaryConnection> lines) {
        WorldFold fold = WorldLoopAttachments.transformerOfReader(level);
        List<CatenaryConnection> copies = null;
        for (int i = 0; i < lines.size(); i++) {
            CatenaryConnection line = lines.get(i);
            if (WireCopies.parted(fold, line.pos1(), line.pos2())) {
                copies = copies == null ? new ArrayList<>(lines) : copies;
                copies.set(i, seated(fold, line, WireCopies.FIRST_END));
                copies.add(seated(fold, line, WireCopies.SECOND_END));
            }
        }

        return copies == null ? lines : copies;
    }

    public static List<CatenaryConnection> inOnePiece(LevelReader level, List<CatenaryConnection> lines) {
        WorldFold fold = WorldLoopAttachments.transformerOfReader(level);
        List<CatenaryConnection> pieces = null;
        for (int i = 0; i < lines.size(); i++) {
            CatenaryConnection line = lines.get(i);
            if (WireCopies.parted(fold, line.pos1(), line.pos2())) {
                pieces = pieces == null ? new ArrayList<>(lines) : pieces;
                pieces.set(i, seated(fold, line, WireCopies.FIRST_END));
            }
        }

        return pieces == null ? lines : pieces;
    }

    private static CatenaryConnection seated(WorldFold fold, CatenaryConnection line, int end) {
        WireCopies.BlockSpan span = WireCopies.from(fold, line.pos1(), line.pos2(), end);
        return new CatenaryConnection(span.pos1(), span.pos2());
    }

    private CatenaryCopies() {
    }
}
