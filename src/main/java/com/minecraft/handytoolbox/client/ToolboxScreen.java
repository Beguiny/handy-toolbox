package com.minecraft.handytoolbox.client;

import com.minecraft.handytoolbox.ToolboxData;
import com.minecraft.handytoolbox.ToolboxMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Looks like a 2-row chest. */
public class ToolboxScreen extends AbstractContainerScreen<ToolboxMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    public ToolboxScreen(ToolboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + ToolboxData.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int top = ToolboxData.ROWS * 18 + 17;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, top);
        graphics.blit(TEXTURE, x, y + top, 0, 126, imageWidth, 96);
    }
}
