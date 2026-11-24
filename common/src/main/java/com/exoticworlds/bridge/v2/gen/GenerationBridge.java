package com.exoticworlds.bridge.v2.gen;

import com.exoticworlds.api.v2.gen.GenerationHooksV2;
import com.exoticworlds.bridge.v2.geometry.GeometryBridge;
import com.exoticworlds.bridge.v2.option.ShapeOptionsAdapter;
import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.gen.GenerationMoments;

public final class GenerationBridge {

    public static void atRandomState(String key, GenerationHooksV2.RandomStateHook hook) {
        GenerationMoments.atRandomState(key, (randomState, fold, seaLevel) ->
                hook.run(randomState, GeometryBridge.of(fold), ShapeOptionsAdapter.toV2(fold.getOptions()), seaLevel));
    }

    public static void atStructureStarts(String key, GenerationHooksV2.StructureStartsHook hook) {
        GenerationMoments.atStructureStarts(key, set ->
                hook.starts(new StructureStartsView(set)).stream()
                        .map(added -> new AddedStart(added.structure(), added.chunk()))
                        .toList());
    }

    private GenerationBridge() {
    }
}
