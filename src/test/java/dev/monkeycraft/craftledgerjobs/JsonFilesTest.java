package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void writeAtomicCreatesParentDirectoryAndRemovesTempFile() throws Exception {
        Path path = tempDir.resolve("nested").resolve("data.json");

        JsonFiles.writeAtomic(path, Map.of("value", 7));

        assertTrue(Files.exists(path));
        assertFalse(Files.exists(path.resolveSibling("data.json.tmp")));
        TestData data = JsonFiles.read(path, TestData.class);
        assertEquals(7, data.value);
    }

    private static final class TestData {
        int value;
    }
}
