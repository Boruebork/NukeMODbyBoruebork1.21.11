package com.boruebork.nukemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class GuidedMissileLauncherBE extends BlockEntity {
    protected List<Player> players;
    public GuidedMissileLauncherBE(BlockPos pos, BlockState blockState) {
        super(ModBE.GUIDED_LAUNCHER_BE.get(), pos, blockState);
    }
}
