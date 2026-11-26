package com.exoticworlds.engine.net;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.jetbrains.annotations.Contract;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.core.fold.DeckTransformation;
import com.exoticworlds.engine.fold.FoldedCopies;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
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

    static final String PATH_SEPARATOR = ".";

    private static final ClassValue<Form> FORMS = new ClassValue<>() {
        @Override
        protected Form computeValue(Class<?> type) {
            return Form.of(type);
        }
    };

    private static volatile Rows rows = new Rows(Set.of(), Map.of());

    static void declare(Set<Identifier> deny, Map<Identifier, Map<String, ComponentRole>> payloads) {
        rows = new Rows(Set.copyOf(deny), Map.copyOf(payloads));
    }

    static CustomPacketPayload toClient(CustomPacketPayload payload, TranslationContext context) {
        return folded(payload, payload.type().id(), context, FoldedValue::toClient);
    }

    static CustomPacketPayload toServer(CustomPacketPayload payload, TranslationContext context) {
        return folded(payload, payload.type().id(), context, FoldedValue::toServer);
    }

    static Packet<?> toClient(Packet<?> packet, TranslationContext context) {
        return folded(packet, packet.type().id(), context, FoldedValue::toClient);
    }

    static Packet<?> toServer(Packet<?> packet, TranslationContext context) {
        return folded(packet, packet.type().id(), context, FoldedValue::toServer);
    }

    static Form getForm(Class<?> payloadType) {
        return FORMS.get(payloadType);
    }

    private static <T> T folded(T record, Identifier typeId, TranslationContext context,
            Function<TranslationContext, FoldedValue.Leaves> leavesOf) {
        Form form = FORMS.get(record.getClass());
        Rows current = rows;
        if (!form.hasPositions() || current.deny().contains(typeId)) {
            return record;
        }

        FoldedValue.Leaves leaves = leavesOf.apply(context);
        UnaryOperator<Object> fold = value -> FoldedValue.walk(context, leaves, value);
        @Nullable List<ResolvedRole> roles = current.rolesOf(typeId, record.getClass());
        if (roles == null) {
            return form.rebuilt(record, fold);
        }

        GroupShift group = new GroupShift(context.dimension(), leaves);
        return form.rebuiltFrom(record, values -> foldByRoles(values, roles, fold, group));
    }

    private static void foldByRoles(@Nullable Object[] values, List<ResolvedRole> roles, UnaryOperator<Object> fold,
            GroupShift group) {
        for (int index = 0; index < values.length; index++) {
            Object value = values[index];
            if (value == null) {
                continue;
            }

            values[index] = switch (roles.get(index)) {
                case ResolvedRole.Direct(Role role) -> switch (role) {
                    case WORLD -> fold.apply(value);
                    case UNTOUCHED -> value;
                    case GROUP -> group.moveMember(value);
                };
                case ResolvedRole.Nested nested -> foldNested(value, nested, fold, group);
            };
        }
    }

    @SuppressWarnings("unchecked")
    @Contract("!null, _, _, _ -> !null")
    private static @Nullable Object foldNested(@Nullable Object value, ResolvedRole.Nested nested,
            UnaryOperator<Object> fold, GroupShift group) {
        return switch (value) {
            case null -> null;
            case Optional<?> held -> {
                Object inner = held.orElse(null);
                if (inner == null) {
                    yield held;
                }

                Object foldedInner = foldNested(inner, nested, fold, group);
                yield foldedInner == inner ? held : Optional.of(foldedInner);
            }
            case List<?> list -> FoldedCopies.of((List<Object>) list, element -> foldNested(element, nested, fold,
                    group));
            default -> value.getClass() == nested.type()
                    ? nested.form().rebuiltFrom(value, values -> foldByRoles(values, nested.roles(), fold, group))
                    : fold.apply(value);
        };
    }

    private static List<ResolvedRole> resolveRoles(Map<String, ComponentRole> row, Class<?> type, String path,
            Identifier payloadId) {
        RecordComponent[] components = type.getRecordComponents();
        Form form = FORMS.get(type);
        List<String> declared = Arrays.stream(components).map(RecordComponent::getName).toList();
        ResolvedRole[] roles = new ResolvedRole[components.length];
        for (int index = 0; index < components.length; index++) {
            roles[index] = new ResolvedRole.Direct(form.isPositional(index) ? Role.WORLD : Role.UNTOUCHED);
        }

        List<String> missing = new ArrayList<>();
        row.forEach((name, role) -> {
            int index = declared.indexOf(name);
            if (index < 0) {
                missing.add(path + name);
                return;
            }

            ResolvedRole byType = roles[index];
            roles[index] = switch (role) {
                case ComponentRole.Direct(Role direct) -> new ResolvedRole.Direct(direct);
                case ComponentRole.Nested nested -> resolveNested(nested, components[index], path + name, payloadId,
                        byType);
            };
        });

        if (!missing.isEmpty()) {
            LOGGER.warn("The {} rows of payload {} name {}, which its record {} does not declare; those names are"
                    + " skipped", PositionRows.FILE_NAME, payloadId, missing, type.getName());
        }

        return List.of(roles);
    }

    private static ResolvedRole resolveNested(ComponentRole.Nested nested, RecordComponent component, String path,
            Identifier payloadId, ResolvedRole byType) {
        Class<?> held = heldRecordOf(component.getGenericType());
        if (held == null) {
            LOGGER.warn("The {} rows of payload {} give {} a nested object, but its declared type {} holds no record;"
                    + " the object is skipped", PositionRows.FILE_NAME, payloadId, path,
                    component.getGenericType().getTypeName());
            return byType;
        }

        List<ResolvedRole> roles = resolveRoles(nested.components(), held, path + PATH_SEPARATOR, payloadId);
        Form form = FORMS.get(held);
        return form.hasPositions() ? new ResolvedRole.Nested(held, form, roles) : byType;
    }

    private static @Nullable Class<?> heldRecordOf(Type type) {
        if (type instanceof ParameterizedType parameterized && CONTAINER_TYPES.contains(parameterized.getRawType())) {
            return heldRecordOf(parameterized.getActualTypeArguments()[0]);
        }

        return type instanceof Class<?> nested && nested.isRecord() ? nested : null;
    }

    private static boolean hasPosition(Type type, Set<Class<?>> visited) {
        if (type instanceof ParameterizedType parameterized && CONTAINER_TYPES.contains(parameterized.getRawType())) {
            return hasPosition(parameterized.getActualTypeArguments()[0], visited);
        }

        if (POSITION_TYPES.contains(type)) {
            return true;
        }

        if (!(type instanceof Class<?> nested) || !nested.isRecord() || !visited.add(nested)) {
            return false;
        }

        for (RecordComponent component : nested.getRecordComponents()) {
            if (hasPosition(component.getGenericType(), visited)) {
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

    public sealed interface ComponentRole {
        Codec<ComponentRole> CODEC = Codec.recursive("component role",
                self -> Codec.either(Role.CODEC, Codec.unboundedMap(Codec.STRING, self))
                        .xmap(spelled -> spelled.<ComponentRole>map(Direct::new, Nested::new),
                                ComponentRole::spellingOf));

        private static Either<Role, Map<String, ComponentRole>> spellingOf(ComponentRole role) {
            return switch (role) {
                case Direct(Role direct) -> Either.left(direct);
                case Nested(Map<String, ComponentRole> components) -> Either.right(components);
            };
        }

        record Direct(Role role) implements ComponentRole {
        }

        record Nested(Map<String, ComponentRole> components) implements ComponentRole {
            public Nested {
                components = Map.copyOf(components);
            }
        }
    }

    private sealed interface ResolvedRole {
        record Direct(Role role) implements ResolvedRole {
        }

        record Nested(Class<?> type, Form form, List<ResolvedRole> roles) implements ResolvedRole {
        }
    }

    private static final class GroupShift {
        private final ResourceKey<Level> dimension;

        private final FoldedValue.Leaves leaves;

        private @Nullable DeckTransformation move;

        GroupShift(ResourceKey<Level> dimension, FoldedValue.Leaves leaves) {
            this.dimension = dimension;
            this.leaves = leaves;
        }

        Object moveMember(Object member) {
            if (move == null) {
                Vec3 anchor = FoldedValue.anchorOf(dimension, member);
                move = anchor == null ? null : leaves.deck().apply(anchor);
            }

            return move == null ? member : FoldedValue.moved(dimension, move, member);
        }
    }

    private record Rows(Set<Identifier> deny, Map<Identifier, Map<String, ComponentRole>> payloads,
            Map<Identifier, List<ResolvedRole>> resolved) {
        Rows(Set<Identifier> deny, Map<Identifier, Map<String, ComponentRole>> payloads) {
            this(deny, payloads, new ConcurrentHashMap<>());
        }

        @Nullable List<ResolvedRole> rolesOf(Identifier typeId, Class<?> type) {
            Map<String, ComponentRole> row = payloads.get(typeId);
            if (row == null) {
                return null;
            }

            return resolved.computeIfAbsent(typeId, id -> resolveRoles(row, type, "", id));
        }
    }

    static final class Form {
        private static final Form POSITIONLESS = new Form(null, new MethodHandle[0], new boolean[0]);

        private final @Nullable MethodHandle constructor;

        private final MethodHandle[] accessors;

        private final boolean[] positional;

        private final AtomicBoolean refusalLogged = new AtomicBoolean();

        private Form(@Nullable MethodHandle constructor, MethodHandle[] accessors, boolean[] positional) {
            this.constructor = constructor;
            this.accessors = accessors;
            this.positional = positional;
        }

        boolean hasPositions() {
            return constructor != null;
        }

        private boolean isPositional(int index) {
            return hasPositions() && positional[index];
        }

        private static Form of(Class<?> type) {
            if (!type.isRecord()) {
                return POSITIONLESS;
            }

            RecordComponent[] components = type.getRecordComponents();
            boolean[] positional = positionalOf(components);
            return positional == null ? POSITIONLESS : reachable(type, components, positional);
        }

        private static boolean @Nullable [] positionalOf(RecordComponent[] components) {
            boolean[] positional = new boolean[components.length];
            boolean isCarrying = false;
            for (int index = 0; index < components.length; index++) {
                positional[index] = hasPosition(components[index].getGenericType(), new HashSet<>());
                isCarrying |= positional[index];
            }

            return isCarrying ? positional : null;
        }

        private static Form reachable(Class<?> type, RecordComponent[] components, boolean[] positional) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
                MethodHandle[] accessors = new MethodHandle[components.length];
                Class<?>[] parameterTypes = new Class<?>[components.length];
                for (int index = 0; index < components.length; index++) {
                    accessors[index] = lookup.unreflect(components[index].getAccessor()).asType(ACCESSOR_TYPE);
                    parameterTypes[index] = components[index].getType();
                }

                MethodHandle constructor = lookup.findConstructor(type, MethodType.methodType(void.class, parameterTypes))
                        .asSpreader(Object[].class, components.length)
                        .asType(CONSTRUCTOR_TYPE);
                return new Form(constructor, accessors, positional);
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
                    if (positional[index] && value != null) {
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
                Object[] values = valuesOf(instance);
                Object[] foldedValues = values.clone();
                fold.accept(foldedValues);
                if (!isAnyChanged(values, foldedValues)) {
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

        private Object[] valuesOf(Object instance) throws Throwable {
            Object[] values = new Object[accessors.length];
            for (int index = 0; index < accessors.length; index++) {
                values[index] = (Object) accessors[index].invokeExact(instance);
            }

            return values;
        }

        private static boolean isAnyChanged(Object[] values, Object[] foldedValues) {
            for (int index = 0; index < values.length; index++) {
                if (foldedValues[index] != values[index]) {
                    return true;
                }
            }

            return false;
        }
    }

    private RecordPayloadFold() {
    }
}
