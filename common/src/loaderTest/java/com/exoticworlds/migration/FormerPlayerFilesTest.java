package com.exoticworlds.migration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.exoticworlds.ExoticWorlds;
import com.exoticworlds.engine.seam.circumnavigation.SeamTravel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

@Timeout(60)
class FormerPlayerFilesTest {
    private static final String PLAYER = "49569d67-e873-42fd-9cd9-664f9a6dbdfe";
    private static final String UNTOUCHED_PLAYER = "0f6d1c2a-3b4e-4f5a-8b9c-0d1e2f3a4b5c";
    private static final String PLAYER_DATA = PLAYER + ".dat";
    private static final String PLAYER_DATA_BACKUP = PLAYER + ".dat_old";
    private static final String UNTOUCHED_DATA = UNTOUCHED_PLAYER + ".dat";
    private static final String ADVANCEMENTS = PLAYER + ".json";

    private static final String TRAVEL_KEY = ExoticWorlds.MODID + ":travel";
    private static final String CIRCUMNAVIGATE_ID = ExoticWorlds.MODID + ":circumnavigate";
    private static final String VANILLA_KEY = "SelectedItemSlot";
    private static final String VANILLA_ADVANCEMENT = "minecraft:story/root";
    private static final String DATA_VERSION_KEY = "DataVersion";
    private static final int SELECTED_SLOT = 3;
    private static final int DATA_VERSION = 4790;
    private static final String LAP_X_KEY = "x";
    private static final String LAP_Z_KEY = "z";
    private static final double TRAVELLED_X = 504.24;
    private static final double TRAVELLED_Z = 0.65;
    private static final String GRANTED_AT = "2026-09-23 12:36:31 +0300";

    private static HolderLookup.Provider registries;

    @TempDir
    Path world;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
    }

    @Test
    void oldKeysOfEveryPlayerFileAreRewrittenUnderTheCurrentNamespace() throws IOException {
        Path data = Files.createDirectories(this.world.resolve("players/data"));
        Path advancements = Files.createDirectories(this.world.resolve("players/advancements"));
        NbtIo.writeCompressed(formerPlayer(), data.resolve(PLAYER_DATA));
        NbtIo.writeCompressed(formerPlayer(), data.resolve(PLAYER_DATA_BACKUP));
        Files.writeString(advancements.resolve(ADVANCEMENTS), formerAdvancements(), StandardCharsets.UTF_8);

        FormerPlayerFiles.migrate(data, advancements);

        for (String file : List.of(PLAYER_DATA, PLAYER_DATA_BACKUP)) {
            CompoundTag player = NbtIo.readCompressed(data.resolve(file), NbtAccounter.unlimitedHeap());
            assertFalse(player.toString().contains(FormerNamespace.NAMESPACE), file + ": " + player);
            assertEquals(SELECTED_SLOT, player.getIntOr(VANILLA_KEY, -1), file);
            SeamTravel travel = TagValueInput.create(ProblemReporter.DISCARDING, registries, player)
                    .read(TRAVEL_KEY, SeamTravel.CODEC).orElseThrow();
            assertEquals(new SeamTravel.Lap(TRAVELLED_X, TRAVELLED_Z), travel.in(Level.OVERWORLD), file);
        }

        String written = Files.readString(advancements.resolve(ADVANCEMENTS), StandardCharsets.UTF_8);
        assertFalse(written.contains(FormerNamespace.NAMESPACE), written);
        JsonObject progress = JsonParser.parseString(written).getAsJsonObject();
        assertEquals(GRANTED_AT, progress.getAsJsonObject(CIRCUMNAVIGATE_ID).getAsJsonObject("criteria")
                .get("circumnavigated").getAsString());
        assertTrue(progress.has(VANILLA_ADVANCEMENT));
        assertEquals(DATA_VERSION, progress.get(DATA_VERSION_KEY).getAsInt());

        assertEquals(List.of(PLAYER_DATA, PLAYER_DATA_BACKUP), names(data));
        assertEquals(List.of(ADVANCEMENTS), names(advancements));
    }

    @Test
    void aFileWithoutOldKeysIsLeftAsItIs() throws IOException {
        Path data = Files.createDirectories(this.world.resolve("players/data"));
        CompoundTag current = new CompoundTag();
        current.putInt(VANILLA_KEY, SELECTED_SLOT);
        NbtIo.writeCompressed(current, data.resolve(UNTOUCHED_DATA));
        byte[] before = Files.readAllBytes(data.resolve(UNTOUCHED_DATA));

        FormerPlayerFiles.migrate(data, this.world.resolve("players/advancements"));

        assertArrayEquals(before, Files.readAllBytes(data.resolve(UNTOUCHED_DATA)));
    }

    @Test
    void aSecondPassChangesNothing() throws IOException {
        Path data = Files.createDirectories(this.world.resolve("players/data"));
        Path advancements = Files.createDirectories(this.world.resolve("players/advancements"));
        NbtIo.writeCompressed(formerPlayer(), data.resolve(PLAYER_DATA));
        Files.writeString(advancements.resolve(ADVANCEMENTS), formerAdvancements(), StandardCharsets.UTF_8);
        FormerPlayerFiles.migrate(data, advancements);
        byte[] player = Files.readAllBytes(data.resolve(PLAYER_DATA));
        byte[] progress = Files.readAllBytes(advancements.resolve(ADVANCEMENTS));

        FormerPlayerFiles.migrate(data, advancements);

        assertArrayEquals(player, Files.readAllBytes(data.resolve(PLAYER_DATA)));
        assertArrayEquals(progress, Files.readAllBytes(advancements.resolve(ADVANCEMENTS)));
    }

    private static CompoundTag formerPlayer() {
        CompoundTag lap = new CompoundTag();
        lap.putDouble(LAP_X_KEY, TRAVELLED_X);
        lap.putDouble(LAP_Z_KEY, TRAVELLED_Z);
        CompoundTag laps = new CompoundTag();
        laps.put(Level.OVERWORLD.identifier().toString(), lap);
        CompoundTag player = new CompoundTag();
        player.putInt(VANILLA_KEY, SELECTED_SLOT);
        player.put(FormerNamespace.formerKey(TRAVEL_KEY), laps);
        return player;
    }

    private static String formerAdvancements() {
        return "{\n"
                + "  \"" + VANILLA_ADVANCEMENT + "\": {\"criteria\": {\"crafting_table\": \"" + GRANTED_AT
                + "\"}, \"done\": true},\n"
                + "  \"" + FormerNamespace.formerKey(CIRCUMNAVIGATE_ID) + "\": {\"criteria\": {\"circumnavigated\": \""
                + GRANTED_AT + "\"}, \"done\": true},\n"
                + "  \"" + DATA_VERSION_KEY + "\": " + DATA_VERSION + "\n"
                + "}\n";
    }

    private static List<String> names(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }
}
