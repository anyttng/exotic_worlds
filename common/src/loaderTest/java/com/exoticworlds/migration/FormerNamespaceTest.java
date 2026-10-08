package com.exoticworlds.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.exoticworlds.ExoticWorlds;
import com.mojang.serialization.Lifecycle;

import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;

@Timeout(60)
class FormerNamespaceTest {
    private static final ResourceKey<Registry<String>> TEST_REGISTRY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, "former_namespace_test"));

    private static final String ENTRY_PATH = "toroidal";
    private static final String OLD_ONLY_PATH = "kept_old";
    private static final String MISSING_PATH = "missing";

    private static final String TERRAIN_MASK_KEY = ExoticWorlds.MODID + ":terrain_mask";
    private static final String MASK_FIELD = "probe";

    private static final List<String> BARE_TORUS_PRESETS = List.of("tiny", "small", "medium", "large", "huge");
    private static final String TORUS_PRESET_PREFIX = "torus_";

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void anOldIdIsAnsweredByItsTwin() {
        MappedRegistry<String> registry = registry();
        ResourceLocation current = ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, ENTRY_PATH);
        ResourceLocation former = ResourceLocation.fromNamespaceAndPath(FormerNamespace.NAMESPACE, ENTRY_PATH);

        assertSame(registry.get(current), registry.get(former));
        assertSame(registry.get(key(current)), registry.get(key(former)));
        assertEquals(registry.getHolder(current), registry.getHolder(former));
        assertEquals(registry.getHolder(key(current)), registry.getHolder(key(former)));
        assertTrue(registry.containsKey(former));
        assertTrue(registry.containsKey(key(former)));
        assertEquals(current, registry.getHolder(former).orElseThrow().key().location());
    }

    @Test
    void anOldIdStillRegisteredIsAnsweredByItself() {
        MappedRegistry<String> registry = registry();
        ResourceLocation former = ResourceLocation.fromNamespaceAndPath(FormerNamespace.NAMESPACE, OLD_ONLY_PATH);

        assertEquals(former, registry.getHolder(former).orElseThrow().key().location());
    }

    @Test
    void aMissStaysAMiss() {
        MappedRegistry<String> registry = registry();
        ResourceLocation former = ResourceLocation.fromNamespaceAndPath(FormerNamespace.NAMESPACE, MISSING_PATH);
        ResourceLocation current = ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, MISSING_PATH);

        assertTrue(registry.getHolder(former).isEmpty());
        assertNull(registry.get(key(former)));
        assertFalse(registry.containsKey(former));
        assertFalse(registry.containsKey(key(current)));
    }

    @Test
    void aBareTorusPresetOfTheFormerNamespaceIsAnsweredByItsPrefixedTwin() {
        for (String size : BARE_TORUS_PRESETS) {
            assertEquals(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, TORUS_PRESET_PREFIX + size),
                    FormerNamespace.twinOf(ResourceLocation.fromNamespaceAndPath(FormerNamespace.NAMESPACE, size)), size);
        }
    }

    @Test
    void aBareTorusPresetOfTheCurrentNamespaceHasNoTwin() {
        for (String size : BARE_TORUS_PRESETS) {
            assertNull(FormerNamespace.twinOf(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, size)), size);
        }
    }

    @Test
    void theCurrentKeyWinsOverTheOldOne() {
        CompoundTag current = new CompoundTag();
        current.putBoolean(MASK_FIELD, true);
        CompoundTag chunk = new CompoundTag();
        chunk.put(TERRAIN_MASK_KEY, current);
        chunk.put(FormerNamespace.formerKey(TERRAIN_MASK_KEY), new CompoundTag());

        assertSame(current, FormerNamespace.compound(chunk, TERRAIN_MASK_KEY).orElseThrow());
    }

    @Test
    void aTerrainMaskStoredUnderTheOldKeyIsRead() {
        CompoundTag former = new CompoundTag();
        former.putBoolean(MASK_FIELD, true);
        CompoundTag chunk = new CompoundTag();
        chunk.put(FormerNamespace.formerKey(TERRAIN_MASK_KEY), former);

        assertSame(former, FormerNamespace.compound(chunk, TERRAIN_MASK_KEY).orElseThrow());
    }

    @Test
    void aKeyOutsideOurNamespaceHasNoFormerForm() {
        assertThrows(IllegalArgumentException.class, () -> FormerNamespace.formerKey(MASK_FIELD));
    }

    private static MappedRegistry<String> registry() {
        MappedRegistry<String> registry = new MappedRegistry<>(TEST_REGISTRY, Lifecycle.stable());
        registry.register(key(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, ENTRY_PATH)), ENTRY_PATH,
                RegistrationInfo.BUILT_IN);
        registry.register(key(ResourceLocation.fromNamespaceAndPath(FormerNamespace.NAMESPACE, OLD_ONLY_PATH)),
                OLD_ONLY_PATH, RegistrationInfo.BUILT_IN);
        registry.register(key(ResourceLocation.fromNamespaceAndPath(ExoticWorlds.MODID, OLD_ONLY_PATH)),
                OLD_ONLY_PATH + "_current", RegistrationInfo.BUILT_IN);
        return registry;
    }

    private static ResourceKey<String> key(ResourceLocation id) {
        return ResourceKey.create(TEST_REGISTRY, id);
    }
}
