package com.exoticworlds.compat.freeterraforged.preview;

import java.util.List;

public interface PreviewReadout {
    int exotic_worlds$centerX();

    int exotic_worlds$centerZ();

    int exotic_worlds$hoveredX();

    int exotic_worlds$hoveredZ();

    String exotic_worlds$hovered();

    void exotic_worlds$hover(String readout);

    String[] exotic_worlds$legend();

    List<MarkerBox> exotic_worlds$markers();
}
