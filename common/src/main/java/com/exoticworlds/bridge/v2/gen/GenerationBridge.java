package com.exoticworlds.bridge.v2.gen;

import com.exoticworlds.api.v2.gen.GenerationHooks;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.core.AddedStart;
import com.exoticworlds.core.GenerationMoments;
import com.exoticworlds.shape.ShapeOptionsAdapter;

public final class GenerationBridge {

    public static void atRandomState(String key, GenerationHooks.RandomStateHook hook) {
        GenerationMoments.atRandomState(key, (randomState, fold, seaLevel) ->
                hook.run(randomState, GeometryBridge.of(fold), ShapeOptionsAdapter.shapeOptions(fold.options()), seaLevel));
    }

    public static void atStructureStarts(String key, GenerationHooks.StructureStartsHook hook) {
        GenerationMoments.atStructureStarts(key, set ->
                hook.starts(new StructureStartsView(set)).stream()
                        .map(added -> new AddedStart(added.structure(), added.chunk()))
                        .toList());
    }

    private GenerationBridge() {
    }
}
