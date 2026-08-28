package com.boruebork.nukemod.gui.menu.launcher;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LauncherScreen extends AbstractContainerScreen<LauncherMenu> {
    private Button launch;
    private Button assemble;
    private EditBox xI;
    private EditBox yI;
    private EditBox zI;
    public LauncherScreen(LauncherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Guided Launcher"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float x, int y, int partialTick) {

    }
}
