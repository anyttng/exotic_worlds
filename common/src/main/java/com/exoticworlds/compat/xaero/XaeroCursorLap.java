package com.exoticworlds.compat.xaero;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import xaero.map.gui.MapTileSelection;

public final class XaeroCursorLap {
    private int cursorLapX;
    private int cursorLapZ;
    private int selectionLapX;
    private int selectionLapZ;
    private @Nullable Object trackedSelection;
    private int selectionEndX;
    private int selectionEndZ;
    private boolean selectionEndPending;
    private ChunkPos selectionEnd = ChunkPos.ZERO;

    public BlockPos foldCursor(int rawX, int rawZ) {
        return foldCursor(XaeroWorldMapFold.browsedShape(), rawX, rawZ);
    }

    BlockPos foldCursor(@Nullable ToroidalShape shape, int rawX, int rawZ) {
        BlockPos folded = XaeroWorldMapFold.foldBlock(shape, rawX, rawZ);
        this.cursorLapX = rawX - folded.getX();
        this.cursorLapZ = rawZ - folded.getZ();
        return folded;
    }

    public int seatX(int x) {
        return x + this.cursorLapX;
    }

    public int seatZ(int z) {
        return z + this.cursorLapZ;
    }

    public ChunkPos selectionEnd(MapTileSelection selection, int cursorX, int cursorZ) {
        return selectionEnd(XaeroWorldMapFold.browsedShape(), selection, selection.getStartX(), selection.getStartZ(),
                cursorX, cursorZ);
    }

    ChunkPos selectionEnd(@Nullable ToroidalShape shape, Object selection, int startX, int startZ, int cursorX,
            int cursorZ) {
        if (this.selectionEndPending) {
            this.selectionEndPending = false;
            return this.selectionEnd;
        }

        int endX = SectionPos.blockToSectionCoord(cursorX);
        int endZ = SectionPos.blockToSectionCoord(cursorZ);
        boolean fresh = selection != this.trackedSelection;
        this.trackedSelection = selection;
        ChunkPos reference = fresh ? new ChunkPos(startX, startZ) : new ChunkPos(this.selectionEndX, this.selectionEndZ);
        ChunkPos unwrapped = XaeroWorldMapFold.nearestChunk(shape, reference, endX, endZ);
        this.selectionEndX = unwrapped.x();
        this.selectionEndZ = unwrapped.z();
        this.selectionLapX = this.cursorLapX - SectionPos.sectionToBlockCoord(unwrapped.x() - endX);
        this.selectionLapZ = this.cursorLapZ - SectionPos.sectionToBlockCoord(unwrapped.z() - endZ);
        this.selectionEnd = new ChunkPos(AxisCopies.ofChunks(shape, Direction.Axis.X).withinOneLap(startX, unwrapped.x()),
                AxisCopies.ofChunks(shape, Direction.Axis.Z).withinOneLap(startZ, unwrapped.z()));
        this.selectionEndPending = true;
        return this.selectionEnd;
    }

    public int seatSelectionX(int chunkX) {
        return chunkX + SectionPos.blockToSectionCoord(this.selectionLapX);
    }

    public int seatSelectionZ(int chunkZ) {
        return chunkZ + SectionPos.blockToSectionCoord(this.selectionLapZ);
    }
}
