package com.boruebork.nukemod.gui.menu;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class IonizerScreen extends AbstractContainerScreen<IonizerMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
            "textures/gui/ionizer/bg.png");
    private static final Identifier PARTICLES = Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,
            "textures/gui/ionizer/particles.png");
    public IonizerScreen(IonizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BG, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        renderProgressArrow(guiGraphics, x, y);
    }
    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isCrafting()) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PARTICLES,x + 73, y + 35, 0, 0, menu.getScaledArrowProgress(), 16, 24, 16);
        }
        guiGraphics.drawString(this.getFont(), menu.getPercentProgress() + "%", x + 138, y + 15, 0xFF00FF00);
        guiGraphics.drawString(this.getFont(), menu.getPercentIonWeardownProgress() + "%", x + 138, y + 40, 0xFF0000FF);
        guiGraphics.drawString(this.getFont(), menu.getState(), x + 7, y + 47, 0xFF00FF00);
        guiGraphics.drawString(this.getFont(), String.valueOf(menu.getAmountOfIonizers()), x + 8, y + 32, 0xFFFFFFFF);
    }
}
