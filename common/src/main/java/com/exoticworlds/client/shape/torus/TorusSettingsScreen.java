package com.exoticworlds.client.shape.torus;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.client.shape.LoopSettingsScreen;
import com.exoticworlds.client.shape.LoopSizeControls;
import com.exoticworlds.api.v1.shape.LoopSpans;
import com.exoticworlds.shape.torus.TorusSettings;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class TorusSettingsScreen extends LoopSettingsScreen<TorusSettings> {
    private static final Component TITLE = Component.translatable("gui.exotic_worlds.toroidal_settings.title");

    public TorusSettingsScreen(Screen parent, TorusSettings current, Consumer<TorusSettings> onDone) {
        super(TITLE, parent, onChange -> LoopSizeControls.perAxis(current.chunkWidth(Direction.Axis.X),
                current.chunkWidth(Direction.Axis.Z),
                Math.floorMod(current.skewChunks(), current.chunkWidth(Direction.Axis.X)),
                current.netherScale(), current.endChunkWidth(), onChange),
                current.generationOptions(), TorusSettings.OFFERED_OPTIONS, onDone);
    }

    @Override
    protected TorusSettings build() {
        return new TorusSettings(
                LoopSpans.ofWidths(this.controls.effectiveSize(Direction.Axis.X),
                        this.controls.effectiveSize(Direction.Axis.Z)).withSkew(this.controls.effectiveSkew()),
                this.controls.netherScale(),
                LoopSpans.ofWidth(this.controls.effectiveEndSize()),
                this.committedOptions());
    }

    @Override
    protected OptionContext newOptionContext() {
        return new ScreenContext();
    }

    private final class ScreenContext extends OptionContext {
        @Override
        public @Nullable Integer loopChunkWidth() {
            Integer xChunkWidth = this.loopChunkWidth(Direction.Axis.X);
            Integer zChunkWidth = this.loopChunkWidth(Direction.Axis.Z);
            return xChunkWidth == null || zChunkWidth == null ? null : Math.min(xChunkWidth, zChunkWidth);
        }

        @Override
        public @Nullable Integer loopChunkWidth(Direction.Axis axis) {
            return TorusSettingsScreen.this.controls.effectiveSize(axis);
        }

        @Override
        public @Nullable LoopSpans loopSpans() {
            LoopSpans spans = super.loopSpans();
            Integer skewChunks = TorusSettingsScreen.this.controls.effectiveSkew();
            return spans == null || skewChunks == null ? null : spans.withSkew(skewChunks);
        }
    }
}
