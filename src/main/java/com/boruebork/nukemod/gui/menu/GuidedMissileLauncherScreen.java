package com.boruebork.nukemod.gui.menu;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.ModEntities;
import com.boruebork.nukemod.entity.custom.GuidedMissile;
import com.boruebork.nukemod.entity.custom.NukeEntity;
import com.boruebork.nukemod.entity.custom.client.NukeRenderState;
import com.boruebork.nukemod.gui.selectionlists.PlayerListUser;
import com.boruebork.nukemod.gui.selectionlists.PlayerSelectionList;
import com.boruebork.nukemod.network.packet.LaunchGuidedPacket;
import com.boruebork.nukemod.network.packet.TargetSelectedPacket;
import com.boruebork.nukemod.util.Colors;
import com.boruebork.nukemod.util.VehiclesToItemsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.registries.DeferredItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

public class GuidedMissileLauncherScreen extends AbstractContainerScreen<GuidedMissileLauncherMenu> implements PlayerListUser {
    private PlayerSelectionList list;
    private Button launch;
    private PlayerInfo playerSelected = null;
    private Entity fakeRocket;
    private static final Identifier BG = Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, "textures/gui/guidedmissilelauncher/guided_missile_launcher.png");
    public GuidedMissileLauncherScreen(GuidedMissileLauncherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

    }

    @Override
    protected void init() {
        this.clearWidgets();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        this.list = new PlayerSelectionList(this.minecraft, 54, 61, 17, 16);
        this.list.setPosition(x + 7, y + 17);
        this.list.generateEntries();
        this.list.setParent(this);
        this.launch = new Button.Builder(Component.literal("LAUNCH"), (button) -> {
            if (playerSelected != null)
                ClientPacketDistributor.sendToServer(new LaunchGuidedPacket(playerSelected.getProfile().id()));
        }).build();
        this.launch.setPosition(x + 66, y + 66);
        this.launch.setHeight(12);
        this.launch.setWidth(32);
        this.addRenderableWidget(list);
        this.addRenderableWidget(launch);
        this.list.setSelected(this.list.getEntryFromUUID(menu.blockEntity.targetPlayer()));

        super.init();
    }
    @Override
    public void onPlayerSelected(PlayerInfo info){
        this.playerSelected = info;
        ClientPacketDistributor.sendToServer(new TargetSelectedPacket(this.menu.blockEntity.getBlockPos(), info.getProfile().id()));

    }
    private Entity getRocketInstance() {
        // Если мы уже создали ракету раньше, просто возвращаем её
        if (this.fakeRocket == null) {
            // Иначе создаем один раз в памяти клиента
            this.fakeRocket = new NukeEntity(ModEntities.NUKE.get(), this.minecraft.level);
        }
        Player player = Minecraft.getInstance().player;
        if (player != null && this.fakeRocket != null) {
            // Переносим ракету в ту же точку, где стоит игрок
            this.fakeRocket.setPos(player.getX(), player.getY(), player.getZ());
        }
        return this.fakeRocket;
    }

    @Override
    public void render(GuiGraphics p_283479_, int p_283661_, int p_281248_, float p_281886_) {
        super.render(p_283479_, p_283661_, p_281248_, p_281886_);
        renderTooltip(p_283479_, p_283661_, p_281248_);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BG, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        guiGraphics.drawString(this.font, this.playerSelected == null ? "N/A" : "LOCKED", x + 133, y +58, this.playerSelected == null ? Colors.RED : Colors.GREEN);
        guiGraphics.drawString(this.font, this.playerSelected == null ? "N/A" : this.playerSelected.getProfile().name(), x + 103, y + 71, this.playerSelected == null ? Colors.RED : Colors.GREEN);
        Component.translatable("item.nukemodbyboruebork.nuclear_warhead");
        //if (fakeRocket != null)
        if (this.fakeRocket != null)
            renderNukePreview(guiGraphics, x, y, this.fakeRocket, i1);


// Проверяем, что слот не пустой, иначе нечего искать в карте

    }

    @Override
    protected void containerTick() {
        super.containerTick();
        //System.out.println(menu.blockEntity.itemHandler.getStackInSlot(0));
        ItemStack stackInSlot = this.menu.getSlot(36).getItem();
        if (!stackInSlot.isEmpty()) {

            // 2. Вытаскиваем ванильный Holder<Item> из стака
            net.minecraft.core.Holder<net.minecraft.world.item.Item> itemHolder = stackInSlot.getItemHolder();

            // 3. Ищем в твоей карте. Так как DeferredItem реализует интерфейс Holder,
            // ты можешь использовать метод .value() или напрямую сопоставлять ключи,
            // но безопаснее всего искать по самому объекту Item, чтобы не воевать с дженериками NeoForge!

            for (java.util.Map.Entry<DeferredItem<Item>, ?> entry : VehiclesToItemsConfig.DATA.entrySet()) {
                // Сравниваем чистые объекты Item из карты и из слота
                if (entry.getKey().get().asItem() == itemHolder.value().asItem()) {

                    // Нашли совпадение! Достаем наш Supplier / Holder сущности
                    java.util.function.Supplier entitySupplier = (java.util.function.Supplier) entry.getValue();

                    EntityType<?> entityType = (EntityType<?>) entitySupplier.get();

                    // Создаем фейковую ракету для рендера
                    if (this.fakeRocket == null || this.fakeRocket.getType() != entityType) {
                        this.fakeRocket = entityType.create(this.minecraft.level, EntitySpawnReason.COMMAND);

                    }
                    break;
                }
            }
        } else {
            // Если слот пустой, обнуляем фейковую ракету, чтобы превью исчезало
            this.fakeRocket = null;
        }
    }

    public static void renderNukePreview(
            GuiGraphics guiGraphics,
            int x,
            int y,
            Entity entity,
            float partialTick
    ) {
        int x0 = x + 105;
        int y0 = y + 19;
        int x1 = x + 168;
        int y1 = y + 50;

        float centerX = (x0 + x1) * 0.5F;
        float centerY = (y0 + y1) * 0.5F;

        // Continuous rotation
        float rotationDegrees =
                (Minecraft.getInstance().level.getGameTime() + partialTick) * 2.0F;

        Quaternionf rotation = new Quaternionf()
                .rotateZ((float)Math.PI)
                .rotateY((float)Math.toRadians(rotationDegrees));

        EntityRenderState state = extractRenderState(entity);

        Vector3f offset = new Vector3f(
                0.0F,
                state.boundingBoxHeight * 0.5F,
                0.0F
        );
        int scale = 40;
        if (entity instanceof NukeEntity){
            offset.y += 1.25f;
        }
        if (entity instanceof GuidedMissile){
            offset.y -= 0.7f;
            scale = 20;
        }

        guiGraphics.submitEntityRenderState(
                state,
                scale,          // scale, adjust as needed
                offset,
                rotation,
                new Quaternionf(),
                x0,
                y0,
                x1,
                y1
        );
    }
    private static EntityRenderState extractRenderState(Entity entity) {
        EntityRenderDispatcher dispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();

        EntityRenderer<? super Entity, ?> renderer =
                (EntityRenderer<? super Entity, ?>) dispatcher.getRenderer(entity);

        EntityRenderState state =
                renderer.createRenderState(entity, 1.0F);

        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        return state;
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
    }
}
