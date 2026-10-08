package com.exoticworlds.compat.create.client;

import java.util.function.Supplier;

import com.exoticworlds.engine.seam.MapSurfaceCopies;
import com.exoticworlds.engine.seam.MapSurfaceCopies.Copies;

public final class TrainMapSurface {
    public static <T> T showing(Copies copies, Supplier<T> render) {
        Copies previous = MapSurfaceCopies.bind(copies);
        try {
            return render.get();
        } finally {
            MapSurfaceCopies.restore(previous);
        }
    }

    private TrainMapSurface() {
    }
}
