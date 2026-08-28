package com.boruebork.nukemod.gui.menu;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class EnricherScreen extends AbstractContainerScreen<EnricherMenu> {
    private static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,"textures/gui/enricher/enricher_gui.png");
    private static final Identifier ARROW_TEXTURE =
            Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID,"textures/gui/arrow_progress.png");
    private static final Identifier FUEL_ARROW =
            Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/gui/enrichemnt_fuel_arrow.png");
    private static final Identifier DROP =
            Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/gui/drop.png");
    public EnricherScreen(EnricherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        renderProgressArrow(guiGraphics, x, y);
    }
    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if(menu.isCrafting()) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE,x + 73, y + 35, 0, 0, menu.getScaledArrowProgress(), 16, 24, 16);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DROP, x + 79, y + 14, 0, 0, 11, menu.getDropProgress(), 11, 15);
        }
        guiGraphics.drawString(this.getFont(), menu.getPercentProgress() + "%", x + 137, y + 8, 0xFF00FF00);
        guiGraphics.drawString(this.getFont(), menu.getPercentFuelProgress() + "%", x + 137, y + 26, 0xFFFF5000);
    }
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);

    }
}
/*
* int WHITE  = 0xFFFFFFFF;
int BLACK  = 0xFF000000;
int RED    = 0xFFFF0000;
int GREEN  = 0xFF00FF00;
int BLUE   = 0xFF0000FF;
int YELLOW = 0xFFFFFF00;
int GRAY   = 0xFFAAAAAA;
* */