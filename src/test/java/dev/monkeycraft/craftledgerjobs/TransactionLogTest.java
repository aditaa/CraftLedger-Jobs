package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionLogTest {
    @TempDir
    Path tempDir;

    @Test
    void constructorCreatesParentDirectoryAndLogFile() throws Exception {
        Path path = tempDir.resolve("nested").resolve("transactions.log");

        new TransactionLog(path);

        assertTrue(Files.exists(path));
    }

    @Test
    void writeAppendsTabSeparatedAuditLine() throws Exception {
        Path path = tempDir.resolve("transactions.log");
        TransactionLog log = new TransactionLog(path);

        log.write("admin_balance_add", "Ada", "uuid-1", 12.345D, "operator");

        String[] fields = Files.readString(path).strip().split("\t", -1);
        assertEquals(6, fields.length);
        assertEquals("admin_balance_add", fields[1]);
        assertEquals("Ada", fields[2]);
        assertEquals("uuid-1", fields[3]);
        assertEquals("12.35", fields[4]);
        assertEquals("operator", fields[5]);
    }

    @Test
    void writeSanitizesControlCharactersFromFields() throws Exception {
        Path path = tempDir.resolve("transactions.log");
        TransactionLog log = new TransactionLog(path);

        log.write("pay\nsend", "Ada\tAdmin", "uuid\r1", 1.0D, "line one\nline two");

        String content = Files.readString(path);
        assertEquals(1, content.lines().count());
        String[] fields = content.strip().split("\t", -1);
        assertEquals("pay send", fields[1]);
        assertEquals("Ada Admin", fields[2]);
        assertEquals("uuid 1", fields[3]);
        assertEquals("line one line two", fields[5]);
    }

    @Test
    void writeSanitizesNonFiniteAmounts() throws Exception {
        Path path = tempDir.resolve("transactions.log");
        TransactionLog log = new TransactionLog(path);

        log.write("admin_balance_set", "Ada", "uuid-1", Double.NaN, "");

        String[] fields = Files.readString(path).strip().split("\t", -1);
        assertEquals("0.00", fields[4]);
    }
}
