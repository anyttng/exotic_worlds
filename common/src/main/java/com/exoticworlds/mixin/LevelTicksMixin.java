package com.exoticworlds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exoticworlds.accessors.LevelBindable;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldFold.Folded;
import com.exoticworlds.engine.level.BoundLevelFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

@Mixin(LevelTicks.class)
public class LevelTicksMixin<T> implements LevelBindable {
    @Unique
    private final BoundLevelFold exotic_worlds$bound = new BoundLevelFold();

    @Override
    public void exotic_worlds$bindLevel(ServerLevel level) {
        this.exotic_worlds$bound.bind(level);
    }

    @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> exotic_worlds$fileTickAtPhysicalPos(ScheduledTick<T> tick) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null) {
            return tick;
        }

        BlockPos pos = tick.pos();
        if (!transformer.isOver(pos)) {
            return tick;
        }

        return new ScheduledTick<>(tick.type(), transformer.fold(pos), tick.triggerTick(), tick.priority(),
                tick.subTickOrder());
    }

    @ModifyVariable(method = "hasScheduledTick", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$queryScheduledAtPhysicalPos(BlockPos pos) {
        return exotic_worlds$foldQueried(pos);
    }

    @ModifyVariable(method = "willTickThisTick", at = @At("HEAD"), argsOnly = true)
    private BlockPos exotic_worlds$queryThisTickAtPhysicalPos(BlockPos pos) {
        return exotic_worlds$foldQueried(pos);
    }

    @Unique
    private BlockPos exotic_worlds$foldQueried(BlockPos pos) {
        return this.exotic_worlds$bound.fold().fold(pos);
    }

    @Inject(method = "clearArea", at = @At("HEAD"), cancellable = true)
    private void exotic_worlds$clearEachCopyOfTheArea(BoundingBox area, CallbackInfo ci) {
        WorldFold transformer = this.exotic_worlds$bound.wrapped();
        if (transformer == null || !transformer.crossesBounds(area)) {
            return;
        }

        ci.cancel();

        @SuppressWarnings("unchecked")
        LevelTicks<T> ticks = (LevelTicks<T>) (Object) this;
        for (Folded<BoundingBox> piece : transformer.split(area)) {
            ticks.clearArea(piece.value());
        }
    }
}
