package com.exoticworlds.compat.distanthorizons;

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

    public static String rewrite(@Nullable ToroidalShape shape, String sql, Site site) {
        if (shape == null) {
            return sql;
        }

        boolean matched = sql.contains(PLAIN_DISTANCE);
        DhProbes.seamSql(site, matched);
        return matched ? sql.replace(PLAIN_DISTANCE, seamDistance(shape)) : sql;
    }

    private static String seamDistance(ToroidalShape shape) {
        return axisDistance(shape, Direction.Axis.X, CORNER_X, TARGET_X) + " + "
                + axisDistance(shape, Direction.Axis.Z, CORNER_Z, TARGET_Z);
    }

    private static String axisDistance(ToroidalShape shape, Direction.Axis axis, String corner, String target) {
        String delta = corner + " - " + target;
        if (!shape.loops(axis)) {
            return "abs(" + delta + ")";
        }

        int width = shape.widthBlocks(axis);
        String lapped = "(((" + delta + ") % " + width + " + " + width + ") % " + width + ")";
        return "min(" + lapped + ", " + width + " - " + lapped + ")";
    }

    private DhSeamSql() {
    }
}
