package com.exoticworlds.compat.distanthorizons;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;

import net.minecraft.core.Direction;

public final class DhSeamSql {
    static final String PLAIN_DISTANCE =
            "abs((PosX << (6 + DetailLevel)) - ?) + abs((PosZ << (6 + DetailLevel)) - ?)";

    private static final String CORNER_X = "(PosX << (6 + DetailLevel))";
    private static final String CORNER_Z = "(PosZ << (6 + DetailLevel))";
    private static final String TARGET_X = "?1";
    private static final String TARGET_Z = "?2";

    public enum Site {
        UPDATE("update"),
        REGEN("regen"),
        REGEN_COUNT("regen_count");

        final String label;

        Site(String label) {
            this.label = label;
        }
    }

    public static String rewrite(@Nullable DhLattice lattice, String sql, Site site) {
        if (lattice == null) {
            return sql;
        }

        boolean matched = sql.contains(PLAIN_DISTANCE);
        DhProbes.seamSql(site, matched);
        return matched ? sql.replace(PLAIN_DISTANCE, lattice.seamDistance()) : sql;
    }

    static String seamDistance(WorldFold fold, int skewBlocks) {
        if (skewBlocks != 0) {
            return latticeDistance(fold, skewBlocks);
        }

        return axisDistance(fold, Direction.Axis.X, CORNER_X + " - " + TARGET_X) + " + "
                + axisDistance(fold, Direction.Axis.Z, CORNER_Z + " - " + TARGET_Z);
    }

    private static String latticeDistance(WorldFold fold, int skewBlocks) {
        int widthZ = fold.widthBlocks(Direction.Axis.Z);
        String deltaX = "(" + CORNER_X + " - " + TARGET_X + ")";
        String deltaZ = "(" + CORNER_Z + " - " + TARGET_Z + ")";
        String lappedZ = lapped(deltaZ, widthZ);
        String rowBelow = "((" + deltaZ + " - " + lappedZ + ") / " + widthZ + ")";
        long reach = DhFold.rowsThatCanWin(fold, skewBlocks);
        List<String> rows = new ArrayList<>();
        for (long row = -reach; row <= 1 + reach; row++) {
            String shiftedX = deltaX + " - (" + rowBelow + " + " + row + ") * " + skewBlocks;
            rows.add("abs(" + lappedZ + " - " + row * widthZ + ") + "
                    + axisDistance(fold, Direction.Axis.X, shiftedX));
        }

        return "min(" + String.join(", ", rows) + ")";
    }

    private static String axisDistance(WorldFold fold, Direction.Axis axis, String delta) {
        if (!fold.loops(axis)) {
            return "abs(" + delta + ")";
        }

        int width = fold.widthBlocks(axis);
        String lapped = lapped(delta, width);
        return "min(" + lapped + ", " + width + " - " + lapped + ")";
    }

    private static String lapped(String delta, int width) {
        return "(((" + delta + ") % " + width + " + " + width + ") % " + width + ")";
    }

    private DhSeamSql() {
    }
}
