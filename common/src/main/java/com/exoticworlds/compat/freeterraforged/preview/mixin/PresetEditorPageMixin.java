package com.exoticworlds.compat.freeterraforged.preview.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.compat.preview.CreationShape;
import com.exoticworlds.compat.preview.ShownShape;
import com.exoticworlds.core.CarriedShape;

import etcodehome.freeterraforged.client.gui.screen.presetconfig.PresetEditorPage;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

@Mixin(value = PresetEditorPage.class, remap = false)
public abstract class PresetEditorPageMixin implements ShownShape {
    @Unique
    private @Nullable WorldCreationContext exotic_worlds$settings;

    @Unique
    private @Nullable CarriedShape exotic_worlds$shown;

    @Override
    public @Nullable CarriedShape exotic_worlds$shown() {
        return this.exotic_worlds$shown;
    }

    @Override
    public void exotic_worlds$show(WorldCreationContext settings) {
        this.exotic_worlds$settings = settings;
        this.exotic_worlds$shown = CreationShape.carried(settings);
    }

    @Override
    public void exotic_worlds$reshow() {
        WorldCreationContext settings = this.exotic_worlds$settings;
        if (settings != null) {
            this.exotic_worlds$show(settings);
        }
    }
}
