package com.exoticworlds.engine.net;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class TagPositions {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int VEC3_COMPONENTS = 3;
    private static final int BLOCK_TRIPLE_KEYS = 3;

    private static final int TRIPLE_X = 0;
    private static final int TRIPLE_Y = 1;
    private static final int TRIPLE_Z = 2;

    public interface Seat {
        BlockPos seat(BlockPos stored);

        Vec3 seat(Vec3 stored);
    }

    public enum PositionShape {
        BLOCK_POS(1) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                String key = keys.getFirst();
                BlockPos stored = NbtUtils.readBlockPos(tag, key).orElse(null);
                if (stored == null) {
                    return null;
                }

                BlockPos seated = seat.seat(stored);
                return seated.equals(stored) ? null : fragment(key, NbtUtils.writeBlockPos(seated));
            }
        },
        PACKED_LONG(1) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                String key = keys.getFirst();
                if (!tag.contains(key, Tag.TAG_LONG)) {
                    return null;
                }

                BlockPos stored = BlockPos.of(tag.getLong(key));
                BlockPos seated = seat.seat(stored);
                return seated.equals(stored) ? null : fragment(key, LongTag.valueOf(seated.asLong()));
            }
        },
        VEC3_LIST(1) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                String key = keys.getFirst();
                ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
                if (list.size() != VEC3_COMPONENTS) {
                    return null;
                }

                Vec3 stored = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
                Vec3 seated = seat.seat(stored);
                return seated.equals(stored) ? null : fragment(key, doubleList(seated));
            }
        },
        BLOCK_INT_TRIPLE(BLOCK_TRIPLE_KEYS) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                for (String key : keys) {
                    if (!tag.contains(key, Tag.TAG_INT)) {
                        return null;
                    }
                }

                BlockPos stored = new BlockPos(tag.getInt(keys.get(TRIPLE_X)), tag.getInt(keys.get(TRIPLE_Y)),
                        tag.getInt(keys.get(TRIPLE_Z)));
                BlockPos seated = seat.seat(stored);
                if (seated.equals(stored)) {
                    return null;
                }

                CompoundTag moved = new CompoundTag();
                moved.putInt(keys.get(TRIPLE_X), seated.getX());
                moved.putInt(keys.get(TRIPLE_Y), seated.getY());
                moved.putInt(keys.get(TRIPLE_Z), seated.getZ());
                return moved;
            }
        };

        private final int keyCount;

        PositionShape(int keyCount) {
            this.keyCount = keyCount;
        }

        public int keyCount() {
            return keyCount;
        }

        abstract @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys);
    }

    public enum Nesting {
        TOP {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position) {
                return position.shape().seated(seat, tag, position.keys());
            }
        },
        COMPOUND {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position) {
                String container = position.container();
                CompoundTag stored = tag.getCompound(container);
                CompoundTag moved = position.shape().seated(seat, stored, position.keys());
                return moved == null ? null : fragment(container, merged(stored, moved));
            }
        },
        EACH_OF_LIST {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position) {
                String container = position.container();
                ListTag stored = tag.getList(container, Tag.TAG_COMPOUND);
                ListTag seated = null;
                for (int index = 0; index < stored.size(); index++) {
                    CompoundTag element = stored.getCompound(index);
                    CompoundTag moved = position.shape().seated(seat, element, position.keys());
                    if (moved == null) {
                        continue;
                    }

                    if (seated == null) {
                        seated = new ListTag();
                        seated.addAll(stored);
                    }

                    seated.set(index, merged(element, moved));
                }

                return seated == null ? null : fragment(container, seated);
            }
        };

        abstract @Nullable CompoundTag seated(Seat seat, CompoundTag tag, TagPosition position);
    }

    public record TagPosition(Nesting nesting, @Nullable String container, List<String> keys, PositionShape shape) {
        public TagPosition(List<String> keys, PositionShape shape) {
            this(Nesting.TOP, null, keys, shape);
        }

        public TagPosition(String key, PositionShape shape) {
            this(List.of(key), shape);
        }

        public TagPosition(Nesting nesting, String container, String key, PositionShape shape) {
            this(nesting, container, List.of(key), shape);
        }

        public TagPosition {
            keys = List.copyOf(keys);
            if (keys.size() != shape.keyCount()) {
                throw new IllegalArgumentException(shape + " spreads a position over " + shape.keyCount()
                        + " keys, and " + keys.size() + " were given");
            }

            if ((container == null) != (nesting == Nesting.TOP)) {
                throw new IllegalArgumentException(nesting + " names a container, and " + container + " was given");
            }
        }

        boolean sharesAddressWith(TagPosition other) {
            return nesting == other.nesting && Objects.equals(container, other.container) && keys.equals(other.keys);
        }
    }

    public record Subject(Class<?> type, @Nullable ResourceLocation id) {
    }

    public static final class Table {
        private final String label;
        private final StartupRegistry<Class<?>, List<TagPosition>> registered;
        private volatile Map<ResourceLocation, List<TagPosition>> declared = Map.of();
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
            int keyCount = shape.keyCount();
            if (keys.length == 0 || keys.length % keyCount != 0) {
                throw new IllegalArgumentException(shape + " spreads a position over " + keyCount + " keys, and "
                        + keys.length + " were registered on " + subjectType.getName());
            }

            List<TagPosition> positions = new ArrayList<>(registered.entries().getOrDefault(subjectType, List.of()));
            for (int index = 0; index < keys.length; index += keyCount) {
                positions.add(new TagPosition(nesting, container,
                        Arrays.asList(keys).subList(index, index + keyCount), shape));
            }

            registered.register(subjectType, List.copyOf(positions));
            resolved = new ConcurrentHashMap<>();
        }

        public synchronized void declare(Map<ResourceLocation, List<TagPosition>> rows) {
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
        CompoundTag folded = null;
        for (TagPosition position : positions) {
            CompoundTag current = folded == null ? tag : folded;
            CompoundTag moved = position.nesting().seated(seat, current, position);
            if (moved != null) {
                folded = merged(current, moved);
            }
        }

        return folded == null ? tag : folded;
    }

    private static CompoundTag merged(CompoundTag stored, CompoundTag moved) {
        CompoundTag copy = new CompoundTag();
        for (String key : stored.getAllKeys()) {
            copy.put(key, stored.get(key));
        }

        for (String key : moved.getAllKeys()) {
            copy.put(key, moved.get(key));
        }

        return copy;
    }

    private static CompoundTag fragment(String key, Tag value) {
        CompoundTag moved = new CompoundTag();
        moved.put(key, value);
        return moved;
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
