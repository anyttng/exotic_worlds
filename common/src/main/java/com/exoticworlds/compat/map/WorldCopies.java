package com.exoticworlds.compat.map;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.RectangleCopies;
import com.exoticworlds.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public final class WorldCopies {
    public static final Copy IDENTITY = new Copy(0, 0);

    public static List<Copy> meeting(@Nullable WorldFold fold, int minX, int minZ, int maxX, int maxZ) {
        if (minX >= maxX || minZ >= maxZ) {
            return List.of();
        }

        if (fold == null) {
            return List.of(IDENTITY);
        }

        List<DeckTransformation> shifts = RectangleCopies.meeting(fold, new AABB(minX, 0, minZ, maxX, 1, maxZ));
        List<Copy> copies = new ArrayList<>(shifts.size());
        for (DeckTransformation shift : shifts) {
            if (!shift.orientation().isIdentity()) {
                throw new IllegalStateException("A map draws its copies unturned, got " + shift.orientation());
            }

            BlockPos moved = shift.apply(BlockPos.ZERO);
            copies.add(new Copy(moved.getX(), moved.getZ()));
        }

        return copies;
    }

    public static List<Piece> pieces(@Nullable WorldFold fold, int minX, int minZ, int maxX, int maxZ) {
        return Window.of(fold, minX, minZ, maxX, maxZ).pieces();
    }

    public static List<Edge> seams(@Nullable WorldFold fold, int minX, int minZ, int maxX, int maxZ) {
        if (fold == null) {
            return List.of();
        }

        AxisCopies x = AxisCopies.of(fold, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(fold, Direction.Axis.Z);
        List<Edge> edges = new ArrayList<>();
        for (Copy copy : meeting(fold, minX, minZ, maxX, maxZ)) {
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

    public record Window(@Nullable WorldFold fold, int minX, int minZ, int maxX, int maxZ, List<Copy> copies) {
        public static Window of(@Nullable WorldFold fold, int minX, int minZ, int maxX, int maxZ) {
            return new Window(fold, minX, minZ, maxX, maxZ, WorldCopies.meeting(fold, minX, minZ, maxX, maxZ));
        }

        public List<Copy> copiesFor(int minX, int minZ, int maxX, int maxZ) {
            if (minX >= this.minX && minZ >= this.minZ && maxX <= this.maxX && maxZ <= this.maxZ) {
                return this.copies;
            }

            return WorldCopies.meeting(this.fold, minX, minZ, maxX, maxZ);
        }

        public Window within(int minX, int minZ, int maxX, int maxZ) {
            return new Window(this.fold, minX, minZ, maxX, maxZ, copiesFor(minX, minZ, maxX, maxZ));
        }

        public List<Piece> pieces() {
            AxisCopies x = AxisCopies.of(this.fold, Direction.Axis.X);
            AxisCopies z = AxisCopies.of(this.fold, Direction.Axis.Z);
            List<Piece> pieces = new ArrayList<>();
            for (Copy copy : this.copies) {
                int pieceMinX = x.clipMin(this.minX - copy.dx());
                int pieceMaxX = x.clipMax(this.maxX - copy.dx());
                int pieceMinZ = z.clipMin(this.minZ - copy.dz());
                int pieceMaxZ = z.clipMax(this.maxZ - copy.dz());
                if (pieceMinX < pieceMaxX && pieceMinZ < pieceMaxZ) {
                    pieces.add(new Piece(copy, pieceMinX, pieceMinZ, pieceMaxX, pieceMaxZ));
                }
            }

            return pieces;
        }

        public List<Copy> meeting() {
            List<Copy> meeting = new ArrayList<>(this.copies.size());
            for (Piece piece : pieces()) {
                meeting.add(piece.copy());
            }

            return meeting;
        }

        public long[] canonicalCells(int cellBlocks) {
            LongLinkedOpenHashSet cells = new LongLinkedOpenHashSet();
            for (Piece piece : pieces()) {
                for (int x = Math.floorDiv(piece.minX(), cellBlocks); x <= Math.floorDiv(piece.maxX() - 1, cellBlocks); x++) {
                    for (int z = Math.floorDiv(piece.minZ(), cellBlocks); z <= Math.floorDiv(piece.maxZ() - 1, cellBlocks); z++) {
                        cells.add(ChunkPos.pack(x, z));
                    }
                }
            }

            return cells.toLongArray();
        }
    }

    private WorldCopies() {
    }
}
