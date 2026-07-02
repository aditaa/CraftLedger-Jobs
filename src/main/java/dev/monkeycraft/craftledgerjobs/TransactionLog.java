package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

public final class TransactionLog implements TransactionStore {
    private static final int MAX_TAIL_LINES = 50;
    private static final int TAIL_CHUNK_SIZE = 8192;
    private final Path path;

    public TransactionLog(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
    }

    @Override
    public void write(String type, ServerPlayer player, double amount, String detail) {
        write(type, player.getGameProfile().getName(), player.getUUID().toString(), amount, detail);
    }

    @Override
    public void write(String type, String playerName, String playerUuid, double amount, String detail) {
        String line = "%s\t%s\t%s\t%s\t%.2f\t%s%n".formatted(
                Instant.now(), clean(type), clean(playerName), clean(playerUuid), cleanAmount(amount), clean(detail)
        );
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to write CraftLedger transaction log", ex);
        }
    }

    @Override
    public List<String> tail(int requestedLines) {
        int lines = Math.max(1, Math.min(MAX_TAIL_LINES, requestedLines));
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long position = file.length();
            int newlineCount = 0;
            byte[] buffer = new byte[0];

            while (position > 0 && newlineCount <= lines) {
                int chunkSize = (int) Math.min(TAIL_CHUNK_SIZE, position);
                position -= chunkSize;
                byte[] chunk = new byte[chunkSize];
                file.seek(position);
                file.readFully(chunk);
                buffer = prepend(chunk, buffer);
                newlineCount = countNewlines(buffer);
            }

            int start = startOffset(buffer, lines);
            String text = new String(buffer, start, buffer.length - start, StandardCharsets.UTF_8).stripTrailing();
            if (text.isBlank()) {
                return List.of();
            }
            List<String> allLines = text.lines().toList();
            return allLines.subList(Math.max(0, allLines.size() - lines), allLines.size());
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to read CraftLedger transaction log", ex);
            return List.of();
        }
    }

    @Override
    public List<String> tail(String playerNameOrUuid, int requestedLines) {
        String filter = playerNameOrUuid == null ? "" : playerNameOrUuid.trim();
        if (filter.isEmpty()) {
            return tail(requestedLines);
        }
        int lines = Math.max(1, Math.min(MAX_TAIL_LINES, requestedLines));
        try {
            List<String> matches = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                    .filter(line -> transactionMatches(line, filter))
                    .toList();
            return matches.subList(Math.max(0, matches.size() - lines), matches.size());
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to read CraftLedger transaction log", ex);
            return List.of();
        }
    }

    private static byte[] prepend(byte[] prefix, byte[] suffix) {
        byte[] combined = new byte[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, combined, 0, prefix.length);
        System.arraycopy(suffix, 0, combined, prefix.length, suffix.length);
        return combined;
    }

    private static int countNewlines(byte[] bytes) {
        int count = 0;
        for (byte value : bytes) {
            if (value == '\n') {
                count++;
            }
        }
        return count;
    }

    private static int startOffset(byte[] bytes, int requestedLines) {
        int newlines = 0;
        for (int index = bytes.length - 1; index >= 0; index--) {
            if (bytes[index] == '\n') {
                newlines++;
                if (newlines > requestedLines) {
                    return index + 1;
                }
            }
        }
        return 0;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static double cleanAmount(double amount) {
        return Double.isFinite(amount) ? amount : 0;
    }

    private static boolean transactionMatches(String line, String playerNameOrUuid) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 4) {
            return false;
        }
        return parts[2].equalsIgnoreCase(playerNameOrUuid) || parts[3].equalsIgnoreCase(playerNameOrUuid);
    }
}
