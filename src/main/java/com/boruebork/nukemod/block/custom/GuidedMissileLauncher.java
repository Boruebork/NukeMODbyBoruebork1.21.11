package com.boruebork.nukemod.block.custom;

import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class GuidedMissileLauncher extends BaseEntityBlock {
    private static final MapCodec<GuidedMissileLauncher> CODEC = simpleCodec(GuidedMissileLauncher::new);
    public GuidedMissileLauncher(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GuidedMissileLauncherBE(blockPos, blockState);
    }
}
