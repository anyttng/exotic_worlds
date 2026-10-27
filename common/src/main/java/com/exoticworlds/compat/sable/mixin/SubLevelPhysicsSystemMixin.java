package com.exoticworlds.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.exoticworlds.compat.sable.SableColliderCopies;
import com.exoticworlds.compat.sable.SableConstraintGraphHolder;
import com.exoticworlds.compat.sable.SablePoseFold;
import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

@Mixin(value = SubLevelPhysicsSystem.class, remap = false)
public class SubLevelPhysicsSystemMixin {
    @Shadow
    @Final
    private PhysicsPipeline pipeline;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private PhysicsChunkTicketManager ticketManager;

    @WrapMethod(method = "handleBlockChange")
    private void exotic_worlds$changeHeldColliderCopies(SectionPos sectionPos, LevelChunkSection section, int localX, int localY,
            int localZ, BlockState oldState, BlockState newState, Operation<Void> original) {
        original.call(sectionPos, section, localX, localY, localZ, oldState, newState);
        WorldFold fold = WorldLoopAttachments.wrappedFoldOf(this.level);
        if (fold == null) {
            return;
        }

        List<SectionPos> copies = SableColliderCopies.held(fold, sectionPos,
                ((PhysicsChunkTicketManagerAccessor) this.ticketManager).exotic_worlds$physicsChunks());
        for (SectionPos copy : copies) {
            original.call(copy, section, localX, localY, localZ, oldState, newState);
        }
    }

    @WrapOperation(
            method = "updatePose",
            at = @At(value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/physics/PhysicsPipeline;readPose(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/companion/math/Pose3d;)Ldev/ryanhcode/sable/companion/math/Pose3d;"))
    private Pose3d exotic_worlds$reseatReadback(PhysicsPipeline pipeline, ServerSubLevel subLevel, Pose3d dest, Operation<Pose3d> original) {
        Pose3d readback = original.call(pipeline, subLevel, dest);
        SablePoseFold.reseat((SubLevelPhysicsSystem) (Object) this, subLevel, readback);
        return readback;
    }

    @Inject(method = "onSubLevelRemoved", at = @At("HEAD"))
    private void exotic_worlds$dropConstraintEdges(SubLevel subLevel, SubLevelRemovalReason reason, CallbackInfo callback) {
        if (subLevel instanceof PhysicsPipelineBody body && this.pipeline instanceof SableConstraintGraphHolder holder) {
            holder.exotic_worlds$constraintGraph().dropBody(body);
        }
    }
}
