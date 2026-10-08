package com.exoticworlds.compat;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.SeamShift;
import com.exoticworlds.api.v1.ToroidalShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class WorldCopies {
    public static final Copy IDENTITY = new Copy(0, 0);

    public static List<Copy> meeting(@Nullable ToroidalShape shape, int minX, int minZ, int maxX, int maxZ) {
        if (minX >= maxX || minZ >= maxZ) {
            return List.of();
        }

        if (shape == null) {
            return List.of(IDENTITY);
        }

        List<SeamShift> shifts = shape.copiesMeeting(new AABB(minX, 0, minZ, maxX, 1, maxZ));
        List<Copy> copies = new ArrayList<>(shifts.size());
        for (SeamShift shift : shifts) {
            if (!shift.orientation().equals(ToroidalShape.Orientation.IDENTITY)) {
                throw new IllegalStateException("A map draws its copies unturned, got " + shift.orientation());
            }

            BlockPos moved = shift.apply(BlockPos.ZERO);
            copies.add(new Copy(moved.getX(), moved.getZ()));
        }

        return copies;
    }

    public static List<Piece> pieces(@Nullable ToroidalShape shape, int minX, int minZ, int maxX, int maxZ) {
        AxisCopies x = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, Direction.Axis.Z);
        List<Piece> pieces = new ArrayList<>();
        for (Copy copy : meeting(shape, minX, minZ, maxX, maxZ)) {
            int pieceMinX = x.clipMin(minX - copy.dx());
            int pieceMaxX = x.clipMax(maxX - copy.dx());
            int pieceMinZ = z.clipMin(minZ - copy.dz());
            int pieceMaxZ = z.clipMax(maxZ - copy.dz());
            if (pieceMinX < pieceMaxX && pieceMinZ < pieceMaxZ) {
                pieces.add(new Piece(copy, pieceMinX, pieceMinZ, pieceMaxX, pieceMaxZ));
            }
        }

        return pieces;
    }

    public static List<Edge> seams(@Nullable ToroidalShape shape, int minX, int minZ, int maxX, int maxZ) {
        if (shape == null) {
            return List.of();
        }

        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        List<Edge> edges = new ArrayList<>();
        for (Copy copy : meeting(shape, minX, minZ, maxX, maxZ)) {
            int fromX = x.loops() ? Math.max(minX, x.min() + copy.dx()) : minX;
            int toX = x.loops() ? Math.min(maxX, x.max() + copy.dx()) : maxX;
            int fromZ = z.loops() ? Math.max(minZ, z.min() + copy.dz()) : minZ;
            int toZ = z.loops() ? Math.min(maxZ, z.max() + copy.dz()) : maxZ;
            if (x.loops() && x.min() + copy.dx() >= minX) {
                edges.add(new Edge(x.min() + copy.dx(), fromZ, x.min() + copy.dx(), toZ));
            }

            if (z.loops() && z.min() + copy.dz() >= minZ) {
                edges.add(new Edge(fromX, z.min() + copy.dz(), toX, z.min() + copy.dz()));
            }
        }

        return edges;
    }

    public record Copy(int dx, int dz) {
        public boolean isIdentity() {
            return this.dx == 0 && this.dz == 0;
        }
    }

    public record Piece(Copy copy, int minX, int minZ, int maxX, int maxZ) {
    }

    public record Edge(int fromX, int fromZ, int toX, int toZ) {
    }

    private WorldCopies() {
    }
}
