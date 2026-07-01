package dev.monkeycraft.craftledgerjobs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TextUtil {
    private TextUtil() {
    }

    public static void success(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    public static void info(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.AQUA));
    }

    public static Component success(String message) {
        return Component.literal(message).withStyle(ChatFormatting.GREEN);
    }

    public static Component error(String message) {
        return Component.literal(message).withStyle(ChatFormatting.RED);
    }
}
