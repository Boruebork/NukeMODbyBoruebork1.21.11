package com.boruebork.nukemod.event;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.block.entity.renderer.GuidedlauncherModel;
import com.boruebork.nukemod.entity.custom.client.fpv.FPVModel;
import com.boruebork.nukemod.entity.custom.client.fpv.FPVRenderer;
import com.boruebork.nukemod.entity.custom.client.fpvint.FPVInterceptorModel;
import com.boruebork.nukemod.entity.custom.client.grenade.GrenadeModel;
import com.boruebork.nukemod.entity.custom.client.grenade_drone.GrenadeDroneModel;
import com.boruebork.nukemod.entity.custom.client.guided.GuidedModel;
import com.boruebork.nukemod.entity.custom.client.mushroom.MushroomModel;
import com.boruebork.nukemod.entity.custom.client.nuke.NukeModel;
import com.boruebork.nukemod.entity.custom.client.rocket.RocketModel;
import com.boruebork.nukemod.entity.custom.client.rocket_drone.RocketDroneModel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid = NukeModbyBoruebork.MODID)
public class CommonEvents {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NukeModel.LAYER_LOCATION, NukeModel::createBodyLayer);
        event.registerLayerDefinition(MushroomModel.LAYER_LOCATION, MushroomModel::createBodyLayer);
        event.registerLayerDefinition(GuidedModel.LAYER_LOCATION, GuidedModel::createBodyLayer);
        event.registerLayerDefinition(GuidedlauncherModel.LAYER_LOCATION, GuidedlauncherModel::createBodyLayer);
        event.registerLayerDefinition(FPVModel.LAYER_LOCATION, FPVModel::createBodyLayer);
        event.registerLayerDefinition(FPVInterceptorModel.LAYER_LOCATION, FPVInterceptorModel::createBodyLayer);
        event.registerLayerDefinition(GrenadeModel.LAYER_LOCATION, GrenadeModel::createBodyLayer);
        event.registerLayerDefinition(GrenadeDroneModel.LAYER_LOCATION, GrenadeDroneModel::createBodyLayer);
        event.registerLayerDefinition(RocketDroneModel.LAYER_LOCATION, RocketDroneModel::createBodyLayer);
        event.registerLayerDefinition(RocketModel.LAYER_LOCATION, RocketModel::createBodyLayer);

    }
}
