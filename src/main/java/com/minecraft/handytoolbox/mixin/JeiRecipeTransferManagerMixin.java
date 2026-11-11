package com.minecraft.handytoolbox.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minecraft.handytoolbox.client.jei.ToolboxTransferHandler;

/**
 * Every [+] handler JEI picks is wrapped in a {@link ToolboxTransferHandler}, which lends tools from the toolbox
 * when the recipe needs them. If JEI is missing (or changes), this mixin is skipped.
 */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeTransferManager", remap = false)
public abstract class JeiRecipeTransferManagerMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getRecipeTransferHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private void handytoolbox$wrapHandler(CallbackInfoReturnable<Optional> cir) {
        cir.setReturnValue(ToolboxTransferHandler.wrap(cir.getReturnValue()));
    }
}
