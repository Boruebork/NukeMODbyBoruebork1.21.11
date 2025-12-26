package com.boruebork.nukemod.gui.screen;

import com.boruebork.nukemod.network.packet.LaunchPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class LaunchScreen extends Screen {
    private EditBox x;
    private EditBox y;
    private EditBox z;
    private Button launch;
    public LaunchScreen() {
        super(Component.literal("launch"));
    }

    @Override
    protected void init() {
        super.init();
        x = new EditBox(this.font,
                50,
                20,
                Component.literal("X"));
        x.setPosition(20, 20);
        y = new EditBox(this.font,
                50,
                20,
                Component.literal("Y"));
        y.setPosition(80, 20);
        z = new EditBox(this.font,
                50,
                20,
                Component.literal("Z"));
        z.setPosition(140, 20);
        launch = Button.builder(Component.literal("LAUNCH"), this::triggerLaunch).build();
        launch.setPosition(this.width/2, this.height/2);
        addRenderableWidget(this.x);
        addRenderableWidget(this.y);
        addRenderableWidget(this.z);
        addRenderableWidget(launch);
    }

    private void triggerLaunch(Button button) {
        try {
            ClientPacketDistributor.sendToServer(new LaunchPacket(Integer.parseInt(this.x.getValue()), Integer.parseInt(this.y.getValue()), Integer.parseInt(this.z.getValue())));
            System.out.println("Sent packet");
        } catch (NumberFormatException e){
            System.out.println("Invalid input");
            return;
            //TODO do something if the EditBoxes have non int input e.g. play a sound render text ig
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
