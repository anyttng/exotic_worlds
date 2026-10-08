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
    private @Nullable BlockPos toroidal$anchor;

    @Unique
    private @Nullable Level toroidal$level;

    @Override
    public @Nullable BlockPos toroidal$anchor() {
        return this.toroidal$anchor;
    }

    @Override
    public @Nullable Level toroidal$level() {
        return this.toroidal$level;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void toroidal$recordAnchor(IMultiblockBase node, CallbackInfo ci) {
        this.toroidal$anchor = node.getBlockPos();
        this.toroidal$level = node.getLevel();
    }

    @Inject(method = "add", at = @At("HEAD"))
    private void toroidal$moveOntoAnchor(Structure absorbed, CallbackInfo ci) {
        if (absorbed != (Object) this) {
            MultiblockFrames.moveOntoAnchor((Structure) (Object) this, absorbed);
        }
    }

    @ModifyVariable(method = "getTile", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$foldNodeKey(BlockPos pos) {
        return MultiblockFrames.fold((Structure) (Object) this, pos);
    }
}
