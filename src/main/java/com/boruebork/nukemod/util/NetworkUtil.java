package com.boruebork.nukemod.util;

import com.boruebork.nukemod.NukeModbyBoruebork;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class NetworkUtil {
    public static CustomPacketPayload.Type<?> setType(String name){
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(NukeModbyBoruebork.MODID, name));
    }
}
