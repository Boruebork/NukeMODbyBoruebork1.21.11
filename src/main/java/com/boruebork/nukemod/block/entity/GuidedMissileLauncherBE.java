package com.boruebork.nukemod.block.entity;

import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class GuidedMissileLauncherBE extends BlockEntity implements MenuProvider {
    private float targetYaw = 20;
    private float targetPitch = 30;

    // Rendered angles
    private float currentYaw;
    private float currentPitch;

    // Previous rendered angles (for partial ticks)
    private float previousYaw;
    private float previousPitch;

    // Rotation speeds (degrees per tick)
    private static final float YAW_SPEED = 3.0F;
    private static final float PITCH_SPEED = 2.0F;

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
        CompoundTag tag = super.getUpdateTag(pRegistries);
        tag.putFloat("Yaw", targetYaw);
        tag.putFloat("Pitch", targetPitch);
        return tag;
    }


    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        itemHandler.serialize(output);
        output.putFloat("Yaw", this.targetYaw);
        output.putFloat("Pitch", this.targetPitch);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        itemHandler.deserialize(input);
        this.targetYaw = input.getFloatOr("Yaw", 0);
        this.targetPitch = input.getFloatOr("Pitch", 0);
        super.loadAdditional(input);
    }
    public void tick(Level level1, BlockPos blockPos, BlockState blockState) {
        tickServer();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    private UUID targetPlayer;
    public void setTarget(UUID target) {
        this.targetPlayer = target;
        calculateYawPitch();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public UUID getTarget() {
        return this.targetPlayer;
    }
    private void calculateYawPitch(){
        assert this.level != null;
        Player target = this.level.getPlayerByUUID(this.targetPlayer);
        if (target == null){
            return;
        }
        Vec3 launcherPos = Vec3.atCenterOf(worldPosition);
        Vec3 targetPos = target.position();

        Vec3 direction = targetPos.subtract(launcherPos).normalize();

        this.targetYaw  = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        System.err.println("Set yaw pitch");
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
    }
    private float lastSyncedPitch;
    private float lastSyncedYaw;
    private void calculateTargetAngles() {

        Vec3 launcher = Vec3.atCenterOf(worldPosition);

        Vec3 targetPos = this.level.getPlayerByUUID(this.targetPlayer).position();

        Vec3 direction = targetPos.subtract(launcher).normalize();

        targetYaw = (float) Math.toDegrees(
                Math.atan2(direction.x, direction.z)
        );

        double horizontal =
                Math.sqrt(direction.x * direction.x +
                        direction.z * direction.z);

        targetPitch = (float)-Math.toDegrees(
                Math.atan2(direction.y, horizontal)
        );
    }
    public void tickServer() {
        if (!level.isClientSide()){
            if (Math.abs(Mth.wrapDegrees(targetYaw - lastSyncedYaw)) > 1.0F
                    || Math.abs(targetPitch - lastSyncedPitch) > 1.0F) {

                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);


                lastSyncedYaw = targetYaw;
                lastSyncedPitch = targetPitch;
                System.err.println("synced upd");

            }
        }
        if (level.isClientSide()){
            System.err.println(lastSyncedPitch);
            System.err.println(lastSyncedYaw);
        }
        // Save previous values
        previousYaw = currentYaw;
        previousPitch = currentPitch;

        // Compute desired angles if a target exists
        if (targetPlayer != null) {
            calculateTargetAngles();
        }

        // Rotate smoothly
        currentYaw = Mth.approachDegrees(
                currentYaw,
                targetYaw,
                YAW_SPEED
        );

        currentPitch = Mth.approach(
                currentPitch,
                targetPitch,
                PITCH_SPEED
        );
    }
    public float getRotatorAngle(float partialTick) {

        return Mth.lerp(
                partialTick,
                previousYaw,
                currentYaw
        );

    }

    public float getHeadRot(float partialTick) {

        return Mth.lerp(
                partialTick,
                previousPitch,
                currentPitch
        );
    }

}
