package dev.monkeycraft.craftledgerjobs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public final class TextUtil {
    private static final UUID SYSTEM_MESSAGE_UUID = new UUID(0L, 0L);

    private TextUtil() {
    }

    public static void success(ServerPlayer player, String message) {
        send(player, success(message));
    }

    public static void info(ServerPlayer player, String message) {
        send(player, styled(message, ChatFormatting.AQUA));
    }

    public static void send(ServerPlayer player, Component message) {
        try {
            Method method = player.getClass().getMethod("sendSystemMessage", Component.class);
            method.invoke(player, message);
            return;
        } catch (NoSuchMethodException ignored) {
            // Minecraft 1.18 exposes player chat feedback through older method names.
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send player message", ex);
        }

        try {
            Method method = player.getClass().getMethod("displayClientMessage", Component.class, boolean.class);
            method.invoke(player, message, false);
            return;
        } catch (NoSuchMethodException ignored) {
            // Fall through to the 1.18 sendMessage(Component, UUID) signature.
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send player message", ex);
        }

        try {
            Method method = player.getClass().getMethod("sendMessage", Component.class, UUID.class);
            method.invoke(player, message, SYSTEM_MESSAGE_UUID);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send player message", ex);
        }
    }

    public static Component text(String message) {
        for (String methodName : new String[]{"literal", "m_237113_"}) {
            try {
                return (Component) Component.class.getMethod(methodName, String.class).invoke(null, message);
            } catch (NoSuchMethodException ignored) {
                // Try the next known runtime name.
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("Failed to create text component", ex);
            }
        }

        try {
            Class<?> textComponent = Class.forName("net.minecraft.network.chat.TextComponent");
            return (Component) textComponent.getConstructor(String.class).newInstance(message);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to create text component", ex);
        }
    }

    public static Component success(String message) {
        return styled(message, ChatFormatting.GREEN);
    }

    public static Component error(String message) {
        return styled(message, ChatFormatting.RED);
    }

    public static Component styled(String message, ChatFormatting color) {
        Component component = text(message);
        for (Method method : component.getClass().getMethods()) {
            if (!"withStyle".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            try {
                Object result;
                if (parameterType.isArray() && parameterType.getComponentType().isAssignableFrom(ChatFormatting.class)) {
                    result = method.invoke(component, (Object) new ChatFormatting[]{color});
                } else if (parameterType.isAssignableFrom(ChatFormatting.class)) {
                    result = method.invoke(component, color);
                } else {
                    continue;
                }
                if (result instanceof Component styled) {
                    return styled;
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                return component;
            }
        }
        return component;
    }
}
