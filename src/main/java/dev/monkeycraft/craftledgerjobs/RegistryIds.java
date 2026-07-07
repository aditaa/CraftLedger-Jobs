package dev.monkeycraft.craftledgerjobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class RegistryIds {
    private RegistryIds() {
    }

    static ResourceLocation blockId(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    static ResourceLocation entityTypeId(EntityType<?> entityType) {
        return registryKey(entityType, "ENTITY_TYPES", "ENTITIES");
    }

    static boolean hasItem(ResourceLocation itemId) {
        return ForgeRegistries.ITEMS.containsKey(itemId);
    }

    static Item item(ResourceLocation itemId) {
        return ForgeRegistries.ITEMS.getValue(itemId);
    }

    static ResourceLocation itemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    private static ResourceLocation registryKey(Object value, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Object registry = ForgeRegistries.class.getField(fieldName).get(null);
                Method getKey = registry.getClass().getMethod("getKey", value.getClass());
                Object key = getKey.invoke(registry, value);
                if (key instanceof ResourceLocation id) {
                    return id;
                }
            } catch (NoSuchFieldException ignored) {
                // Try the field name used by another Forge generation.
            } catch (NoSuchMethodException ex) {
                return registryKeyByAssignableMethod(value, fieldName);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("Failed to read Forge registry key", ex);
            }
        }
        throw new IllegalStateException("Missing Forge registry field for " + value.getClass().getName());
    }

    private static ResourceLocation registryKeyByAssignableMethod(Object value, String fieldName) {
        try {
            Object registry = ForgeRegistries.class.getField(fieldName).get(null);
            for (Method method : registry.getClass().getMethods()) {
                if (!"getKey".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                    continue;
                }
                Object key = method.invoke(registry, value);
                if (key instanceof ResourceLocation id) {
                    return id;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to read Forge registry key", ex);
        }
        throw new IllegalStateException("Missing getKey method for Forge registry " + fieldName);
    }
}
