package com.boruebork.nukemod.gui.selectionlists;

import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherScreen;
import com.boruebork.nukemod.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.UUID;

public class PlayerSelectionList extends ObjectSelectionList<PlayerSelectionList.PlayerEntry> {
    public GuidedMissileLauncherScreen parent;
    public PlayerSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
    }
    public void generateEntries(){
        for (PlayerInfo info : this.minecraft.getConnection().getOnlinePlayers().stream().toList()){
            this.addEntry(new PlayerEntry(info));
        }
    }

    public void setParent(GuidedMissileLauncherScreen parent) {
        this.parent = parent;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean result = super.mouseClicked(event, doubleClick);
        if (!doubleClick) return result;
        PlayerEntry entry = this.getSelected();
        if (entry != null) {
            if (parent instanceof PlayerListUser) {
                parent.onPlayerSelected(entry.info);
            }else {
                throw new IllegalStateException("Your screen class should implement PlayerListUser to use PlayerSelectionList");
            }
        }
        return result;
    }
    public PlayerEntry getEntryFromUUID(UUID id){
        for (PlayerEntry entry : this.children()){
            if (entry.info.getProfile().id() == id){
                return entry;
            }
        }
        return null;
    }


    @Override
    public int getRowWidth() {
        return this.getWidth();
    }

    public static class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry>{
        public Component playerName;
        public PlayerInfo info;
        private PlayerSkin picture;
        public ClientPacketListener connection = Minecraft.getInstance().getConnection();

        public PlayerEntry(PlayerInfo info) {
            this.info = info;
            this.playerName = Component.literal(info.getProfile().name() );
            this.picture = info.getSkin();
        }

        @Override
        public Component getNarration() {
            return this.playerName;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float pT) {
            //guiGraphics.fill(this.getContentX() - 1, this.getContentY() - 1, this.getContentX() + 5, this.getContentY() + 5, Colors.GREEN);
            //guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.picture, this.getContentX() + 1, this.getContentY() + 1, 0, 0, 12, 12, 32, 32);
            PlayerFaceRenderer.draw(guiGraphics, this.picture,this.getContentX() + 1, this.getContentY() + 1,12);
            guiGraphics.drawString(Minecraft.getInstance().font,this.playerName,this.getContentX() + 18, this.getContentY() + 4, Colors.WHITE);
        }
    }
}
