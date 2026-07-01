package dev.monkeycraft.craftledgerjobs;

import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class TransactionLog {
    private final Path path;

    public TransactionLog(Path path) throws IOException {
        this.path = path;
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.createFile(path);
        }
    }

    public void write(String type, ServerPlayer player, double amount, String detail) {
        write(type, player.getGameProfile().getName(), player.getUUID().toString(), amount, detail);
    }

    public void write(String type, String playerName, String playerUuid, double amount, String detail) {
        String line = "%s\t%s\t%s\t%s\t%.2f\t%s%n".formatted(
                Instant.now(), type, playerName, playerUuid, amount, detail == null ? "" : detail
        );
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            CraftLedgerJobs.LOGGER.error("Failed to write CraftLedger transaction log", ex);
        }
    }
}
