package com.exoticworlds.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exoticworlds.accessors.LevelHolder;
import com.exoticworlds.accessors.TransformerSource;
import com.exoticworlds.compat.c2me.TransformerSourceBindable;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.rewrites.chunksystem.common.TheChunkSystem;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ChunkMap;

@Mixin(TheChunkSystem.class)
public class TheChunkSystemMixin implements TransformerSource {
    @Shadow
    @Final
    private ChunkMap tacs;

    @Shadow
    @Final
    private SchedulingManager schedulingManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void exotic_worlds$bindLockFoldSource(ChunkMap tacs, CallbackInfo ci) {
        ((TransformerSourceBindable) this.schedulingManager).exotic_worlds$bindTransformerSource(this);
    }

    @Override
    public @Nullable WorldFold exotic_worlds$wrappedTransformer() {
        return WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) (Object) this.tacs).exotic_worlds$level());
    }

    @Unique
    private static final Logger exotic_worlds$LOGGER = LogUtils.getLogger();

    // C2ME passes the throwable as a third slf4j argument, so the stack is swallowed; this prints it.
    @Inject(method = "handleTransactionException", at = @At("HEAD"))
    private void exotic_worlds$logTransactionFailure(
            ItemHolder<?, ?, ?, ?> holder,
            ItemStatus<?, ?, ?> nextStatus,
            boolean isUpgrade,
            Throwable throwable,
            CallbackInfoReturnable<?> cir) {
        exotic_worlds$LOGGER.warn("[ew/c2me-compat] chunk_transaction_failed chunk={} status={} upgrade={}",
                holder.getKey(), nextStatus, isUpgrade, throwable);
    }
}
