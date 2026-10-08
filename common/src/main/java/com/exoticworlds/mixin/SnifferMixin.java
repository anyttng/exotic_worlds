package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exoticworlds.core.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

@Mixin(Sniffer.class)
public class SnifferMixin {
    @ModifyVariable(method = "canDig(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$canonicalDigCandidate(BlockPos position) {
        return WorldLoopAttachments.transformerOf(((Sniffer) (Object) this).level()).fold(position);
    }
}
