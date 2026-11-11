package com.minecraft.handytoolbox.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minecraft.handytoolbox.ToolLending;
import com.minecraft.handytoolbox.ToolboxData;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * ProjectE's Repair Talisman repairs the items of each inventory it is given (player inventory, curios, alchemical
 * bag/chest), but not items stored inside other items. This also repairs the tools inside toolboxes found in those
 * inventories, with the same rules. Skipped when ProjectE is not installed.
 */
@Pseudo
@Mixin(targets = "moze_intel.projecte.gameObjs.items.RepairTalisman", remap = false)
public abstract class ProjectERepairTalismanMixin {
    @Inject(method = "repairAllItems(Lnet/minecraftforge/items/IItemHandler;Ljava/util/function/Predicate;)Z",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void handytoolbox$repairToolboxContents(IItemHandler inventory, Predicate<ItemStack> canRepair,
            CallbackInfoReturnable<Boolean> cir) {
        boolean repaired = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ToolLending.isToolbox(stack) && ToolboxData.repairContents(stack, canRepair)) {
                repaired = true;
            }
        }
        if (repaired && !cir.getReturnValueZ()) {
            cir.setReturnValue(true);
        }
    }
}
