package dev.monkeycraft.craftledgerjobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

final class RegistryIds {
    private RegistryIds() {
    }

    static ResourceLocation blockId(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    static ResourceLocation entityTypeId(EntityType<?> entityType) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entityType);
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
}
