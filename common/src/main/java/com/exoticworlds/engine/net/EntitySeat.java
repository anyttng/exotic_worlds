package com.exoticworlds.engine.net;

import com.exoticworlds.core.FoldOrientation;
import com.exoticworlds.engine.seam.ClientPosition;

import net.minecraft.world.phys.Vec3;

record EntitySeat(Vec3 position, FoldOrientation orientation) {
    static EntitySeat of(TranslationContext context, Vec3 serverPosition) {
        return at(context, context.toClient(serverPosition, context.trackedReach()));
    }

    static EntitySeat ofEntity(TranslationContext context, int entityId) {
        Vec3 serverPosition = context.entityPosition().apply(entityId);
        return serverPosition == null
                ? at(context, context.clientPosition().anchor(0.0))
                : of(context, serverPosition);
    }

    static FoldOrientation orientationOf(TranslationContext context, int entityId) {
        return context.fold().preservesLocalIndices()
                ? FoldOrientation.IDENTITY
                : ofEntity(context, entityId).orientation();
    }

    static FoldOrientation orientationAt(TranslationContext context, Vec3 clientPosition) {
        return ClientPosition.orientationAt(context.fold(), clientPosition);
    }

    private static EntitySeat at(TranslationContext context, Vec3 clientPosition) {
        return new EntitySeat(clientPosition, orientationAt(context, clientPosition));
    }
}
