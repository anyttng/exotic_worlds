package com.exoticworlds.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.exoticworlds.core.WorldFold;
import com.exoticworlds.core.WorldLoopAttachments;
import com.exoticworlds.engine.ThreadScope;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// Folded on read, since FactoryPanelBehaviour.at refuses a canonical position at its isLoaded gate before any lookup.
public final class CreateMenuFrame {
    private static final ThreadScope<Frame> PAYLOAD_FRAME = new ThreadScope<>();

    private record Frame(WorldFold transformer, BlockPos anchor) {
    }

    public static <T> T readingPayload(Supplier<T> read) {
        return PAYLOAD_FRAME.with(resolve(), read);
    }

    public static BlockPos fold(BlockPos canonical) {
        Frame frame = PAYLOAD_FRAME.current();
        if (frame == null) {
            return canonical;
        }

        return frame.transformer().nearestCopy(frame.anchor(), canonical);
    }

    private static @Nullable Frame resolve() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return null;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return transformer == null ? null : new Frame(transformer, player.blockPosition());
    }

    private CreateMenuFrame() {
    }
}
