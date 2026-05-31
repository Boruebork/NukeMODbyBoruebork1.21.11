package com.boruebork.nukemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LauncherBE extends BlockEntity {
    public LauncherBE(BlockPos pos, BlockState blockState) {
        super(ModBE.LAUNCHER_BE.get(), pos, blockState);
    }

    public LauncherBE(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }
}
