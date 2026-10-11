package com.exoticworlds.engine.net;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.core.DeckTransformation;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RecordPayloadFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<Type> POSITION_TYPES = Set.of(BlockPos.class, Vec3.class, ChunkPos.class,
            SectionPos.class, GlobalPos.class, AABB.class, Vector3d.class, Vector3dc.class);

    private static final Set<Type> CONTAINER_TYPES = Set.of(Optional.class, List.class);

    private static final MethodType ACCESSOR_TYPE = MethodType.methodType(Object.class, Object.class);

    private static final MethodType CONSTRUCTOR_TYPE = MethodType.methodType(Object.class, Object[].class);

    private static final ClassValue<Form> FORMS = new ClassValue<>() {
        @Override
        protected Form computeValue(Class<?> type) {
            return Form.of(type);
        }
    };

    private static volatile Rows rows = new Rows(Set.of(), Map.of());

    static void declare(Set<Identifier> deny, Map<Identifier, Map<String, Role>> payloads) {
        rows = new Rows(Set.copyOf(deny), Map.copyOf(payloads));
    }

    static CustomPacketPayload toClient(CustomPacketPayload payload, TranslationContext context) {
        return folded(payload, context, FoldedValue::toClient);
    }

    static CustomPacketPayload toServer(CustomPacketPayload payload, TranslationContext context) {
        return folded(payload, context, FoldedValue::toServer);
    }

    static Form formOf(Class<?> payloadType) {
        return FORMS.get(payloadType);
    }

    private static CustomPacketPayload folded(CustomPacketPayload payload, TranslationContext context,
            Function<TranslationContext, FoldedValue.Leaves> leavesOf) {
        Form form = FORMS.get(payload.getClass());
        Rows current = rows;
        if (!form.carriesPositions() || current.deny().contains(payload.type().id())) {
            return payload;
        }

        FoldedValue.Leaves leaves = leavesOf.apply(context);
        UnaryOperator<Object> fold = value -> FoldedValue.walk(context, leaves, value);
        Role @Nullable [] roles = current.rolesOf(payload, form);
        if (roles == null) {
            return form.rebuilt(payload, fold);
        }

        return form.rebuiltFrom(payload, values -> foldByRoles(values, roles, fold, context.dimension(), leaves));
    }

    private static void foldByRoles(@Nullable Object[] values, Role[] roles, UnaryOperator<Object> fold,
            ResourceKey<Level> dimension, FoldedValue.Leaves leaves) {
        DeckTransformation groupMove = null;
        for (int index = 0; index < values.length; index++) {
            Object value = values[index];
            if (value == null) {
                continue;
            }

            switch (roles[index]) {
                case WORLD -> values[index] = fold.apply(value);
                case UNTOUCHED -> {
                }
                case GROUP -> {
                    if (groupMove == null) {
                        Vec3 anchor = FoldedValue.anchorOf(dimension, value);
                        if (anchor == null) {
                            continue;
                        }

                        groupMove = leaves.deck().apply(anchor);
                    }

                    values[index] = FoldedValue.moved(dimension, groupMove, value);
                }
            }
        }
    }

    private static boolean carriesPosition(Type type, Set<Class<?>> visited) {
        if (type instanceof ParameterizedType parameterized && CONTAINER_TYPES.contains(parameterized.getRawType())) {
            return carriesPosition(parameterized.getActualTypeArguments()[0], visited);
        }

        if (POSITION_TYPES.contains(type)) {
            return true;
        }

        if (!(type instanceof Class<?> nested) || !nested.isRecord() || !visited.add(nested)) {
            return false;
        }

        for (RecordComponent component : nested.getRecordComponents()) {
            if (carriesPosition(component.getGenericType(), visited)) {
                return true;
            }
        }

        return false;
    }

    public enum Role implements StringRepresentable {
        WORLD,
        UNTOUCHED,
        GROUP;

        public static final Codec<Role> CODEC = StringRepresentable.fromEnum(Role::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private record Rows(Set<Identifier> deny, Map<Identifier, Map<String, Role>> payloads,
            Map<Identifier, Role[]> resolved) {
        Rows(Set<Identifier> deny, Map<Identifier, Map<String, Role>> payloads) {
            this(deny, payloads, new ConcurrentHashMap<>());
        }

        Role @Nullable [] rolesOf(CustomPacketPayload payload, Form form) {
            Identifier payloadId = payload.type().id();
            Map<String, Role> row = payloads.get(payloadId);
            if (row == null) {
                return null;
            }

            return resolved.computeIfAbsent(payloadId, id -> form.rolesOf(row, id, payload.getClass()));
        }
    }

    static final class Form {
        private static final Form POSITIONLESS = new Form(null, new MethodHandle[0], new String[0], new boolean[0]);

        private final @Nullable MethodHandle constructor;

        private final MethodHandle[] accessors;

        private final String[] names;

        private final boolean[] positions;

        private final AtomicBoolean refusalLogged = new AtomicBoolean();

        private Form(@Nullable MethodHandle constructor, MethodHandle[] accessors, String[] names,
                boolean[] positions) {
            this.constructor = constructor;
            this.accessors = accessors;
            this.names = names;
            this.positions = positions;
        }

        boolean carriesPositions() {
            return constructor != null;
        }

        private Role[] rolesOf(Map<String, Role> row, Identifier payloadId, Class<?> type) {
            Role[] roles = new Role[names.length];
            List<String> declared = List.of(names);
            for (int index = 0; index < names.length; index++) {
                roles[index] = positions[index] ? Role.WORLD : Role.UNTOUCHED;
            }

            List<String> missing = new ArrayList<>();
            row.forEach((name, role) -> {
                int index = declared.indexOf(name);
                if (index < 0) {
                    missing.add(name);
                } else {
                    roles[index] = role;
                }
            });

            if (!missing.isEmpty()) {
                LOGGER.warn("The {} rows of payload {} name {}, which its record {} does not declare; those names are"
                        + " skipped", PositionRows.FILE_NAME, payloadId, missing, type.getName());
            }

            return roles;
        }

        private static Form of(Class<?> type) {
            if (!type.isRecord()) {
                return POSITIONLESS;
            }

            RecordComponent[] components = type.getRecordComponents();
            boolean[] positions = new boolean[components.length];
            boolean carries = false;
            for (int index = 0; index < components.length; index++) {
                positions[index] = carriesPosition(components[index].getGenericType(), new HashSet<>());
                carries |= positions[index];
            }

            if (!carries) {
                return POSITIONLESS;
            }

            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                MethodHandle[] accessors = new MethodHandle[components.length];
                String[] names = new String[components.length];
                Class<?>[] parameterTypes = new Class<?>[components.length];
                for (int index = 0; index < components.length; index++) {
                    accessors[index] = lookup.unreflect(components[index].getAccessor()).asType(ACCESSOR_TYPE);
                    names[index] = components[index].getName();
                    parameterTypes[index] = components[index].getType();
                }

                MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(void.class, parameterTypes))
                        .asSpreader(Object[].class, components.length)
                        .asType(CONSTRUCTOR_TYPE);
                return new Form(constructor, accessors, names, positions);
            } catch (IllegalAccessException | NoSuchMethodException e) {
                LOGGER.warn("Record payload {} carries a world position, but its components cannot be reached ({});"
                        + " its positions cross the seam unfolded", type.getName(), e.toString());
                return POSITIONLESS;
            }
        }

        <T> T rebuilt(T instance, UnaryOperator<Object> fold) {
            return rebuiltFrom(instance, values -> {
                for (int index = 0; index < values.length; index++) {
                    Object value = values[index];
                    if (positions[index] && value != null) {
                        values[index] = fold.apply(value);
                    }
                }
            });
        }

        @SuppressWarnings("unchecked")
        <T> T rebuiltFrom(T instance, Consumer<@Nullable Object[]> fold) {
            MethodHandle canonical = constructor;
            if (canonical == null) {
                return instance;
            }

            try {
                Object[] values = new Object[accessors.length];
                for (int index = 0; index < accessors.length; index++) {
                    values[index] = (Object) accessors[index].invokeExact((Object) instance);
                }

                Object[] foldedValues = values.clone();
                fold.accept(foldedValues);
                boolean changed = false;
                for (int index = 0; index < values.length; index++) {
                    changed |= foldedValues[index] != values[index];
                }

                if (!changed) {
                    return instance;
                }

                Object rebuilt = (Object) canonical.invokeExact(foldedValues);
                return (T) rebuilt;
            } catch (Error error) {
                throw error;
            } catch (Throwable refusal) {
                if (refusalLogged.compareAndSet(false, true)) {
                    LOGGER.warn("Record {} refused its folded positions ({}); it crosses the seam unfolded",
                            instance.getClass().getName(), refusal.toString());
                }

                return instance;
            }
        }
    }

    private RecordPayloadFold() {
    }
}
