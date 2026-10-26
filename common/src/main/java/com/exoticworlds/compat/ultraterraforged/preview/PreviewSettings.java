package com.exoticworlds.compat.ultraterraforged.preview;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.compat.ultraterraforged.preview.mixin.EditorSessionAccessor;

import com.pandaismyname1.ultraterraforged.client.gui.createworld.PreviewSource;
import com.pandaismyname1.ultraterraforged.client.gui.createworld.TerrainState;

import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

public final class PreviewSettings {
    public static @Nullable WorldCreationContext of(PreviewSource source) {
        if (source instanceof TerrainState terrain) {
            return terrain.uiState().getSettings();
        }

        if (source instanceof EditorSessionAccessor session) {
            return session.exotic_worlds$screen().getSettings();
        }

        return null;
    }

    private PreviewSettings() {
    }
}
