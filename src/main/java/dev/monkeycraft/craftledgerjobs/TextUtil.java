package dev.monkeycraft.craftledgerjobs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;

public final class TextUtil {
    private TextUtil() {
    }

    public static void success(ServerPlayer player, String message) {
        player.sendSystemMessage(text(message).withStyle(ChatFormatting.GREEN));
    }

    public static void info(ServerPlayer player, String message) {
        player.sendSystemMessage(text(message).withStyle(ChatFormatting.AQUA));
    }

    public static Component text(String message) {
        try {
            return (Component) Component.class.getMethod("literal", String.class).invoke(null, message);
        } catch (NoSuchMethodException ignored) {
            // Minecraft 1.18 uses TextComponent instead of Component.literal(...).
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to create text component", ex);
        }

        try {
            Class<?> textComponent = Class.forName("net.minecraft.network.chat.TextComponent");
            return (Component) textComponent.getConstructor(String.class).newInstance(message);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create text component", ex);
        }
    }

    public static Component success(String message) {
        return text(message).withStyle(ChatFormatting.GREEN);
    }

    public static Component error(String message) {
        return text(message).withStyle(ChatFormatting.RED);
    }
}
