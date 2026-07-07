package dev.monkeycraft.craftledgerjobs;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class CommandFeedback {
    private CommandFeedback() {
    }

    public static void success(CommandSourceStack source, Component message, boolean broadcastToOps) {
        try {
            Method supplierMethod = CommandSourceStack.class.getMethod("sendSuccess", Supplier.class, boolean.class);
            supplierMethod.invoke(source, (Supplier<Component>) () -> message, broadcastToOps);
            return;
        } catch (NoSuchMethodException ignored) {
            // Minecraft 1.18/1.19 use the older sendSuccess(Component, boolean) signature.
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send command feedback", ex);
        }

        try {
            Method componentMethod = CommandSourceStack.class.getMethod("sendSuccess", Component.class, boolean.class);
            componentMethod.invoke(source, message, broadcastToOps);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send command feedback", ex);
        }
    }

    public static void failure(CommandSourceStack source, Component message) {
        try {
            Method method = CommandSourceStack.class.getMethod("sendFailure", Component.class);
            method.invoke(source, message);
            return;
        } catch (NoSuchMethodException ignored) {
            // Fall through to generic command-source messaging for versions without sendFailure.
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send command feedback", ex);
        }

        message(source, message);
    }

    public static void message(CommandSourceStack source, Component message) {
        try {
            Method method = CommandSourceStack.class.getMethod("sendSystemMessage", Component.class);
            method.invoke(source, message);
            return;
        } catch (NoSuchMethodException ignored) {
            // Older command sources can still report through sendFailure.
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send command feedback", ex);
        }

        try {
            Method method = CommandSourceStack.class.getMethod("sendFailure", Component.class);
            method.invoke(source, message);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to send command feedback", ex);
        }
    }
}
