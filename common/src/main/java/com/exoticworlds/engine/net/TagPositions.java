package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.exoticworlds.core.StartupRegistry;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class TagPositions {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int COMPONENTS = 3;

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    public interface Seat {
        BlockPos seat(BlockPos stored);

        Vec3 seat(Vec3 stored);
    }

    public enum PositionShape {
        BLOCK_POS {
            @Override
            @Nullable Tag seated(Seat seat, Tag stored) {
                if (!(stored instanceof IntArrayTag array) || array.size() != COMPONENTS) {
                    return null;
                }

                int[] components = array.getAsIntArray();
                BlockPos position = new BlockPos(components[X], components[Y], components[Z]);
                BlockPos seated = seat.seat(position);
                return seated.equals(position)
                        ? null
                        : new IntArrayTag(new int[] {seated.getX(), seated.getY(), seated.getZ()});
            }
        },
        PACKED_LONG {
            @Override
            @Nullable Tag seated(Seat seat, Tag stored) {
                if (!(stored instanceof LongTag packed)) {
                    return null;
                }

                BlockPos position = BlockPos.of(packed.value());
                BlockPos seated = seat.seat(position);
                return seated.equals(position) ? null : LongTag.valueOf(seated.asLong());
            }
        },
        VEC3_LIST {
            @Override
            @Nullable Tag seated(Seat seat, Tag stored) {
                if (!(stored instanceof ListTag list) || list.size() != COMPONENTS
                        || !(list.get(X) instanceof DoubleTag xTag)
                        || !(list.get(Y) instanceof DoubleTag yTag)
                        || !(list.get(Z) instanceof DoubleTag zTag)) {
                    return null;
                }

                Vec3 position = new Vec3(xTag.value(), yTag.value(), zTag.value());
                Vec3 seated = seat.seat(position);
                return seated.equals(position) ? null : doubleList(seated);
            }
        };

        abstract @Nullable Tag seated(Seat seat, Tag stored);
    }

    public enum Nesting {
        TOP {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position) {
                return seatedKey(seat, tag, position);
            }
        },
        COMPOUND {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position) {
                String container = position.container();
                if (!(tag.get(container) instanceof CompoundTag stored)) {
                    return null;
                }

                CompoundTag moved = seatedKey(seat, stored, position);
                return moved == null ? null : with(tag, container, moved);
            }
        },
        EACH_OF_LIST {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position) {
                String container = position.container();
                if (!(tag.get(container) instanceof ListTag stored)) {
                    return null;
                }

                ListTag seated = null;
                for (int index = 0; index < stored.size(); index++) {
                    if (!(stored.get(index) instanceof CompoundTag element)) {
                        continue;
                    }

                    CompoundTag moved = seatedKey(seat, element, position);
                    if (moved == null) {
                        continue;
                    }

                    if (seated == null) {
                        seated = new ListTag();
                        seated.addAll(stored);
                    }

                    seated.set(index, moved);
                }

                return seated == null ? null : with(tag, container, seated);
            }
        };

        abstract @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position);
    }

    public record TagPosition(Nesting nesting, @Nullable String container, String key, PositionShape shape) {
        public TagPosition(String key, PositionShape shape) {
            this(Nesting.TOP, null, key, shape);
        }

        public TagPosition {
            if ((container == null) != (nesting == Nesting.TOP)) {
                throw new IllegalArgumentException(nesting + " names a container, and " + container + " was given");
            }
        }

        boolean sharesAddressWith(TagPosition other) {
            return nesting == other.nesting && Objects.equals(container, other.container) && key.equals(other.key);
        }
    }

    public record Subject(Class<?> type, @Nullable Identifier id) {
    }

    public static final class Table {
        private final String label;
        private final StartupRegistry<Class<?>, List<TagPosition>> registered;
        private volatile Map<Identifier, List<TagPosition>> declared = Map.of();
        private volatile Map<Subject, List<TagPosition>> resolved = new ConcurrentHashMap<>();

        public Table(String subject) {
            this.label = subject;
            this.registered = new StartupRegistry<>(subject);
        }

        public void register(Class<?> subjectType, PositionShape shape, String... keys) {
            register(subjectType, Nesting.TOP, null, shape, keys);
        }

        public void registerIn(Class<?> subjectType, String container, PositionShape shape, String... keys) {
            register(subjectType, Nesting.COMPOUND, container, shape, keys);
        }

        public void registerInEach(Class<?> subjectType, String container, PositionShape shape, String... keys) {
            register(subjectType, Nesting.EACH_OF_LIST, container, shape, keys);
        }

        private synchronized void register(Class<?> subjectType, Nesting nesting, @Nullable String container,
                PositionShape shape, String... keys) {
            if (keys.length == 0) {
                throw new IllegalArgumentException(shape + " was registered on " + subjectType.getName()
                        + " with no key");
            }

            List<TagPosition> positions = new ArrayList<>(registered.entries().getOrDefault(subjectType, List.of()));
            for (String key : keys) {
                positions.add(new TagPosition(nesting, container, key, shape));
            }

            registered.register(subjectType, List.copyOf(positions));
            resolved = new ConcurrentHashMap<>();
        }

        public synchronized void declare(Map<Identifier, List<TagPosition>> rows) {
            declared = Map.copyOf(rows);
            resolved = new ConcurrentHashMap<>();
        }

        public boolean carriesPositions(Subject carrier) {
            return !positionsOf(carrier).isEmpty();
        }

        public CompoundTag seatedIn(Seat seat, Subject carrier, CompoundTag tag) {
            List<TagPosition> positions = positionsOf(carrier);
            return positions.isEmpty() ? tag : TagPositions.seatedIn(seat, positions, tag);
        }

        private List<TagPosition> positionsOf(Subject carrier) {
            return resolved.computeIfAbsent(carrier, this::resolve);
        }

        private List<TagPosition> resolve(Subject carrier) {
            List<TagPosition> positions = new ArrayList<>();
            registered.entries().forEach((registeredType, registeredPositions) -> {
                if (registeredType.isAssignableFrom(carrier.type())) {
                    positions.addAll(registeredPositions);
                }
            });

            List<TagPosition> registeredOnly = List.copyOf(positions);
            List<TagPosition> rows = carrier.id() == null ? List.of() : declared.getOrDefault(carrier.id(), List.of());
            for (TagPosition row : rows) {
                if (registeredOnly.stream().anyMatch(row::sharesAddressWith)) {
                    LOGGER.warn("{}: a data file declares {} for {} at an address a registration on {} already holds;"
                            + " the registration is kept", label, row, carrier.id(), carrier.type().getName());
                } else {
                    positions.add(row);
                }
            }

            return List.copyOf(positions);
        }
    }

    public static CompoundTag seatedIn(Seat seat, List<TagPosition> positions, CompoundTag tag) {
        CompoundTag folded = tag;
        for (TagPosition position : positions) {
            CompoundTag moved = position.nesting().seated(seat, folded, position);
            if (moved != null) {
                folded = moved;
            }
        }

        return folded;
    }

    private static @Nullable CompoundTag seatedKey(Seat seat, CompoundTag tag, TagPosition position) {
        Tag stored = tag.get(position.key());
        if (stored == null) {
            return null;
        }

        Tag seated = position.shape().seated(seat, stored);
        return seated == null ? null : with(tag, position.key(), seated);
    }

    private static CompoundTag with(CompoundTag tag, String key, Tag value) {
        CompoundTag copy = new CompoundTag();
        tag.forEach(copy::put);
        copy.put(key, value);
        return copy;
    }

    private static ListTag doubleList(Vec3 position) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(position.x));
        list.add(DoubleTag.valueOf(position.y));
        list.add(DoubleTag.valueOf(position.z));
        return list;
    }

    private TagPositions() {
    }
}
