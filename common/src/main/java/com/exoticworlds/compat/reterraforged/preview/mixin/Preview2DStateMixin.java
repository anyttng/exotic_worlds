package com.exoticworlds.compat.reterraforged.preview.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.preview.MarkerBox;
import com.exoticworlds.compat.preview.PreviewHandle;
import com.exoticworlds.compat.preview.PreviewReadout;
import com.exoticworlds.compat.preview.ShownShape;

import net.minecraft.client.gui.components.AbstractWidget;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetEditorPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.Preview2D;

@Mixin(value = Preview2D.class, remap = false)
public abstract class Preview2DStateMixin implements PreviewReadout, PreviewHandle {
    @Shadow
    @Final
    private PresetEditorPage page;

    @Shadow
    @Final
    private String[] legendValues;

    @Shadow
    private int centerX;

    @Shadow
    private int centerZ;

    @Shadow
    private int hoveredCoordX;

    @Shadow
    private int hoveredCoordZ;

    @Shadow
    private String hoveredCoords;

    @Unique
    private final List<MarkerBox> exotic_worlds$markers = new ArrayList<>();

    @Shadow
    private int getZoom() {
        throw new AssertionError();
    }

    @Override
    public PreviewReadout exotic_worlds$readout() {
        return this;
    }

    @Override
    public ShownShape exotic_worlds$page() {
        return (ShownShape) this.page;
    }

    @Override
    public AbstractWidget exotic_worlds$widget() {
        return (AbstractWidget) (Object) this;
    }

    @Override
    public int exotic_worlds$zoom() {
        return this.getZoom();
    }

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
