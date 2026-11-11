package com.minecraft.handytoolbox;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Contents of an open toolbox; every change is written back to the item's NBT. */
public class ToolboxContainer implements Container {
    /** Open toolboxes by item instance, so code that edits a box from outside can go through its open screen. */
    private static final Map<ItemStack, ToolboxContainer> OPEN = Collections.synchronizedMap(new WeakHashMap<>());

    private final Supplier<ItemStack> box;
    private final NonNullList<ItemStack> items;

    public ToolboxContainer(Supplier<ItemStack> box) {
        this.box = box;
        this.items = ToolboxData.load(box.get());
        OPEN.put(box.get(), this);
    }

    /** The open container showing this toolbox item, or null if it isn't open. */
    public static ToolboxContainer openFor(ItemStack box) {
        return OPEN.get(box);
    }

    public void close() {
        OPEN.values().remove(this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ToolLending.unmark(stack);
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        ItemStack stack = box.get();
        if (ToolLending.isToolbox(stack)) {
            ToolboxData.save(stack, items);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return ToolLending.isToolbox(box.get());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return ToolboxData.canStore(stack);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
