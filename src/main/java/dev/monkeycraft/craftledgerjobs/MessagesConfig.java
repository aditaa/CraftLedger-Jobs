package dev.monkeycraft.craftledgerjobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class MessagesConfig {
    private static final Pattern MESSAGE_KEY = Pattern.compile("[a-z0-9_.-]+");
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public Map<String, String> messages = new LinkedHashMap<>();

    public static MessagesConfig load(Path path) throws IOException {
        if (Files.notExists(path)) {
            MessagesConfig config = defaults();
            JsonFiles.writeAtomic(path, config);
            return config;
        }
        MessagesConfig config = JsonFiles.read(path, MessagesConfig.class);
        if (config == null) {
            return defaults();
        }
        return config.normalize();
    }

    public String get(String key) {
        return messages.getOrDefault(key, defaults().messages.getOrDefault(key, key));
    }

    public String format(String key, Map<String, String> placeholders) {
        String message = get(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return message;
    }

    public String format(String key, String placeholder, String value) {
        return format(key, Map.of(placeholder, value));
    }

    private MessagesConfig normalize() {
        if (messages == null) {
            messages = new LinkedHashMap<>();
        }
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(defaults().messages);
        messages.forEach((key, value) -> merged.put(key, value == null ? "" : value));
        messages = merged;
        validate();
        return this;
    }

    private void validate() {
        if (version < 1) {
            throw new ConfigValidationException("messages.json version must be greater than or equal to 1.");
        }
        messages.forEach((key, value) -> {
            if (key == null || !MESSAGE_KEY.matcher(key).matches()) {
                throw new ConfigValidationException("messages.json contains invalid message key: " + key);
            }
            if (value == null) {
                throw new ConfigValidationException("messages.json message " + key + " must not be null.");
            }
        });
    }

    private static MessagesConfig defaults() {
        MessagesConfig config = new MessagesConfig();
        config.messages.put("currency.disabled", "Currency is disabled.");
        config.messages.put("jobs.disabled", "Jobs are disabled.");
        config.messages.put("balance.self", "Balance: {balance}");
        config.messages.put("balance.other", "{player} balance: {balance}");
        config.messages.put("balance.updated", "Balance updated for {player}: {balance}");
        config.messages.put("pay.self", "You cannot pay yourself.");
        config.messages.put("pay.target_full", "That player cannot receive that amount.");
        config.messages.put("pay.insufficient", "Insufficient funds.");
        config.messages.put("pay.rollback_failed", "Payment failed and your money was returned.");
        config.messages.put("pay.sent", "Paid {target} {amount}. Balance: {balance}");
        config.messages.put("pay.received", "Received {amount} from {source}. Balance: {balance}");
        config.messages.put("job.unknown", "Unknown job: {job}");
        config.messages.put("job.already", "You already have that job.");
        config.messages.put("job.switching_disabled", "Leave your current job before joining another one.");
        config.messages.put("job.joined", "Joined job: {job}");
        config.messages.put("job.current", "Current job: {job}");
        config.messages.put("job.left", "Left your job.");
        config.messages.put("job.none", "You have not joined a job.");
        config.messages.put("job.payout", "Job payout: {payout}");
        config.messages.put("admin.reload_success", "CraftLedger Jobs reloaded.");
        config.messages.put("admin.reload_failed", "CraftLedger reload failed: {error}");
        config.messages.put("admin.unknown_player", "Unknown stored player: {player}. The player must join once before offline commands can target them.");
        config.messages.put("admin.player_info", "{player}: balance {balance}, job {job}");
        config.messages.put("admin.job_set", "Set {player}'s job to {job}.");
        config.messages.put("admin.job_cleared", "Cleared {player}'s job.");
        config.messages.put("transactions.empty", "No transactions logged.");
        config.messages.put("transactions.header", "Recent transactions:");
        config.messages.put("sell.hand.success", "Sold {count} item(s) from hand for {total}");
        config.messages.put("sell.all.success", "Sold {count} item(s) for {total}{summary}");
        config.messages.put("shop.buy.success", "Bought {count} {item} for {total}{overflow}");
        config.messages.put("shop.buy.overflow", " Some items were dropped because your inventory was full.");
        return config;
    }
}
