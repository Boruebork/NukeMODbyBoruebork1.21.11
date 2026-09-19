package com.boruebork.nukemod.entity.custom.radar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class RadarScreen extends Screen {
    private AbstractRadar radar;
    protected RadarScreen(AbstractRadar radar) {
        super(Component.literal("radar screen"));
        this.radar = radar;
    }
    @Override
    protected void init() {
        super.init();
        // add a close button, or rely on ESC/E like vanilla screens
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int radius = 80; // pixel radius of the radar circle on screen

        gui.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 0x8800FF00); // translucent green scope background

        float range = radar.getRange();
        Vec3 radarPos = radar.position();

        for (int i = 0; i < radar.getContacts().size(); i++) {
            Optional<Vec3> pos = radar.getContactPos(i);
            if (pos.isEmpty()) continue;

            Vec3 rel = pos.get().subtract(radarPos);
            // flatten to top-down: X/Z world offset -> screen offset, scaled by range->radius
            int px = centerX + (int) (rel.x / range * radius);
            int py = centerY + (int) (rel.z / range * radius);

            gui.fill(px - 2, py - 2, px + 2, py + 2, 0xFFFF0000); // red blip
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // don't pause singleplayer while checking the radar
    }

    public void update(AbstractRadar radar) {
        this.radar = radar;
    }
}
