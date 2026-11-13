package com.exoticworlds.compat.preview;

import net.minecraft.client.gui.components.AbstractWidget;

public interface PreviewHandle {
    PreviewReadout exotic_worlds$readout();

    ShownShape exotic_worlds$page();

    AbstractWidget exotic_worlds$widget();

    int exotic_worlds$zoom();
}
