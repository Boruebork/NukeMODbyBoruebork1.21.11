package com.boruebork.nukemod.block.entity;

import com.boruebork.nukemod.gui.menu.IonizerMenu;
import com.boruebork.nukemod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

public class WaterIonizerBE extends BlockEntity implements MenuProvider {
    public final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 440;
    private int ionzierWeardown = 0;
    private int maxIonizerWeardown = 880;
    private int numOfIonizers = 0;

    public WaterIonizerBE(BlockPos pos, BlockState blockState) {
        super(ModBE.WIBE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> WaterIonizerBE.this.progress;
                    case 1 -> WaterIonizerBE.this.maxProgress;
                    case 2 -> WaterIonizerBE.this.ionzierWeardown;
                    case 3 -> WaterIonizerBE.this.maxIonizerWeardown;
                    case 4 -> WaterIonizerBE.this.numOfIonizers;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0: WaterIonizerBE.this.progress = value; break;
                    case 1: WaterIonizerBE.this.maxProgress = value; break;
                    case 2: WaterIonizerBE.this.ionzierWeardown = value; break;
                    case 3: WaterIonizerBE.this.maxIonizerWeardown = value; break;
                    case 4: WaterIonizerBE.this.numOfIonizers = value; break;
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        if (!hasRecipe()) {
            resetProgress();
            return;
        }

        // No active ionizer → try to start one
        if (ionzierWeardown <= 0) {
            if (!useIonizer()) {
                resetProgress();
                return;
            }
        }

        // Work
        progress++;
        ionzierWeardown--;

        if (progress >= maxProgress) {
            craftItem();
            resetProgress();
        }

        setChanged(level, pos, state);
    }

    private boolean useIonizer() {
        if (numOfIonizers > 0) {
            numOfIonizers--;
            ionzierWeardown = maxIonizerWeardown;
            return true;
        }
        return false;
    }

    private void craftItem() {
        ItemStack output = new ItemStack(ModItems.HEAVY_WATER.get(), 1);

        itemHandler.extractItem(INPUT_SLOT, 1, false);//TODO check what simulate deos
        itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));

    }

    private boolean hasRecipe() {
        ItemStack output = new ItemStack(ModItems.HEAVY_WATER.get(), 1);
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        int InputCount = input.getCount();

        return itemHandler.getStackInSlot(INPUT_SLOT).is(Items.WATER_BUCKET) &&
                canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Water Ionizer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new IonizerMenu(i, inventory, this, this.data);
    }
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        drops();
        super.preRemoveSideEffects(pos, state);
    }
    @Override
    protected void saveAdditional(ValueOutput output) {
        itemHandler.serialize(output);
        output.putInt("ionizer.progress", progress);
        output.putInt("ionizer.max_progress", maxProgress);
        output.putInt("ionizer.iw", ionzierWeardown);
        output.putInt("ionizer.miw", maxIonizerWeardown);
        output.putInt("ionizer.noi", numOfIonizers);

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input);
        progress = input.getIntOr("ionizer.progress", 0);
        maxProgress = input.getIntOr("ionizer.max_progress", 0);
        ionzierWeardown = input.getIntOr("ionizer.iw", 0);
        maxIonizerWeardown = input.getIntOr("ionizer.miw", 0);
        numOfIonizers = input.getIntOr("ionizer.noi", 0);
    }
    private void resetProgress() {
        progress = 0;
        maxProgress = 440;
    }
    public void refreshIonizer(){
        this.numOfIonizers++;
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseIonizingProgress() {
        progress++;
    }
    private boolean canInsertItemIntoOutputSlot(ItemStack output) {
        return itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ||
                itemHandler.getStackInSlot(OUTPUT_SLOT).getItem() == output.getItem();
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        int maxCount = itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ? 64 : itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
        int currentCount = itemHandler.getStackInSlot(OUTPUT_SLOT).getCount();

        return maxCount >= currentCount + count;
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
}
