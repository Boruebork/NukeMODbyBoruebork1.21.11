package com.boruebork.nukemod.gui.menu.launcher;

import com.boruebork.nukemod.gui.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class LauncherMenu extends AbstractContainerMenu {
    public LauncherMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.LAUNCHER_MENU.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
