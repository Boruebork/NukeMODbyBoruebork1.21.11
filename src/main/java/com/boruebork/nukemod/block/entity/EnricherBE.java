package com.boruebork.nukemod.block.entity;

import com.boruebork.nukemod.gui.menu.EnricherMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

public class EnricherBE extends BlockEntity implements MenuProvider {
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
    private static final int FUEL_SLOT = 2;

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 440;
    private int maxFuelTime = 200;
    private int fuelTime = maxFuelTime;

    public EnricherBE(BlockPos pos, BlockState blockState) {
        super(ModBE.ENRICHER_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> EnricherBE.this.progress;
                    case 1 -> EnricherBE.this.maxProgress;
                    case 2 -> EnricherBE.this.fuelTime;
                    case 3 ->EnricherBE.this.maxFuelTime;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0: EnricherBE.this.progress = value; break;
                    case 1: EnricherBE.this.maxProgress = value; break;
                    case 2: EnricherBE.this.fuelTime = value; break;
                    case 3: EnricherBE.this.maxFuelTime = value; break;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public void tick(Level level1, BlockPos blockPos, BlockState blockState) {
        if(hasRecipe()) {
            if (!itemHandler.getStackInSlot(FUEL_SLOT).is(ModItems.HEAVY_WATER)){
                return;
            }
            if (fuelTime <= 0){
                if (itemHandler.getStackInSlot(FUEL_SLOT).is(ModItems.HEAVY_WATER)){
                    useFuel();
                }else{
                    fuelTime = 0;
                    return;
                }
            }
            increaseCraftingProgress();
            fuelTime--;
            setChanged(level, blockPos, blockState);

            if(hasCraftingFinished()) {
                craftItem();
                resetProgress();
            }
        } else {
            resetProgress();
        }
    }

    private void useFuel() {
        //ItemStack fuel = new ItemStack(ModItems.HEAVY_WATER.get(), )
        itemHandler.extractItem(FUEL_SLOT, 1, false);
        fuelTime = maxFuelTime;
    }

    private void craftItem() {
        ItemStack output = new ItemStack(ModItems.ENRICHED_URANIUM_DUST.get(), 1);

        itemHandler.extractItem(INPUT_SLOT, 32, false);//TODO check what simulate deos
        itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));

    }

    private boolean hasRecipe() {
        ItemStack output = new ItemStack(ModItems.ENRICHED_URANIUM_DUST.get(), 32);
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        int InputCount = input.getCount();

        return itemHandler.getStackInSlot(INPUT_SLOT).is(ModItems.URANIUM_DUST) &&
                canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output) && InputCount >= 32;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Uranium Enricher");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EnricherMenu(i, inventory, this, this.data);
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
        output.putInt("enricher.progress", progress);
        output.putInt("enricher.max_progress", maxProgress);
        output.putInt("enricher.fuel_time", fuelTime);
        output.putInt("enricher.max_fuel_time", maxFuelTime);

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input);
        progress = input.getIntOr("enricher.progress", 0);
        maxProgress = input.getIntOr("enricher.max_progress", 0);
        fuelTime = input.getIntOr("enricher.fuel_time", 0);
        fuelTime = input.getIntOr("enricher.max_fuel_time", 0);
    }
    private void resetProgress() {
        progress = 0;
        maxProgress = 440;
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
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
