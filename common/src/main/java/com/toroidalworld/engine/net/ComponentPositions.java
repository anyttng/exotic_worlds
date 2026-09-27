package com.toroidalworld.engine.net;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.core.StartupRegistry;
import com.mojang.logging.LogUtils;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ComponentPositions {
    @FunctionalInterface
    public interface Mover {
        Object moved(Object stored, UnaryOperator<Object> seat);
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Mover WALK = (stored, seat) -> seat.apply(stored);

    private static final Identifier LODESTONE_TRACKER_ID = Identifier.withDefaultNamespace("lodestone_tracker");

    private static final StartupRegistry<Identifier, Mover> REGISTERED = new StartupRegistry<>("Component positions");

    private static volatile Map<DataComponentType<?>, Mover> named = Map.of();

    // mc/1.21: registers the plain position components of Create and Aeronautics.
    public static void register(Identifier componentId) {
        register(componentId, WALK);
    }

    // mc/1.21: registers Electro Energetics' node components with WireNodes.moved.
    public static void register(Identifier componentId, Mover mover) {
        REGISTERED.register(componentId, mover);
    }

    public static void declare(Set<Identifier> componentIds) {
        named = resolve(javaRows(), componentIds, BuiltInRegistries.DATA_COMPONENT_TYPE::getValue);
    }

    public static @Nullable Mover moverOf(DataComponentType<?> type) {
        return named.get(type);
    }

    static ItemStack canonical(ItemStack stack, TranslationContext context) {
        FoldedValue.Leaves leaves = FoldedValue.toServer(context);
        return seatedIn(named, stack, stored -> FoldedValue.walk(context, leaves, stored));
    }

    static Map<Identifier, Mover> javaRows() {
        Map<Identifier, Mover> rows = new LinkedHashMap<>();
        rows.put(LODESTONE_TRACKER_ID, WALK);
        rows.putAll(REGISTERED.entries());
        return rows;
    }

    static Map<DataComponentType<?>, Mover> resolve(Map<Identifier, Mover> javaRows, Set<Identifier> declared,
            Function<Identifier, @Nullable DataComponentType<?>> lookup) {
        Map<Identifier, Mover> rows = new LinkedHashMap<>(javaRows);
        for (Identifier id : declared) {
            if (rows.containsKey(id)) {
                LOGGER.warn("Component positions: a data file declares {}, which a registration already holds;"
                        + " the registration is kept", id);
            } else {
                rows.put(id, WALK);
            }
        }

        Map<DataComponentType<?>, Mover> resolved = new LinkedHashMap<>();
        rows.forEach((id, mover) -> {
            DataComponentType<?> type = lookup.apply(id);
            if (type == null) {
                LOGGER.warn("Component positions: no component type is registered as {}; its row is skipped", id);
            } else {
                resolved.put(type, mover);
            }
        });

        return Map.copyOf(resolved);
    }

    static ItemStack seatedIn(Map<DataComponentType<?>, Mover> rows, ItemStack stack, UnaryOperator<Object> seat) {
        ItemStack seated = stack;
        for (Map.Entry<DataComponentType<?>, Mover> row : rows.entrySet()) {
            Object stored = stack.getComponents().get(row.getKey());
            if (stored == null) {
                continue;
            }

            Object moved = row.getValue().moved(stored, seat);
            if (moved != stored) {
                if (seated == stack) {
                    seated = stack.copy();
                }

                set(seated, row.getKey(), moved);
            }
        }

        return seated;
    }

    @SuppressWarnings("unchecked")
    private static <T> void set(ItemStack stack, DataComponentType<T> type, Object value) {
        stack.set(type, (T) value);
    }

    private ComponentPositions() {
    }
}
