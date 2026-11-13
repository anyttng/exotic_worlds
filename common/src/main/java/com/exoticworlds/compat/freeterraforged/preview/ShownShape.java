package com.exoticworlds.compat.freeterraforged.preview;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.CarriedShape;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

public interface ShownShape {
    @Nullable CarriedShape exotic_worlds$shown();

    void exotic_worlds$show(WorldCreationContext settings);

    void exotic_worlds$reshow();
}
