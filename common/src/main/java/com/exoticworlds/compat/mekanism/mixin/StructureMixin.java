package com.exoticworlds.compat.mekanism.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.compat.mekanism.MultiblockFrames;
import com.exoticworlds.compat.mekanism.MultiblockStructureFrame;

import mekanism.common.lib.multiblock.IMultiblockBase;
import mekanism.common.lib.multiblock.Structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = Structure.class, remap = false)
public class StructureMixin implements MultiblockStructureFrame {
    @Unique
    private @Nullable BlockPos exotic_worlds$anchor;

    @Unique
    private @Nullable Level exotic_worlds$level;

    @Override
    public @Nullable BlockPos exotic_worlds$anchor() {
        return this.exotic_worlds$anchor;
    }

    @Override
    public @Nullable Level exotic_worlds$level() {
        return this.exotic_worlds$level;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void exotic_worlds$recordAnchor(IMultiblockBase node, CallbackInfo ci) {
        this.exotic_worlds$anchor = node.getBlockPos();
        this.exotic_worlds$level = node.getLevel();
    }

    @Inject(method = "add", at = @At("HEAD"))
    private void exotic_worlds$moveOntoAnchor(Structure absorbed, CallbackInfo ci) {
        if (absorbed != (Object) this) {
            MultiblockFrames.moveOntoAnchor((Structure) (Object) this, absorbed);
        }
    }

    @ModifyVariable(method = "getTile", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$foldNodeKey(BlockPos pos) {
        return MultiblockFrames.fold((Structure) (Object) this, pos);
    }
}
