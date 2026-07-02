package com.boruebork.nukemod.block.custom;

import com.boruebork.nukemod.block.entity.LauncherBE;
import com.boruebork.nukemod.explosion.ExpandingExplosion;
import com.boruebork.nukemod.explosion.ExplosionManager;
import com.boruebork.nukemod.explosion.NuclearExplosion;
import com.boruebork.nukemod.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.SimpleMapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class LauncherBlock extends BaseEntityBlock {
    private static final MapCodec<LauncherBlock> CODEC = simpleCodec(LauncherBlock::new);
    public LauncherBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()){
            ExplosionManager.addExplosion((ServerLevel) level, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new LauncherBE(blockPos, blockState);
    }
}
