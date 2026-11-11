package com.minecraft.handytoolbox;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Toolbox menu: 2 rows of tools + player inventory. The slot holding the open toolbox is locked. */
public class ToolboxMenu extends AbstractContainerMenu {
    private static final int TOOL_SLOTS = ToolboxData.SIZE;

    private final Inventory inventory;
    private final int boxSlot;
    private final ToolboxContainer container;

    public ToolboxMenu(int id, Inventory inventory, int boxSlot) {
        super(HandyToolbox.TOOLBOX_MENU.get(), id);
        this.inventory = inventory;
        this.boxSlot = boxSlot;
        this.container = new ToolboxContainer(() -> inventory.getItem(boxSlot));

        for (int row = 0; row < ToolboxData.ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return ToolboxData.canStore(stack);
                    }
                });
            }
        }

        int offset = (ToolboxData.ROWS - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerSlot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new PlayerSlot(inventory, col, 8 + col * 18, 161 + offset));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return ToolLending.isToolbox(inventory.getItem(boxSlot));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.close();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // don't let number keys / F swap the open toolbox out of its slot
        if (clickType == ClickType.SWAP && button == boxSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < TOOL_SLOTS) {
            if (!moveItemStackTo(stack, TOOL_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!ToolboxData.canStore(stack) || !moveItemStackTo(stack, 0, TOOL_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Inventory slot that locks the open toolbox in place. */
    private class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return getContainerSlot() != boxSlot && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() != boxSlot && super.mayPlace(stack);
        }
    }
}
