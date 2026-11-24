package com.exoticworlds.bridge.v1.gen;

import com.exoticworlds.api.v1.gen.GenerationHooks;
import com.exoticworlds.bridge.v1.geometry.GeometryBridge;
import com.exoticworlds.bridge.v1.option.OptionBridge;
import com.exoticworlds.core.gen.AddedStart;
import com.exoticworlds.core.gen.GenerationMoments;

public final class GenerationBridge {

    public static void atRandomState(String key, GenerationHooks.RandomStateHook hook) {
        GenerationMoments.atRandomState(key, (randomState, fold, seaLevel) ->
                hook.run(randomState, GeometryBridge.of(fold), OptionBridge.toV1(fold.getOptions()), seaLevel));
    }

    public static void atStructureStarts(String key, GenerationHooks.StructureStartsHook hook) {
        GenerationMoments.atStructureStarts(key, set ->
                hook.starts(new StructureStartsView(set, GeometryBridge.of(set.getFold()))).stream()
                        .map(added -> new AddedStart(added.structure(), added.chunk()))
                        .toList());
    }

    private GenerationBridge() {
    }
}
