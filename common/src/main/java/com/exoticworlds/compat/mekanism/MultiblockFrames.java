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

public final class MultiblockFrames {
    public static void moveOntoAnchor(Structure absorbing, Structure absorbed) {
        MultiblockStructureFrame frame = (MultiblockStructureFrame) absorbing;
        BlockPos anchor = frame.toroidal$anchor();
        BlockPos absorbedAnchor = ((MultiblockStructureFrame) absorbed).toroidal$anchor();
        if (anchor == null || absorbedAnchor == null) {
            return;
        }

        DeckTransformation move = WorldLoopAttachments.transformerOfReader(frame.toroidal$level())
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

        WorldFold fold = WorldLoopAttachments.transformerOfReader(((MultiblockStructureFrame) structure).toroidal$level());
        ((MultiblockBoundsFrame) cuboid).toroidal$setFold(fold);
        BlockPos min = cuboid.getMinPos();
        DeckTransformation move = fold.nearestCopyTransformation(fold.fold(min), min);
        if (move.isIdentity()) {
            return cuboid;
        }

        BlockPos a = move.apply(min);
        BlockPos b = move.apply(cuboid.getMaxPos());
        cuboid.setMinPos(new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())));
        cuboid.setMaxPos(new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())));
        return cuboid;
    }

    public static BlockPos seatOnto(VoxelCuboid bounds, BlockPos pos) {
        BlockPos copy = ((MultiblockBoundsFrame) bounds).toroidal$fold().nearestCopy(bounds.getMinPos(), pos);
        return copy.equals(pos) ? pos : copy;
    }

    public static Object seatOnto(Object data, Object pos) {
        return pos instanceof BlockPos blockPos ? seatOnto(((MultiblockDataFrame) data).toroidal$bounds(), blockPos)
                : pos;
    }

    public static BlockPos fold(Structure structure, BlockPos pos) {
        return WorldLoopAttachments.transformerOfReader(((MultiblockStructureFrame) structure).toroidal$level())
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
        List<BlockPos> outside = new ArrayList<>(access.toroidal$outsideSet());
        access.toroidal$outsideSet().clear();
        for (BlockPos pos : outside) {
            access.toroidal$outsideSet().add(move.apply(pos));
        }

        BlockPos low = move.apply(at(axis, key, plane.getMinCol(), plane.getMinRow()));
        if (plane.hasFrame()) {
            BlockPos high = move.apply(at(axis, key, plane.getMaxCol(), plane.getMaxRow()));
            Structure.Axis horizontal = axis.horizontal();
            Structure.Axis vertical = axis.vertical();
            access.toroidal$setMinCol(Math.min(horizontal.getCoord(low), horizontal.getCoord(high)));
            access.toroidal$setMaxCol(Math.max(horizontal.getCoord(low), horizontal.getCoord(high)));
            access.toroidal$setMinRow(Math.min(vertical.getCoord(low), vertical.getCoord(high)));
            access.toroidal$setMaxRow(Math.max(vertical.getCoord(low), vertical.getCoord(high)));
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
