package com.exoticworlds.compat.freeterraforged.preview.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.preview.MarkerBox;
import com.exoticworlds.compat.preview.PreviewReadout;

@Mixin(targets = "etcodehome.freeterraforged.client.gui.screen.presetconfig.PreviewState", remap = false)
public abstract class PreviewStateMixin implements PreviewReadout {
    @Shadow
    @Final
    String[] legendValues;

    @Shadow
    int centerX;

    @Shadow
    int centerZ;

    @Shadow
    int hoveredCoordX;

    @Shadow
    int hoveredCoordZ;

    @Shadow
    String hoveredCoords;

    @Unique
    private final List<MarkerBox> exotic_worlds$markers = new ArrayList<>();

    @Override
    public int exotic_worlds$centerX() {
        return this.centerX;
    }

    @Override
    public int exotic_worlds$centerZ() {
        return this.centerZ;
    }

    @Override
    public int exotic_worlds$hoveredX() {
        return this.hoveredCoordX;
    }

    @Override
    public int exotic_worlds$hoveredZ() {
        return this.hoveredCoordZ;
    }

    @Override
    public String exotic_worlds$hovered() {
        return this.hoveredCoords;
    }

    @Override
    public void exotic_worlds$hover(String readout) {
        this.hoveredCoords = readout;
    }

    @Override
    public String[] exotic_worlds$legend() {
        return this.legendValues;
    }

    @Override
    public List<MarkerBox> exotic_worlds$markers() {
        return this.exotic_worlds$markers;
    }
}
