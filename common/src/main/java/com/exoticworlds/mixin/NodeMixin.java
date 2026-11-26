package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.exoticworlds.accessors.PathCarry;
import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.core.fold.SeamTransform;
import com.exoticworlds.engine.seam.NavigationCarry;

import net.minecraft.world.level.pathfinder.Node;

@Mixin(Node.class)
public class NodeMixin implements PathCarry {
    @Mutable
    @Shadow
    @Final
    public int x;

    @Shadow
    @Final
    public int y;

    @Mutable
    @Shadow
    @Final
    public int z;

    @Mutable
    @Shadow
    @Final
    private int hash;

    @Override
    public void exotic_worlds$carry(DeckTransformation lap, int footprint) {
        SeamTransform move = lap.blockTransform();
        int fromX = this.x;
        int fromZ = this.z;
        this.x = NavigationCarry.nodeX(move, fromX, fromZ, footprint);
        this.z = NavigationCarry.nodeZ(move, fromX, fromZ, footprint);
        this.hash = Node.createHash(this.x, this.y, this.z);
    }
}
