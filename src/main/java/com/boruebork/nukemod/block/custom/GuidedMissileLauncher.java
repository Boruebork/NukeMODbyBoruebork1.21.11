package com.boruebork.nukemod.block.custom;

import com.boruebork.nukemod.block.entity.GuidedMissileLauncherBE;
import com.boruebork.nukemod.block.entity.WaterIonizerBE;
import com.boruebork.nukemod.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if(entity instanceof GuidedMissileLauncherBE guidedMissileLauncherBE) {
                    ((ServerPlayer) player).openMenu(new SimpleMenuProvider(guidedMissileLauncherBE, Component.literal("Guided Missile Launcher")), pos);

            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GuidedMissileLauncherBE(blockPos, blockState);
    }
}
