package com.exoticworlds.frames;

import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;

public final class XaeroFramesProvider implements ClassProcessorProvider {
    @Override
    public void createProcessors(Context context, Collector collector) {
        collector.add(new XaeroFramesProcessor());
        collector.add(new XaeroFramesValveProcessor());
    }
}
