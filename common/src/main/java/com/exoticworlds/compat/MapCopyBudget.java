package com.exoticworlds.compat;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class MapCopyBudget {
    public static final int MAX_TILE_BLITS = 16_384;

    public static Copies painted(AxisCopies x, int rangeX, AxisCopies z, int rangeZ) {
        return new Copies(Math.max(rangeX, rangeZ), new BoundingBox(
                paintedMin(x, x.offset(-rangeX)), Integer.MIN_VALUE, paintedMin(z, z.offset(-rangeZ)),
                paintedMax(x, x.offset(rangeX)), Integer.MAX_VALUE, paintedMax(z, z.offset(rangeZ))));
    }

    public static Copies painted(@Nullable ToroidalShape shape, List<WorldCopies.Copy> copies) {
        AxisCopies x = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.Z);
        int minDx = 0;
        int maxDx = 0;
        int minDz = 0;
        int maxDz = 0;
        for (WorldCopies.Copy copy : copies) {
            minDx = Math.min(minDx, copy.dx());
            maxDx = Math.max(maxDx, copy.dx());
            minDz = Math.min(minDz, copy.dz());
            maxDz = Math.max(maxDz, copy.dz());
        }

        return new Copies(reach(shape, copies), new BoundingBox(
                paintedMin(x, minDx), Integer.MIN_VALUE, paintedMin(z, minDz),
                paintedMax(x, maxDx), Integer.MAX_VALUE, paintedMax(z, maxDz)));
    }

    public static int reach(@Nullable ToroidalShape shape, List<WorldCopies.Copy> copies) {
        AxisCopies x = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.Z);
        int reach = 0;
        for (WorldCopies.Copy copy : copies) {
            reach = Math.max(reach, Math.max(lapsSpanned(x, copy.dx()), lapsSpanned(z, copy.dz())));
        }

        return reach;
    }

    private static int lapsSpanned(AxisCopies copies, int shift) {
        return copies.loops() ? -Math.floorDiv(-Math.abs(shift), copies.width()) : 0;
    }

    private static int paintedMin(AxisCopies copies, int shift) {
        return copies.loops() ? copies.min() + shift : Integer.MIN_VALUE;
    }

    private static int paintedMax(AxisCopies copies, int shift) {
        return copies.loops() ? copies.max() + shift - 1 : Integer.MAX_VALUE;
    }

    private MapCopyBudget() {
    }
}
