package com.exoticworlds.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.PathCarry;
import com.exoticworlds.core.DeckTransformation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

@Mixin(Path.class)
public class PathMixin implements PathCarry {
    @Shadow
    @Final
    private List<Node> nodes;

    @Mutable
    @Shadow
    @Final
    private BlockPos target;

    @Override
    public void exotic_worlds$carry(DeckTransformation lap, int footprint) {
        for (Node node : this.nodes) {
            ((PathCarry) node).exotic_worlds$carry(lap, footprint);
        }

        this.target = lap.apply(this.target);
    }
}
