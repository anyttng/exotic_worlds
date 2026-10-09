package com.exoticworlds.compat.distanthorizons;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;

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
        return matched ? sql.replace(PLAIN_DISTANCE, seamDistance(lattice)) : sql;
    }

    private static String seamDistance(DhLattice lattice) {
        ToroidalShape shape = lattice.shape();
        if (lattice.isSkewed()) {
            return latticeDistance(lattice);
        }

        return axisDistance(shape, Direction.Axis.X, CORNER_X + " - " + TARGET_X) + " + "
                + axisDistance(shape, Direction.Axis.Z, CORNER_Z + " - " + TARGET_Z);
    }

    private static String latticeDistance(DhLattice lattice) {
        ToroidalShape shape = lattice.shape();
        int widthZ = shape.widthBlocks(Direction.Axis.Z);
        String deltaX = "(" + CORNER_X + " - " + TARGET_X + ")";
        String deltaZ = "(" + CORNER_Z + " - " + TARGET_Z + ")";
        String lappedZ = lapped(deltaZ, widthZ);
        String rowBelow = "((" + deltaZ + " - " + lappedZ + ") / " + widthZ + ")";
        long reach = DhFold.rowsThatCanWin(lattice);
        List<String> rows = new ArrayList<>();
        for (long row = -reach; row <= 1 + reach; row++) {
            String shiftedX = deltaX + " - (" + rowBelow + " + " + row + ") * " + lattice.skewBlocks();
            rows.add("abs(" + lappedZ + " - " + row * widthZ + ") + "
                    + axisDistance(shape, Direction.Axis.X, shiftedX));
        }

        return "min(" + String.join(", ", rows) + ")";
    }

    private static String axisDistance(ToroidalShape shape, Direction.Axis axis, String delta) {
        if (!shape.loops(axis)) {
            return "abs(" + delta + ")";
        }

        int width = shape.widthBlocks(axis);
        String lapped = lapped(delta, width);
        return "min(" + lapped + ", " + width + " - " + lapped + ")";
    }

    private static String lapped(String delta, int width) {
        return "(((" + delta + ") % " + width + " + " + width + ") % " + width + ")";
    }

    private DhSeamSql() {
    }
}
