package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.commands.SpreadPlayersCommand;

@Mixin(SpreadPlayersCommand.Position.class)
public interface SpreadPositionAccessor {
    @Accessor("x")
    double exotic_worlds$x();

    @Accessor("x")
    void exotic_worlds$setX(double x);

    @Accessor("z")
    double exotic_worlds$z();

    @Accessor("z")
    void exotic_worlds$setZ(double z);

    @Invoker("<init>")
    static SpreadPlayersCommand.Position exotic_worlds$create() {
        throw new AssertionError();
    }
}
