package com.boruebork.nukemod.event;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.entity.custom.client.GuidedModel;
import com.boruebork.nukemod.entity.custom.client.MushroomModel;
import com.boruebork.nukemod.entity.custom.client.NukeModel;
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
    }
}
