package com.boruebork.nukemod.entity.ticket;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.LoadingValidationCallback;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.boruebork.nukemod.NukeModbyBoruebork.MODID;
@EventBusSubscriber
public class ModTickets {
    public static final DeferredRegister<TicketType> TICKET_TYPES = DeferredRegister.create(Registries.TICKET_TYPE, MODID);

    public static final DeferredHolder<TicketType, TicketType> DRONE_TICKET = TICKET_TYPES.register("drone_ticket",
            ()-> new TicketType(40L, 14));
    public static void register(IEventBus eventBus){
        TICKET_TYPES.register(eventBus);
    }
    public static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(MODID, "default"),
            null // or a LoadingValidationCallback
    );
    @SubscribeEvent
    public static void reg(RegisterTicketControllersEvent event){
        event.register(CONTROLLER);
    }
}
