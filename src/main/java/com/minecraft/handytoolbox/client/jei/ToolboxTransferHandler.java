package com.minecraft.handytoolbox.client.jei;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.minecraft.handytoolbox.ToolLending;
import com.minecraft.handytoolbox.network.BorrowToolsPacket;
import com.minecraft.handytoolbox.network.Network;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Wraps the [+] handler JEI would use anyway (crafting table, inventory, FastWorkbench, terminals...).
 * If a recipe only fails because of tools that are in a toolbox, those tools are lent to the inventory and the
 * original handler does the rest. Recipes themselves are not changed.
 */
public final class ToolboxTransferHandler<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
    private static final Map<IRecipeTransferHandler<?, ?>, ToolboxTransferHandler<?, ?>> WRAPPERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final IRecipeTransferHandler<C, R> delegate;

    private ToolboxTransferHandler(IRecipeTransferHandler<C, R> delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional wrap(Optional handler) {
        if (handler == null || handler.isEmpty() || handler.get() instanceof ToolboxTransferHandler) {
            return handler;
        }
        IRecipeTransferHandler original = (IRecipeTransferHandler) handler.get();
        return Optional.of(WRAPPERS.computeIfAbsent(original, h -> new ToolboxTransferHandler(h)));
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return delegate.getContainerClass();
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return delegate.getMenuType();
    }

    @Override
    public RecipeType<R> getRecipeType() {
        return delegate.getRecipeType();
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(C container, R recipe, IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        return handle(player, recipeSlots,
                transfer -> delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, transfer), doTransfer);
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(IRecipeTransferContext<R, C> context, boolean doTransfer) {
        return handle(context.getPlayer(), context.getRecipeSlots(), transfer -> delegate.transferRecipe(context, transfer), doTransfer);
    }

    private static @Nullable IRecipeTransferError handle(Player player, IRecipeSlotsView recipeSlots,
                                                         Function<Boolean, IRecipeTransferError> call, boolean doTransfer) {
        IRecipeTransferError result = call.apply(false);
        if (allowsTransfer(result) || result.getType() != IRecipeTransferError.Type.USER_FACING) {
            return doTransfer && allowsTransfer(result) ? call.apply(true) : result;
        }

        List<ToolStaging.Planned> plans = ToolStaging.plan(player, recipeSlots);
        if (plans.isEmpty()) {
            return result;
        }

        IRecipeTransferError withTools;
        ToolStaging.simulate(player, plans);
        try {
            withTools = call.apply(false);
        } finally {
            ToolStaging.undoSimulation(player, plans);
        }
        if (!allowsTransfer(withTools)) {
            return result; // something other than a tool is missing: show the original error
        }
        if (!doTransfer) {
            return withTools;
        }

        List<ToolLending.Move> moves = plans.stream().map(ToolStaging.Planned::move).toList();
        for (ToolLending.Move move : moves) {
            ToolLending.apply(player, move); // client prediction; the server does the same when it gets the packet
        }
        Network.CHANNEL.sendToServer(new BorrowToolsPacket(moves));
        return call.apply(true);
    }

    private static boolean allowsTransfer(@Nullable IRecipeTransferError error) {
        return error == null || error.getType().allowsTransfer;
    }
}
