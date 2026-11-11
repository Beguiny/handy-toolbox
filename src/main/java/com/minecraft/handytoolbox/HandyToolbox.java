package com.minecraft.handytoolbox;

import com.minecraft.handytoolbox.network.Network;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Handy Toolbox: a toolbox for crafting tools. JEI's [+] takes the tools a recipe needs from the box
 * (see client/jei) and they go back into the box when the crafting screen is closed.
 */
@Mod(HandyToolbox.MOD_ID)
public class HandyToolbox {
    public static final String MOD_ID = "handytoolbox";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<ToolboxItem> TOOLBOX = ITEMS.register("toolbox", () -> new ToolboxItem(new Item.Properties()));
    public static final RegistryObject<MenuType<ToolboxMenu>> TOOLBOX_MENU = MENUS.register("toolbox",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ToolboxMenu(id, inventory, buf.readVarInt())));

    public HandyToolbox() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTab);

        MinecraftForge.EVENT_BUS.addListener(this::onContainerClose);
        MinecraftForge.EVENT_BUS.addListener(this::onLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onToss);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Network::register);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TOOLBOX);
        }
    }

    /** A screen was closed (vanilla already moved the crafting grid back to the inventory): return borrowed tools. */
    private void onContainerClose(PlayerContainerEvent.Close event) {
        if (!event.getEntity().level().isClientSide) {
            ToolLending.returnBorrowed(event.getEntity());
        }
    }

    /** Safety net: if the game stopped before the close event (crash), return borrowed tools on login. */
    private void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ToolLending.returnBorrowed(event.getEntity());
    }

    /** A borrowed tool that is dropped is no longer borrowed. */
    private void onToss(ItemTossEvent event) {
        ToolLending.unmark(event.getEntity().getItem());
    }
}
