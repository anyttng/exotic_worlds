package com.exoticworlds.compat.freeterraforged.preview.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.exoticworlds.compat.preview.PreviewHandle;
import com.exoticworlds.compat.preview.PreviewReadout;
import com.exoticworlds.compat.preview.ShownShape;

import etcodehome.freeterraforged.client.gui.screen.presetconfig.IPreviewHandler;
import etcodehome.freeterraforged.client.gui.screen.presetconfig.Preview2D;
import etcodehome.freeterraforged.client.gui.screen.presetconfig.Preview3D;

import net.minecraft.client.gui.components.AbstractWidget;

@Mixin(value = {Preview2D.class, Preview3D.class}, remap = false)
public abstract class PreviewHandleMixin implements PreviewHandle {
    @Override
    public PreviewReadout exotic_worlds$readout() {
        return (PreviewReadout) (Object) ((IPreviewHandler) this).state();
    }

    @Override
    public ShownShape exotic_worlds$page() {
        return (ShownShape) ((IPreviewHandler) this).page();
    }

    @Override
    public AbstractWidget exotic_worlds$widget() {
        return ((IPreviewHandler) this).widget();
    }

    @Override
    public int exotic_worlds$zoom() {
        return ((IPreviewHandler) this).getZoom();
    }
}
