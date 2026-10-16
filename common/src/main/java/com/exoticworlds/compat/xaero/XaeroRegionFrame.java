package com.exoticworlds.compat.xaero;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.api.v1.ToroidalShape;
import com.exoticworlds.compat.AxisCopies;
import com.exoticworlds.compat.MapCopies;
import com.exoticworlds.compat.MapViewSpan;
import com.exoticworlds.compat.WorldCopies;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

public final class XaeroRegionFrame {
    private static final int NO_SLOT_SIZE = 0;

    private @Nullable ToroidalShape shape;
    private MapCopies mapCopies = MapCopies.REPEATED;
    private AxisCopies copiesX = AxisCopies.UNBOUNDED;
    private AxisCopies copiesZ = AxisCopies.UNBOUNDED;
    private int glueSlotSize = NO_SLOT_SIZE;
    private boolean glueable;
    private int drawnCopiesSlotSize = NO_SLOT_SIZE;
    private List<WorldCopies.Copy> drawnCopies = List.of();
    private int slotViewBlockX;
    private int slotViewBlockZ;
    private long @Nullable [] slotOrigins;

    public void begin() {
        this.shape = XaeroWorldMapFold.browsedShape();
        this.mapCopies = MapCopies.current();
        this.copiesX = AxisCopies.of(this.shape, Direction.Axis.X);
        this.copiesZ = AxisCopies.of(this.shape, Direction.Axis.Z);
        this.glueSlotSize = NO_SLOT_SIZE;
        this.drawnCopiesSlotSize = NO_SLOT_SIZE;
    }

    public @Nullable ToroidalShape shape() {
        return this.shape;
    }

    public MapCopies mapCopies() {
        return this.mapCopies;
    }

    public boolean glueableAt(int slotSize) {
        if (slotSize != this.glueSlotSize) {
            this.glueSlotSize = slotSize;
            this.glueable = XaeroWorldMapFold.glueableAt(this.shape, slotSize);
        }

        return this.glueable;
    }

    public List<WorldCopies.Copy> drawnCopies(int slotSize, double cameraX, double cameraZ, double scale) {
        if (slotSize != this.drawnCopiesSlotSize) {
            Window window = Minecraft.getInstance().getWindow();
            int[] spanX = MapViewSpan.of(cameraX, window.getWidth(), scale, slotSize);
            int[] spanZ = MapViewSpan.of(cameraZ, window.getHeight(), scale, slotSize);
            this.drawnCopiesSlotSize = slotSize;
            this.drawnCopies = XaeroWorldMapFold.drawnCopies(this.shape, spanX, spanZ, this.mapCopies);
        }

        return this.drawnCopies;
    }

    public void beginSlot(int viewBlockX, int viewBlockZ) {
        this.slotViewBlockX = viewBlockX;
        this.slotViewBlockZ = viewBlockZ;
        this.slotOrigins = null;
    }

    public int slotViewBlockX() {
        return this.slotViewBlockX;
    }

    public int slotViewBlockZ() {
        return this.slotViewBlockZ;
    }

    public boolean slotInsideWorld(int slotSize) {
        return !XaeroWorldMapFold.spanLeavesWorld(this.copiesX, this.slotViewBlockX, slotSize)
                && !XaeroWorldMapFold.spanLeavesWorld(this.copiesZ, this.slotViewBlockZ, slotSize);
    }

    public long[] slotOrigins(int slotSize) {
        long[] origins = this.slotOrigins;
        if (origins == null) {
            origins = XaeroWorldMapFold.canonicalSlotOrigins(this.shape, this.slotViewBlockX, this.slotViewBlockZ,
                    slotSize);
            this.slotOrigins = origins;
        }

        return origins;
    }
}
