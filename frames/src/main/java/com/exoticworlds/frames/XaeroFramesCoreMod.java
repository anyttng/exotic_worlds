package com.exoticworlds.frames;

import java.util.List;

import cpw.mods.modlauncher.api.ITransformer;
import net.neoforged.neoforgespi.coremod.ICoreMod;

public final class XaeroFramesCoreMod implements ICoreMod {
    @Override
    public Iterable<? extends ITransformer<?>> getTransformers() {
        return List.of(new XaeroFramesTransformer());
    }
}
