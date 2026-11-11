package com.minecraft.handytoolbox;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Lends tools from a toolbox to the inventory (so JEI can move them into the grid) and returns them later.
 * A borrowed tool carries an NBT marker that is removed when it goes back into a box.
 * Runs on the client (prediction) and on the server (which validates every move).
 */
public final class ToolLending {
    public static final String BORROWED = "handytoolbox_borrowed";
    /** Main inventory slots (hotbar + 27). */
    public static final int MAIN_SLOTS = 36;

    /** Takes tool {@code innerSlot} out of the box in {@code boxSlot} and puts it in the empty slot {@code destSlot}. */
    public record Move(int boxSlot, int innerSlot, int destSlot) {
    }

    private ToolLending() {
    }

    public static boolean isToolbox(ItemStack stack) {
        return stack.getItem() instanceof ToolboxItem;
    }

    /** Where a toolbox can be: main inventory or offhand. */
    public static boolean isBoxSlot(int slot) {
        return (slot >= 0 && slot < MAIN_SLOTS) || slot == Inventory.SLOT_OFFHAND;
    }

    public static int[] boxSlots() {
        int[] slots = new int[MAIN_SLOTS + 1];
        for (int i = 0; i < MAIN_SLOTS; i++) {
            slots[i] = i;
        }
        slots[MAIN_SLOTS] = Inventory.SLOT_OFFHAND;
        return slots;
    }

    public static boolean apply(Player player, Move move) {
        Inventory inventory = player.getInventory();
        if (!isBoxSlot(move.boxSlot()) || move.destSlot() < 0 || move.destSlot() >= MAIN_SLOTS
                || move.innerSlot() < 0 || move.innerSlot() >= ToolboxData.SIZE) {
            return false;
        }
        ItemStack box = inventory.getItem(move.boxSlot());
        if (!isToolbox(box) || !inventory.getItem(move.destSlot()).isEmpty()) {
            return false;
        }
        NonNullList<ItemStack> items = ToolboxData.load(box);
        ItemStack tool = items.get(move.innerSlot());
        if (tool.isEmpty()) {
            return false;
        }
        items.set(move.innerSlot(), ItemStack.EMPTY);
        ToolboxData.save(box, items);
        mark(tool);
        inventory.setItem(move.destSlot(), tool);
        return true;
    }

    /**
     * Server side: the client already predicted these slots (or got a direct slot packet from vanilla), so the
     * server's copy of the client state is stale. Marks the slots as unknown so the next sync sends the real
     * contents; otherwise the client could keep showing a ghost tool.
     */
    public static void forceResync(ServerPlayer player, int... inventorySlots) {
        ItemStack unknown = new ItemStack(Items.BARRIER);
        unknown.getOrCreateTag().putBoolean(HandyToolbox.MOD_ID + "_resync", true);
        for (AbstractContainerMenu menu : new AbstractContainerMenu[] {player.containerMenu, player.inventoryMenu}) {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot.container != player.getInventory()) {
                    continue;
                }
                for (int inventorySlot : inventorySlots) {
                    if (slot.getContainerSlot() == inventorySlot) {
                        menu.setRemoteSlot(i, unknown);
                    }
                }
            }
        }
    }

    /** Returns every borrowed tool in the inventory to a toolbox. Returns how many were returned. */
    public static int returnBorrowed(Player player) {
        int returned = returnBorrowedInternal(player);
        if (returned > 0 && player instanceof ServerPlayer serverPlayer) {
            // vanilla moved the grid back with direct slot packets; resend the whole inventory
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
        return returned;
    }

    private static int returnBorrowedInternal(Player player) {
        Inventory inventory = player.getInventory();
        int returned = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!isBorrowed(stack)) {
                continue;
            }
            unmark(stack);
            if (storeInAnyToolbox(inventory, stack)) {
                inventory.setItem(i, ItemStack.EMPTY);
                returned++;
            }
        }
        return returned;
    }

    private static boolean storeInAnyToolbox(Inventory inventory, ItemStack tool) {
        for (int slot : boxSlots()) {
            ItemStack box = inventory.getItem(slot);
            if (!isToolbox(box)) {
                continue;
            }
            NonNullList<ItemStack> items = ToolboxData.load(box);
            for (int j = 0; j < items.size(); j++) {
                if (items.get(j).isEmpty()) {
                    items.set(j, tool);
                    ToolboxData.save(box, items);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBorrowed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return !stack.isEmpty() && tag != null && tag.getBoolean(BORROWED);
    }

    public static void mark(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(BORROWED, true);
    }

    public static void unmark(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BORROWED)) {
            tag.remove(BORROWED);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }
}
