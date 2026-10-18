package com.exoticworlds.compat.mekanism;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.mekanism.mixin.VoxelPlaneAccessor;
import com.exoticworlds.core.DeckTransformation;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.math.voxel.VoxelPlane;
import mekanism.common.lib.multiblock.Structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class MultiblockFrames {
    public static void moveOntoAnchor(Structure absorbing, Structure absorbed) {
        MultiblockStructureFrame frame = (MultiblockStructureFrame) absorbing;
        BlockPos anchor = frame.exotic_worlds$anchor();
        BlockPos absorbedAnchor = ((MultiblockStructureFrame) absorbed).exotic_worlds$anchor();
        if (anchor == null || absorbedAnchor == null) {
            return;
        }

        DeckTransformation move = WorldLoopAttachments.transformerOfReader(frame.exotic_worlds$level())
                .nearestCopyTransformation(anchor, absorbedAnchor);
        if (move.isIdentity()) {
            return;
        }

        for (Structure.Axis axis : Structure.Axis.values()) {
            movePlanes(absorbed.getMinorAxisMap(axis), axis, move);
            movePlanes(absorbed.getMajorAxisMap(axis), axis, move);
        }
    }

    public static @Nullable VoxelCuboid seatCanonicalMin(Structure structure, @Nullable VoxelCuboid cuboid) {
        if (cuboid == null) {
            return null;
        }

        WorldFold fold = WorldLoopAttachments.transformerOfReader(((MultiblockStructureFrame) structure).exotic_worlds$level());
        ((MultiblockBoundsFrame) cuboid).exotic_worlds$setFold(fold);
        BlockPos min = cuboid.getMinPos();
        DeckTransformation move = fold.nearestCopyTransformation(fold.fold(min), min);
        if (move.isIdentity()) {
            return cuboid;
        }

        BoundingBox moved = move.apply(BoundingBox.fromCorners(min, cuboid.getMaxPos()));
        cuboid.setMinPos(new BlockPos(moved.minX(), moved.minY(), moved.minZ()));
        cuboid.setMaxPos(new BlockPos(moved.maxX(), moved.maxY(), moved.maxZ()));
        return cuboid;
    }

    public static BlockPos seatOnto(VoxelCuboid bounds, BlockPos pos) {
        BlockPos copy = ((MultiblockBoundsFrame) bounds).exotic_worlds$fold().nearestCopy(bounds.getMinPos(), pos);
        return copy.equals(pos) ? pos : copy;
    }

    public static Object seatOnto(Object data, Object pos) {
        return pos instanceof BlockPos blockPos ? seatOnto(((MultiblockDataFrame) data).exotic_worlds$bounds(), blockPos)
                : pos;
    }

    public static BlockPos fold(Structure structure, BlockPos pos) {
        return WorldLoopAttachments.transformerOfReader(((MultiblockStructureFrame) structure).exotic_worlds$level())
                .fold(pos);
    }

    private static void movePlanes(Int2ObjectSortedMap<VoxelPlane> planes, Structure.Axis axis,
            DeckTransformation move) {
        if (planes.isEmpty()) {
            return;
        }

        Int2ObjectMap<VoxelPlane> moved = new Int2ObjectOpenHashMap<>(planes.size());
        for (Int2ObjectMap.Entry<VoxelPlane> entry : planes.int2ObjectEntrySet()) {
            VoxelPlane plane = entry.getValue();
            moved.put(movePlane(axis, entry.getIntKey(), plane, move), plane);
        }

        planes.clear();
        planes.putAll(moved);
    }

    private static int movePlane(Structure.Axis axis, int key, VoxelPlane plane, DeckTransformation move) {
        VoxelPlaneAccessor access = (VoxelPlaneAccessor) plane;
        List<BlockPos> outside = new ArrayList<>(access.exotic_worlds$outsideSet());
        access.exotic_worlds$outsideSet().clear();
        for (BlockPos pos : outside) {
            access.exotic_worlds$outsideSet().add(move.apply(pos));
        }

        BlockPos low = move.apply(at(axis, key, plane.getMinCol(), plane.getMinRow()));
        if (plane.hasFrame()) {
            BlockPos high = move.apply(at(axis, key, plane.getMaxCol(), plane.getMaxRow()));
            Structure.Axis horizontal = axis.horizontal();
            Structure.Axis vertical = axis.vertical();
            access.exotic_worlds$setMinCol(Math.min(horizontal.getCoord(low), horizontal.getCoord(high)));
            access.exotic_worlds$setMaxCol(Math.max(horizontal.getCoord(low), horizontal.getCoord(high)));
            access.exotic_worlds$setMinRow(Math.min(vertical.getCoord(low), vertical.getCoord(high)));
            access.exotic_worlds$setMaxRow(Math.max(vertical.getCoord(low), vertical.getCoord(high)));
        }

        return axis.getCoord(low);
    }

    private static BlockPos at(Structure.Axis axis, int key, int col, int row) {
        return switch (axis) {
            case X -> new BlockPos(key, row, col);
            case Y -> new BlockPos(col, key, row);
            case Z -> new BlockPos(col, row, key);
        };
    }

    private MultiblockFrames() {
    }
}
