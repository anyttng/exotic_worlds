package com.exoticworlds.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exoticworlds.accessors.FoldedCompileSource;
import com.exoticworlds.engine.noise.FoldedCompileContext;

@Mixin(targets = "net.minecraft.world.level.levelgen.RandomState$1")
public class RandomStateCompileContextMixin implements FoldedCompileSource {
    @Unique
    private @Nullable FoldedCompileContext exotic_worlds$foldedCompile;

    @Override
    public @Nullable FoldedCompileContext exotic_worlds$foldedCompile() {
        return this.exotic_worlds$foldedCompile;
    }

    @Override
    public void exotic_worlds$foldedCompile(FoldedCompileContext context) {
        this.exotic_worlds$foldedCompile = context;
    }
}
