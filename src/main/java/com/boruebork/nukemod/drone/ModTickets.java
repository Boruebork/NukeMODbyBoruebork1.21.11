package com.boruebork.nukemod.drone;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTickets {
    public static final DeferredRegister<TicketType> TT = DeferredRegister.create(Registries.TICKET_TYPE, NukeModbyBoruebork.MODID);

    public static final DeferredHolder<TicketType, TicketType> DRONE_LOADING = TT.register("drone_loading",
            () -> new TicketType(0L, TicketType.FLAG_LOADING));
    public static final DeferredHolder<TicketType, TicketType> DRONE_SIMULATION = TT.register("drone_sim",
            () -> new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    public static void register(IEventBus eventBus){
        TT.register(eventBus);
    }
}
