package com.exoticworlds.compat.ultraterraforged.preview.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.PresetConfigScreen;

@Mixin(targets = "com.pandaismyname1.ultraterraforged.client.gui.screen.presetconfig.EditorSession", remap = false)
public interface EditorSessionAccessor {
    @Accessor("screen")
    PresetConfigScreen exotic_worlds$screen();
}
