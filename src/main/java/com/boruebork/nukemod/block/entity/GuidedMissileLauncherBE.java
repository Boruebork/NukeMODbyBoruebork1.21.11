package com.boruebork.nukemod.block.entity;

import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherMenu;
import com.boruebork.nukemod.network.packet.SetTargetForClientBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GuidedMissileLauncherBE extends BlockEntity implements MenuProvider {

    // ---------------- TARGET (SERVER AUTHORITY)
    private UUID targetPlayer;

    private float targetYaw;
    private float targetPitch;

    // ---------------- CLIENT RENDER STATE
    private float prevYaw;
    private float prevPitch;
    private float renderYaw;
    private float renderPitch;

    private static final float YAW_SPEED = 3.0F;
    private static final float PITCH_SPEED = 2.0F;

    // ---------------- INVENTORY
    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    public UUID targetPlayer() {
        return targetPlayer;
    }

    // ---------------- MENU DATA
    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return 0; }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 0; }
    };

    public GuidedMissileLauncherBE(BlockPos pos, BlockState state) {
        super(ModBE.GUIDED_LAUNCHER_BE.get(), pos, state);
    }

    // ---------------- MENU
    @Override
    public Component getDisplayName() {
        return Component.literal("Guided Missile Launcher");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GuidedMissileLauncherMenu(id, inv, this, data);
    }

    // ---------------- SYNC (NBT SAVE/LOAD)
    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", targetYaw);
        tag.putFloat("Pitch", targetPitch);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        //temHandler.deserializeNBT(tag.getCompound("Inventory"));

        targetYaw = tag.getFloatOr("Yaw", 0.0f);
        targetPitch = tag.getFloatOr("Pitch", 0.0f);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("target",targetPlayer != null ? targetPlayer.toString() : "");

        //saveAdditional(tag);
        return tag;
    }
    // ---------------- TARGET SETTER
    public void setTarget(UUID uuid) {
        this.targetPlayer = uuid;
        setChanged();
        System.err.println("are you stupied?");
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ---------------- SERVER LOGIC
    private void tickServer() {
        if (level == null || targetPlayer == null) return;

        Player player = level.getPlayerByUUID(targetPlayer);
        if (player == null) return;

        Vec3 launcher = Vec3.atCenterOf(worldPosition);
        Vec3 dir = player.position().subtract(launcher).normalize();

        targetYaw = (float) Math.toDegrees(Math.atan2(dir.x, dir.z));

        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, horizontal));

        setChanged();

        // light sync (not every tick spam)
        if (level.getGameTime() % 10 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------------- CLIENT LOGIC
    private void tickClient() {
        if (level == null || targetPlayer == null) return;

        Player player = level.getPlayerByUUID(targetPlayer);
        if (player == null) return;

        Vec3 launcher = Vec3.atCenterOf(worldPosition);
        Vec3 dir = player.position().subtract(launcher).normalize();

        targetYaw = (float) -Math.toDegrees(Math.atan2(dir.x, dir.z));

        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        targetPitch = (float) -Math.toDegrees(Math.atan2(dir.y, horizontal));
        
        prevYaw = renderYaw;
        prevPitch = renderPitch;
        System.err.println(targetPlayer);
        renderYaw = Mth.approachDegrees(renderYaw, targetYaw, YAW_SPEED);
        renderPitch = Mth.approach(renderPitch, targetPitch, PITCH_SPEED);
        System.err.println(renderYaw + " " + targetYaw);
        System.err.println(renderPitch + " " + targetPitch);
        System.err.println(targetPlayer);
        System.err.println(renderYaw);
        System.err.println(renderPitch);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        super.onDataPacket(net, valueInput);
        System.err.println("data packet");
        String s = valueInput.getString("target").get();

        if (!s.isEmpty()) {
            try {
                System.err.println("cool!");
                targetPlayer = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                System.err.println(s);
                System.err.println("exception");
                targetPlayer = null;
            }
        } else {
            System.err.println("else");
            targetPlayer = null;
        }
    }

    // ---------------- MAIN TICK
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            tickClient();
        } else {
            tickServer();
        }
    }

    // ---------------- RENDER ACCESS
    public float getYaw(float partialTick) {
        return Mth.lerp(partialTick, prevYaw, renderYaw);
    }

    public float getPitch(float partialTick) {
        return Mth.lerp(partialTick, prevPitch, renderPitch);
    }

    public void setClientTarget(UUID id) {
        this.targetPlayer = id;
    }
}