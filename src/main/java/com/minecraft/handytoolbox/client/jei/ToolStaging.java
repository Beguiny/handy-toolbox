package com.minecraft.handytoolbox.client.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.minecraft.handytoolbox.ToolLending;
import com.minecraft.handytoolbox.ToolboxData;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Finds which tools of a recipe are missing from the inventory and where they are in the toolboxes.
 * Can also simulate the lending on the client to ask JEI whether the transfer would succeed.
 */
final class ToolStaging {
    record Planned(ToolLending.Move move, ItemStack tool) {
    }

    private ToolStaging() {
    }

    static List<Planned> plan(Player player, IRecipeSlotsView recipeSlots) {
        Inventory inventory = player.getInventory();
        Map<Item, Integer> available = availableItems(player);
        List<Integer> freeSlots = freeSlots(inventory);
        Set<Integer> usedTools = new HashSet<>();
        List<Planned> plans = new ArrayList<>();

        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> options = slotView.getItemStacks().toList();
            if (options.isEmpty() || options.stream().noneMatch(ToolboxData::canStore)) {
                continue; // empty slot or not a tool
            }
            Set<Item> accepted = new HashSet<>();
            for (ItemStack option : options) {
                accepted.add(option.getItem());
            }
            if (takeAvailable(available, accepted)) {
                continue; // the player already has this tool outside a box
            }
            Planned planned = findInToolboxes(inventory, accepted, usedTools, freeSlots);
            if (planned != null) {
                plans.add(planned);
            }
        }
        return plans;
    }

    private static Planned findInToolboxes(Inventory inventory, Set<Item> accepted, Set<Integer> usedTools, List<Integer> freeSlots) {
        if (freeSlots.isEmpty()) {
            return null;
        }
        for (int boxSlot : ToolLending.boxSlots()) {
            ItemStack box = inventory.getItem(boxSlot);
            if (!ToolLending.isToolbox(box)) {
                continue;
            }
            NonNullList<ItemStack> tools = ToolboxData.load(box);
            for (int inner = 0; inner < tools.size(); inner++) {
                ItemStack tool = tools.get(inner);
                int key = boxSlot * 100 + inner;
                if (!tool.isEmpty() && accepted.contains(tool.getItem()) && usedTools.add(key)) {
                    int dest = freeSlots.remove(0);
                    return new Planned(new ToolLending.Move(boxSlot, inner, dest), tool);
                }
            }
        }
        return null;
    }

    /** Items the player has outside toolboxes: inventory + slots of the open screen (crafting grid). */
    private static Map<Item, Integer> availableItems(Player player) {
        Map<Item, Integer> available = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < ToolLending.MAIN_SLOTS; i++) {
            count(available, inventory.getItem(i));
        }
        count(available, inventory.getItem(Inventory.SLOT_OFFHAND));
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container != inventory && !(slot instanceof ResultSlot)) {
                count(available, slot.getItem());
            }
        }
        return available;
    }

    private static void count(Map<Item, Integer> available, ItemStack stack) {
        if (!stack.isEmpty() && !ToolLending.isToolbox(stack)) {
            available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
    }

    private static boolean takeAvailable(Map<Item, Integer> available, Set<Item> accepted) {
        for (Item item : accepted) {
            Integer count = available.get(item);
            if (count != null && count > 0) {
                available.put(item, count - 1);
                return true;
            }
        }
        return false;
    }

    /** Empty inventory slots, upper rows first (to keep the hotbar tidy). */
    private static List<Integer> freeSlots(Inventory inventory) {
        List<Integer> free = new ArrayList<>();
        for (int i = 9; i < ToolLending.MAIN_SLOTS; i++) {
            if (inventory.getItem(i).isEmpty()) {
                free.add(i);
            }
        }
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).isEmpty()) {
                free.add(i);
            }
        }
        return free;
    }

    static void simulate(Player player, List<Planned> plans) {
        for (Planned planned : plans) {
            player.getInventory().setItem(planned.move().destSlot(), planned.tool().copy());
        }
    }

    static void undoSimulation(Player player, List<Planned> plans) {
        for (Planned planned : plans) {
            player.getInventory().setItem(planned.move().destSlot(), ItemStack.EMPTY);
        }
    }
}
