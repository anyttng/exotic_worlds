package com.exoticworlds.compat.eminus.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.eminus.cell.ColumnCoverage;
import com.eminus.cell.cache.CellHandle;
import com.eminus.mesh.CellMesh;
import com.eminus.mesh.MeshListener;
import com.eminus.mesh.MeshQueue;
import com.eminus.mesh.MeshService;
import com.eminus.mesh.MeshTask;
import com.exoticworlds.compat.eminus.CellFold;
import com.exoticworlds.compat.eminus.CellFoldHolder;
import com.exoticworlds.compat.eminus.CopyBuilds;
import com.exoticworlds.compat.eminus.EminusInjectionTargets;
import com.exoticworlds.compat.eminus.MeshKeys;
import com.exoticworlds.compat.eminus.MeshServiceCopies;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(MeshService.class)
public class MeshServiceMixin implements MeshServiceCopies {
    @Shadow
    @Final
    private ColumnCoverage coverage;

    @Unique
    private final CopyBuilds<CellHandle> toroidal$copies = new CopyBuilds<>();

    @Override
    public CopyBuilds<CellHandle> toroidal$copies() {
        return toroidal$copies;
    }

    @Inject(method = "request(JLcom/eminus/cell/cache/CellHandle;IJF)V", at = @At("HEAD"), cancellable = true)
    private void toroidal$joinAWaitingCopy(long key, @Nullable CellHandle held, int references, long request,
            float priority, CallbackInfo ci) {
        CellFold fold = CellFoldHolder.foldOf(coverage);
        if (!fold.repeats()) {
            return;
        }

        CopyBuilds.Member<CellHandle> member = new CopyBuilds.Member<>(key, request, priority, held, references);
        if (toroidal$copies.offer(fold.foldKey(key), member) == CopyBuilds.Offer.JOINED) {
            toroidal$release(held, references);
            ci.cancel();
        }
    }

    @WrapOperation(method = "take", at = @At(value = "INVOKE", target = EminusInjectionTargets.MESH_QUEUE_POLL))
    private @Nullable MeshTask toroidal$skipAbsorbedCopies(MeshQueue queue, Operation<@Nullable MeshTask> original) {
        MeshTask task = original.call(queue);
        while (task != null && toroidal$copies.skip(task.request())) {
            toroidal$release(task.held(), task.references());
            task = original.call(queue);
        }

        if (task != null) {
            toroidal$copies.taken(task.request());
        }

        return task;
    }

    @WrapOperation(method = "build", at = @At(value = "INVOKE", target = EminusInjectionTargets.MESH_LISTENER_MESHED))
    private void toroidal$meshEveryCopy(MeshListener listener, CellMesh mesh, long request,
            Operation<Void> original) {
        original.call(listener, mesh, request);
        for (CopyBuilds.Member<CellHandle> copy : toroidal$copies.delivered(request)) {
            listener.meshed(MeshKeys.withKey(mesh, copy.key()), copy.request());
        }
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void toroidal$forgetCopies(CallbackInfo ci) {
        toroidal$copies.clear();
    }

    @Unique
    private void toroidal$release(@Nullable CellHandle held, int references) {
        if (held != null && references > 0) {
            ((MeshService) (Object) this).release(held, references);
        }
    }
}
