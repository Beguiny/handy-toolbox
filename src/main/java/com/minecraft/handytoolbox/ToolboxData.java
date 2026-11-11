package com.minecraft.handytoolbox;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What fits in a toolbox and how its contents are stored in the item's NBT. */
public final class ToolboxData {
    public static final int ROWS = 2;
    public static final int SIZE = ROWS * 9;

    /** Extra items accepted besides damageable tools (modpacks can add items to this tag). */
    public static final TagKey<Item> STORABLE = ItemTags.create(new ResourceLocation(HandyToolbox.MOD_ID, "storable"));

    private ToolboxData() {
    }

    public static boolean canStore(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ToolboxItem) {
            return false;
        }
        if (stack.is(STORABLE)) {
            return true;
        }
        if (stack.getMaxStackSize() != 1) {
            return false;
        }
        // unbreakable tools (e.g. Allthemodium) have no durability, so also check the item type and tags
        return stack.isDamageableItem() || isGregTechTool(stack) || hasToolTag(stack);
    }

    /** Any "*:tools/..." tag (forge:tools/files, forge:tools/pickaxes, c:tools/...). */
    private static boolean hasToolTag(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().startsWith("tools"));
    }

    private static final Class<?> GT_TOOL = findClass("com.gregtechceu.gtceu.api.item.IGTTool");

    private static boolean isGregTechTool(ItemStack stack) {
        return GT_TOOL != null && GT_TOOL.isInstance(stack.getItem());
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name, false, ToolboxData.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    public static NonNullList<ItemStack> load(ItemStack box) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        CompoundTag tag = box.getTag();
        if (tag != null && tag.contains("Items", Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(tag, items);
        }
        return items;
    }

    public static void save(ItemStack box, NonNullList<ItemStack> items) {
        ContainerHelper.saveAllItems(box.getOrCreateTag(), items, true);
    }

    public static int count(ItemStack box) {
        int count = 0;
        for (ItemStack stack : load(box)) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
