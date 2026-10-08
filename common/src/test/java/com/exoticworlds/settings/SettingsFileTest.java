package com.exoticworlds.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.exoticworlds.compat.MapCopies;

class SettingsFileTest {
    @TempDir
    Path directory;

    @Test
    void writesTheDefaultsWhenNoFileIsThere() {
        Path file = directory.resolve(SettingsService.FILE_NAME);

        assertEquals(Settings.defaults(), SettingsFile.load(file));
        assertTrue(Files.isRegularFile(file));
        assertEquals(Settings.defaults(), SettingsFile.load(file));
    }

    @Test
    void readsBackWhatItWrote() {
        Path file = directory.resolve(SettingsService.FILE_NAME);
        SettingsFile.save(file, new Settings(MapCopies.SINGLE));

        assertEquals(new Settings(MapCopies.SINGLE), SettingsFile.load(file));
    }

    @Test
    void fallsBackWhenTheValueIsUnknown() throws IOException {
        Path file = write("{ \"" + SettingsFile.MAP_COPIES_KEY + "\": \"sideways\" }");

        assertEquals(Settings.defaults(), SettingsFile.load(file));
    }

    @Test
    void fallsBackWhenTheFileIsNotAnObject() throws IOException {
        Path file = write("[]");

        assertEquals(Settings.defaults(), SettingsFile.load(file));
    }

    private Path write(String content) throws IOException {
        Path file = directory.resolve(SettingsService.FILE_NAME);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
