package com.boruebork.nukemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class LauncherBE extends BlockEntity implements MenuProvider {
    public LauncherBE(BlockPos pos, BlockState blockState) {
        super(ModBE.LAUNCHER_BE.get(), pos, blockState);
    }
    public Vec3i target = null;
    public LauncherBE(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }


    public void tick(){

    }
    @Override
    public Component getDisplayName() {
        return Component.literal("Launcher");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }
}
