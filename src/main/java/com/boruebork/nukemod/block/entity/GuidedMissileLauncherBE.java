package com.boruebork.nukemod.block.entity;

import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class GuidedMissileLauncherBE extends BlockEntity implements MenuProvider {
    protected List<Player> players;
    protected ContainerData data;
    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };
    public GuidedMissileLauncherBE(BlockPos pos, BlockState blockState) {
        super(ModBE.GUIDED_LAUNCHER_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return 0;
            }

            @Override
            public void set(int i, int i1) {

            }

            @Override
            public int getCount() {
                return 0;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Guided Missile Launch menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new GuidedMissileLauncherMenu(i, inventory, this, this.data);
    }
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        itemHandler.serialize(output);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        itemHandler.deserialize(input);
        super.loadAdditional(input);
    }
}
